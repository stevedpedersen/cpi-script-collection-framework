package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.io.PrintWriter
import groovy.json.JsonSlurper
import src.main.resources.script.Constants

class Framework_ExceptionHandler {
    Message message
    MessageLog messageLog
    Map error = [
    	className: "",
    	statusCode: 500,
    	message: "Internal Server Error",
    	details: "",
    	exceptionMessage: "",
    	requestUri: "",
    	stackTrace: "",
    	headers: null,
    	hasRetry: false,
    ]

    static final String ERROR_MSG_KEY = "message"
    static final String ERROR_KEY_PROP = "errorMessageKeyName"

    static adapterExceptions = [
        "HTTP": [
            "org.apache.camel.component.ahc.AhcOperationFailedException": "handleHttpAdapterException"
        ],
        "OData V2": [
            "com.sap.gateway.core.ip.component.odata.exception.OsciException": "handleODataV2AdapterException",
            "com.sap.gateway.core.ip.component.exception.ODataProcessingException": "handleODataV2AdapterException",
            "org.apache.olingo.odata2.api.exception.ODataApplicationException": "handleODataV2AdapterException",
            "org.apache.olingo.odata2.api.uri.UriNotMatchingException": "handleODataV2AdapterException"
        ],
        // "SOAP": [
        //     "org.apache.cxf.binding.soap.SoapFault": "handleSOAPAdapterException"
        // ],
    ]

    Framework_ExceptionHandler(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        setErrorProperties()
    }

    /**
     * Unified method to set message properties, custom headers, and optionally log.
     * In the iFlow log scripts, call this with `handleError(true)` to log,
     * or `handleError(false)` to just parse & set props.
     */
    def handleError(boolean shouldLog = false, String tracePoint = "ERROR") {
    	try {
	        // Set message properties
	        message.setProperty("log_statusCode", error.statusCode.toString())
	        message.setProperty("log_exceptionClass", error.className)
	        if (!error.message || error.message != error.exceptionMessage) {
	        	message.setProperty("log_exceptionMessage", error.exceptionMessage)
        	}
	        
	        // Add custom headers if we have a messageLog
	        if (messageLog) {
	        	messageLog.addCustomHeaderProperty("error_message", error.message ?: error.exceptionMessage)
	            messageLog.addCustomHeaderProperty("error_exceptionClass", error.className)
	            messageLog.addCustomHeaderProperty("error_statusCode", error.statusCode.toString())
	            if (!error.message || error.message != error.exceptionMessage) {
	            	messageLog.addCustomHeaderProperty("error_exceptionMessage", error.exceptionMessage)
	            }
	        }

	        // If we want to log, do it now
	        if (shouldLog) {
	            def logger = new Framework_Logger(message, messageLog)
	            logger.logMessage(tracePoint, "ERROR", error.message ?: "Unknown error")
	        }
    	} catch (Exception e) {
    		Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ExceptionHandler.handleError", true)
    	}
    }

    def Map getErrorDetails() {
    	return this.error
    }

    private void setErrorProperties() {
        def hasAdapterRedelivery = this.message.getProperty(Constants.Property.SAP_IS_REDELIVERY_ENABLED)
        this.error.hasRetry = hasAdapterRedelivery != null && hasAdapterRedelivery.toBoolean() != false
        this.error.headers = this.message.getHeaders()

        def ex = this.message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT)
        if (ex != null) {
            this.error.className = ex.getClass().getCanonicalName()
            this.error.message = ex.getMessage() ?: this.error.message
            this.error.exceptionMessage = ex.getMessage()

            // Possibly override statusCode for certain validation
            if (this.error.message?.contains(Constants.ILCD.Validator.EXC_CLASS)) {
                this.error.statusCode = 400
                this.message.setHeader(Constants.Header.CAMEL_RESPONSE_CODE, "400")
                this.error.className = this.error.className
            }

    		// Use adapter handlers to parse error details
    		adapterExceptions.each { adapter, exceptions ->
    			if (exceptions.containsKey(this.error.className)) {
    				def methodName = exceptions.get(this.error.className)
    				this."$methodName"(ex)
    			}
    		}
    	}

