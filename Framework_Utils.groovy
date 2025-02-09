package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.xml.MarkupBuilder
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants


class Framework_Utils {

	static final String CLASSNAME = "Framework_Utils"

	Message message
    MessageLog messageLog
    Framework_Logger logger

    Framework_Utils(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.logger = new Framework_Logger(message, messageLog)
    }

    def String formatResponseXml() {
    	this.message.setHeader(Constants.Header.CONTENT_TYPE, Constants.Header.CONTENT_TYPE_XML)

    	try {
    		return formatResponse("xml")
    	} catch (Exception e) {
    		Framework_Logger.handleScriptError(this.message, this.messageLog, e, "Framework_Utils.formatResponseXml()", false)
    		throw e
    	}
    }

    def String formatResponseJson() {
    	this.message.setHeader(Constants.Header.CONTENT_TYPE, Constants.Header.CONTENT_TYPE_JSON)

    	try {
    		return formatResponse("json")
    	} catch (Exception e) {
    		Framework_Logger.handleScriptError(this.message, this.messageLog, e, "Framework_Utils.formatResponseJson()", false)
    		throw e
    	}
    }

    def String formatResponse(String format) {
	    def headers = this.message.getHeaders()
	    def properties = this.message.getProperties()
	    def jsonBodyString = this.logger.conclude()
	    def json = new JsonSlurper().parseText(jsonBodyString)

	    def correlationID = headers?.get(Constants.Header.SAP_CORRELATION_ID)
	    if (!json?.correlationID && correlationID) {
	        json = [correlationID: correlationID] + json
	    }

	    def xmlResponse, jsonResponse
	    def hasError = json.messages.any { it.logLevel == 'ERROR' || it.errorLocation }

	    if (hasError) {
	        // Error response formatting
	        def statusCode = this.message
	        def errorDetails = json.messages.find { it.logLevel == 'ERROR' && it.errorLocation }
	        xmlResponse = createXmlErrorResponse(errorDetails, json)
	        jsonResponse = createJsonErrorResponse(errorDetails, json)
	    } else {
	        // Success response formatting
	        xmlResponse = createXmlSuccessResponse(json)
	        jsonResponse = createJsonSuccessResponse(json)
	    }

	    // Set the formatted responses as properties
	    message.setProperty("xmlResponse", xmlResponse)
	    message.setProperty("jsonResponse", jsonResponse)

	    return format.equalsIgnoreCase("json") ? JsonOutput.prettyPrint(jsonResponse) : xmlResponse
    }

    def String getStatusCode(errorDetails) {
	    def headers = this.message.getHeaders()
	    def properties = this.message.getProperties()
	    def statusCode = errorDetails ? "500" : "200"

	    if (properties.get("errorStatusCode")) {
	    	statusCode = properties.get("errorStatusCode").toString()
	    } else if (errorDetails?.statusCode) {
	    	statusCode = errorDetails.statusCode.toString()
	    } else if (properties.get("statusCode")) {
	    	statusCode = properties.get("statusCode").toString()
	    } else if (headers.get(Constants.Header.CAMEL_RESPONSE_CODE)) {
	    	statusCode = headers.get(Constants.Header.CAMEL_RESPONSE_CODE).toString()
	    }
	    
	    def httpCode = headers.get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
	    if (statusCode?.toInteger() >= 300 && (!httpCode || httpCode?.toInteger() < 300)) {
	        this.message.setHeader(Constants.Header.CAMEL_RESPONSE_CODE, statusCode)
	    }

	    return statusCode
    }

    def String getErrorMessage(errorDetails) {
		def ex = this.message.getProperties().get(Constants.Property.CAMEL_EXC_CAUGHT)
		def errorMessage = ex != null ? ex.getMessage() : "Internal Server Error" // Fallback error message
    	if (errorDetails.text && errorDetails.text != errorMessage) {
    		errorMessage = errorDetails.text
    	}
    	return errorMessage
    }

	// Function to create XML error response
	def createXmlErrorResponse(errorDetails, json) {
	    StringWriter writer = new StringWriter()
	    MarkupBuilder xml = new MarkupBuilder(writer)
	    xml.Error {
	        Code(getStatusCode(errorDetails))
	        Message(getErrorMessage(errorDetails))
	        ErrorDetails {
	            TransactionID(json.correlationID)
	            TimeStamp(errorDetails.timestamp ?: json.timestamp)
	            ErrorLocation(errorDetails.errorLocation ?: errorDetails.errorStepID)
	        }
	    }
	    return writer.toString()
	}

	// Function to create JSON error response
	def createJsonErrorResponse(errorDetails, json) {
	    def response = [
	        Code: getStatusCode(errorDetails),
	        Message: getErrorMessage(errorDetails),
	        ErrorDetails: [
	            TransactionID: json.correlationID,
	            TimeStamp: errorDetails.timestamp ?: json.timestamp,
	            ErrorLocation: errorDetails.errorLocation ?: errorDetails.errorStepID
	        ]
	    ]
	    return JsonOutput.toJson(response)
	}

	// Function to create XML success response
	def createXmlSuccessResponse(json) {
	    StringWriter writer = new StringWriter()
	    MarkupBuilder xml = new MarkupBuilder(writer)
	    xml.Response {
	        Code(getStatusCode(null))
	        Message(Constants.ILCD.Utils.SUCCESS_RESPONSE_MSG)
	        Details {
	            TransactionID(json.correlationID)
	            TimeStamp(json.timestamp)
	        }
	    }
	    return writer.toString()
	}

	// Function to create JSON success response
	def createJsonSuccessResponse(json) {
	    def response = [
	        Code: getStatusCode(null),
	        Message: Constants.ILCD.Utils.SUCCESS_RESPONSE_MSG,
	        Details: [
	            TransactionID: json.correlationID,
	            TimeStamp: json.timestamp
	        ]
	    ]
	    return JsonOutput.toJson(response)
	}
}	