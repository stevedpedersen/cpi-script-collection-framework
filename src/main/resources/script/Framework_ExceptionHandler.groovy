package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
// import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.io.PrintWriter
import java.io.RandomAccessFile
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
        location: "BTP CI",
        type: "",
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
                if (error.message != null) { 
                    error.message.split("\n").each { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_MSG, "${it}") }
                }
                if (error.className != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_EXC_CLASS, "${error.className}") }
                if (error.statusCode != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_STATUS_CODE, "${error.statusCode.toString()}")}
                if (error.type != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_TYPE, "${error.type}") }
                if (error.exceptionMessage != null && (!error.message || error.message != error.exceptionMessage)) {
                    messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_EXC_MSG, "${error.exceptionMessage}")
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
        this.error.type = Constants.ILCD.ExceptionHandler.normalizeErrorType(
            this.message.getProperty(Constants.ILCD.ExceptionHandler.ERR_TYPE_PROPERTY))

        def ex = this.message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT)
        if (ex != null) {
            this.error.className = ex.getClass().getCanonicalName()
            this.error.message = ex.getMessage() ?: this.error.message
            this.error.exceptionMessage = ex.getMessage()

            // Set the error fields for a 400 Bad Request when throwing validation errors
            if (this.error.message?.contains(Constants.ILCD.Validator.EXC_CLASS)) {
				this.message.setHeader(Constants.Header.CAMEL_RESPONSE_CODE, "400")
                this.error.statusCode = 400
                this.error.className = "Framework_Validator.${Constants.ILCD.Validator.EXC_CLASS}"
                this.error.type = Constants.ILCD.ExceptionHandler.ERR_TYPE_FUNC

                def lines = this.error.message.split("\\r?\\n")
                if (lines && lines.size() > 0 && lines[0].startsWith("java.lang.Exception:")) {
                    def errBody = lines[0].replaceFirst(/^java\\.lang\\.Exception: [^:]+: /, "")
                    def newLines = errBody.split("\\n") as List
                    if (lines.size() > 1) newLines.addAll(lines[1..-1])
                    lines = newLines
                }
                def filtered = lines.findAll { it.trim().startsWith("[") }
                if (filtered && filtered.size() > 0) {
                    this.error.message = filtered.collect { it.replaceAll(/@.*/, '').trim() }.join("\n")
                }
            }

    		// Use adapter handlers to parse error details
			Constants.ILCD.ExceptionHandler.ADAPTER_EXC_CLASSES.each { adapter, exceptions ->
				if (exceptions.containsKey(this.error.className)) {
					def methodName = exceptions.get(this.error.className)
					this."$methodName"(ex)
				}
			}
        }

        // Custom error parsing for XML body (if not already set by adapters)
        def body = this.message.getBody(String)
        if (body && body.trim().startsWith("<") && (!this.error.message || this.error.message == "Internal Server Error")) {
            parseAndSetXmlError(body)
        }

        // Also read the CamelHttpResponseCode
        def existingCode = this.error.headers.get(Constants.Header.CAMEL_RESPONSE_CODE) 
            ?: this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)
            ?: this.message.getProperty(Constants.Header.CAMEL_RESPONSE_CODE)
        if (existingCode) {
            this.error.statusCode = existingCode.toString().isInteger() ? existingCode.toInteger() : 500
        }

        // Store in message for further steps
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_RESP_MSG, this.error.message)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_STATUS_CODE, this.error.statusCode)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_EXC_MSG, this.error.exceptionMessage)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_EXC_CLASS, this.error.className)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_TYPE, this.error.type)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_META_ATTR_ERR_TYPE, this.error.type)
    }
    
    private void handleHttpAdapterException(Exception ex) {
        def statusCode = ex.getStatusCode()
        def headerCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
        this.error.statusCode = statusCode ?: headerCode ?: this.error.statusCode
        def body = ex.getResponseBody()
        if (body && body.trim().length() > 0) {
            this.error.message = parseMessageFromBody(body) ?: this.error.message
        }
        this.error.details = ex?.getStatusText() ?: ""
    }

    private void handleODataV2AdapterException(Exception ex) {
        def camelCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
        this.error.statusCode = camelCode ? camelCode : this.error.statusCode
        def body = this.message.getBody(String)
        if (body && body.trim().length() > 0) {
            this.error.message = parseMessageFromBody(body) ?: this.error.message
        }
        this.error.requestUri = ex.getRequestUri()
        this.error.details = ex.getLocalizedMessage() ?: ex.getHttpStatus() ?: ex.getErrorCode()
    }

    /**
     * Parses XML response (as String) for error/status and sets error fields if found.
     * Used for custom error detection in CPI SOAP/REST responses.
     */
    void parseAndSetXmlError(String body) {
        if (!body || body.trim().isEmpty()) return
        try {
            def parser = new XmlSlurper(false, false)
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            def parsed = parser.parseText(body)

            // Get escaped XML content inside <processActionReturn>
            def encodedXml = parsed.'**'.find { it.name() == 'processActionReturn' }?.text()
            if (encodedXml && encodedXml.trim()) {
                // Unescape and parse the CDATA XML string (decode HTML entities like &lt;)
                def decodedXml = encodedXml.replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('&amp;', '&')
                parsed = parser.parseText(decodedXml)
            }

            // Check for <property id="error">
            def errorNode = parsed.'**'.find { it.name() == 'property' && it.@id == 'error' }
            if (errorNode && errorNode.text().trim()) {
                this.error.message = errorNode.text().trim()
                this.error.statusCode = 500
                this.error.type = "XML_PARSE"
                return
            }
            // Check <property id="status"> == FAILED
            def statusNode = parsed.'**'.find { it.name() == 'property' && it.@id == 'status' }
            if (statusNode && statusNode.text().toUpperCase() == "FAILED") {
                this.error.message = "Response status is FAILED."
                this.error.statusCode = 500
                this.error.type = "XML_PARSE"
            }
        } catch (Exception e) {
            // Fallback: don't throw, just log
        }
    }

    def String parseMessageFromBody(String body) {
        def altKey = this.message.getProperty(Constants.ILCD.ExceptionHandler.ERR_KEY_PROP) ?: ""
        def messages = []
        if (body.startsWith('{')) {
            try {
                def json = new JsonSlurper().parseText(body)
                def findMessages
                findMessages = { node ->
                    if (node instanceof Map) {
                        node.each { key, value ->
                            if (value instanceof String &&
                                (key.equalsIgnoreCase(Constants.ILCD.ExceptionHandler.ERR_MSG_KEY) || key.equalsIgnoreCase(altKey))) {
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
                    node.name().equalsIgnoreCase(Constants.ILCD.ExceptionHandler.ERR_MSG_KEY) || node.name().equalsIgnoreCase(altKey)
                }.each { node -> 
                    if (node.children().isEmpty()) {
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

    /**
     * Fetches the last N lines from the server trace log at /app/log/ljs_trace.log.
     * @param numLines Number of lines to read from the end of the log (default 100)
     * @return String containing the last N lines, or error message if file not found/readable.
     */
    static String getServerTraceTail(int numLines = 100) {
        String logPath = "/app/log/ljs_trace.log"
        File logFile = new File(logPath)
        if (!logFile.exists() || !logFile.canRead()) {
            return "Error: Log file not found or not readable: ${logPath}"
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(logFile, "r")
            long fileLength = raf.length() - 1
            int linesRead = 0
            StringBuilder sb = new StringBuilder()
            for(long pointer = fileLength; pointer >= 0; pointer--) {
                raf.seek(pointer)
                int readByte = raf.readByte()
                if(readByte == 0xA) { // LF: \n
                    if(pointer < fileLength) {
                        linesRead++
                        if(linesRead > numLines) break
                    }
                }
                sb.append((char) readByte)
            }
            raf.close()
            String tailStr = sb.reverse().toString()
            List<String> tailLines = tailStr.split("\n")
            if (tailLines.size() > numLines) {
                tailLines = tailLines[-numLines..-1]
            }
            return tailLines.join("\n")
        } catch(Exception e) {
            return "Error reading log file: ${e.getMessage()}"
        }
    }

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

    boolean toBool(Object rawValue) {
        if (rawValue == null) return false 
        if (rawValue instanceof Boolean) return (Boolean) rawValue
        return Boolean.parseBoolean(rawValue.toString())
    }
}