        // Also read the CamelHttpResponseCode
        def existingCode = this.error.headers.get(Constants.Header.CAMEL_RESPONSE_CODE) 
            ?: this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)
            ?: this.message.getProperty(Constants.Header.CAMEL_RESPONSE_CODE)
        if (existingCode) {
            this.error.statusCode = existingCode.toString().isInteger() ? existingCode.toInteger() : 500
        }

        // Store in message for further steps
        this.message.setProperty("errorResponseMessage", this.error.message)
        this.message.setProperty("errorStatusCode", this.error.statusCode)
        this.message.setProperty("errorExceptionMessage", this.error.exceptionMessage)
        this.message.setProperty("errorExceptionClass", this.error.exceptionClass)
    }

    private void handleHttpAdapterException(Exception ex) {
    	// Status Code
    	def statusCode = ex.getStatusCode()
    	def headerCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
    	this.error.statusCode = statusCode ?: headerCode ?: this.error.statusCode

    	// Error Message
    	def body = ex.getResponseBody()
    	if (body && body.trim().length() > 0) {
    		this.error.message = parseMessageFromBody(body) ?: this.error.message
    	}

    	// Status Text
    	this.error.details = ex?.getStatusText() ?: ""
    }

    private void handleODataV2AdapterException(Exception ex) {
    	// Status Code
	    def camelCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
	    this.error.statusCode = camelCode ? camelCode : this.error.statusCode

	    // Error Message (from body)
	    def body = this.message.getBody(String)
	    if (body && body.trim().length() > 0) {
	    	this.error.message = parseMessageFromBody(body) ?: this.error.message
	    }

	    // Requested URI
	    this.error.requestUri = ex.getRequestUri()

	    // Details
	    this.error.details = ex.getLocalizedMessage() ?: ex.getHttpStatus() ?: ex.getErrorCode()
    }

	def String parseMessageFromBody(String body) {
		def altKey = this.message.getProperty(ERROR_KEY_PROP) ?: ""
	    def messages = []
    	if (body.startsWith('{')) {
    		try {
    			def json = new JsonSlurper().parseText(body)

	            // JSON structure: Recursively find "message" fields and add to messages list
	            def findMessages
	            findMessages = { node ->
	                if (node instanceof Map) {
	                    node.each { key, value ->
	                        if (value instanceof String &&
	                        	(key.equalsIgnoreCase(ERROR_MSG_KEY) || key.equalsIgnoreCase(altKey))) {
	                            // Check that the same message hasn't been added already
	                            if (!messages.contains(value)) {
	                            	messages.add(value)
	                            }
	                        }
	                        findMessages(value)
	                    }
	                } else if (node instanceof List) {
	                    node.each { findMessages(it) }
	                }
	            }
	            findMessages(json)

			} catch (Exception e) {}
    	} else if (body.startsWith('<')) {
	        try {
	            def xml = new XmlSlurper().parseText(body)

	            xml.depthFirst().findAll { node -> 
	            	node.name().equalsIgnoreCase(ERROR_MSG_KEY) || node.name().equalsIgnoreCase(altKey)
	            }.each { node -> 
		            if (node.children().isEmpty()) {
                        // Check that the same message hasn't been added already
                        if (!messages.contains(node.text())) {
                        	messages.add(node.text())
                        }
		            } else if (node.value?.children()?.isEmpty()) {
		            	messages.add(node.value.text())
		            }
	            }
	        } catch (Exception e) {}
    	}

	    return messages.join(", ")
	}

    // private void handleSOAPAdapterException(Exception ex) {
	//     def messageLog = messageLogFactory.getMessageLog(message)

	//     // Fault Detail Element
	//     def xml = XmlUtil.serialize(ex.getOrCreateDetail())
	//     messageLog.addAttachmentAsString("http.response", xml, "application/xml")

	//     message.setProperty("http.response", ex.getMessage())
	//     message.setProperty("http.statusCode", ex.getStatusCode())

	//     setErrorProperties(ex.getStatusCode(), ex.getStatusCode(), ex.getMessage())
    // }

    def static String getStackTrace(Exception e) {
        try {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            e.printStackTrace(pw)
            return sw.toString()
        } catch (Exception innerEx) {
            // LoggerFactory.getLogger("Framework_ExceptionHandler").error("Error generating stack trace: ${innerEx.message}", innerEx)
            return "Error generating stack trace: ${innerEx.message}"
        }
    }
}