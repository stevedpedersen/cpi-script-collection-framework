// PARSE_METADATA.groovy
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonSlurper
import src.main.resources.script.Framework_Notifications
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Constants

/*
 * Used at beginning of framework iFlows to validate the logs payload and add the 
 * interface's custom headers to correlate the logs together. Sets a Header named
 * HasError if there is an error log entry.
 */
def Message processData(Message message) {
    def json
    try {
        def body = message.getBody(String)
        if (!body?.trim()) return message
        json = new JsonSlurper().parseText(body)
    } catch (Exception e) {
        message.setProperty("hasInvalidPayload", "true")
        return message
    }

    if (json?.projectName) message.setProperty("projectName", json.projectName)
    if (json?.integrationID) message.setProperty("integrationID", json.integrationID)

    def hasError = false
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS.each { key ->
            if (json.containsKey(key) && json[key]) {
                log("meta_${key}", json[key].toString(), messageLog)
            }
        }

        // Check if there's an error (logLevel=="ERROR") in the messages array
        if (json.messages instanceof List) {
            def errorLogs = json.messages.findAll { it.logLevel?.equalsIgnoreCase("ERROR") }
            if (errorLogs) {
                hasError = true
                errorLogs.each { logError(it, message, messageLog) }
            }
        } else if (json.messages instanceof Map) {
            if (json.messages?.logLevel?.equalsIgnoreCase("ERROR")) {
                hasError = true
                logError(json.messages, message, messageLog)
            }
        }


    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_METADATA", true)
    }

    message.setHeader("HasError", "${hasError}")
    return message
}

// If it is an ERROR, set various error_ headers and ensure custom status is "Failed"
def logError(Map errorLog, Message message, MessageLog messageLog) {
    if (!errorLog?.logLevel?.equalsIgnoreCase("ERROR")) return
    
    if (errorLog.text) log("error_message", errorLog.text, messageLog)
    if (errorLog.statusCode) log("error_statusCode", errorLog.statusCode, messageLog)
    if (errorLog.exceptionClass) log("error_exceptionClass", errorLog.exceptionClass, messageLog)
    if (errorLog.exceptionMessage) log("error_exceptionMessage", errorLog.exceptionMessage, messageLog)
    
    def customStatus = message.getProperty(Constants.Property.MPL_CUSTOM_STATUS)
    if (!customStatus) message.setProperty(Constants.Property.MPL_CUSTOM_STATUS, "Failed")
}

// A tiny helper to keep code neat
def log(String key, String value, MessageLog messageLog) {
    if (messageLog) {
        messageLog.addCustomHeaderProperty(key, value)
    }
}

// Filters out the log.messages to include the emailFilterValues only (error logs only by default)
def Message filterLogs(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def handler = new Framework_Utils(message, messageLog)
        def updatedBody = handler.filterLogs()
        message.setBody(updatedBody)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_METADATA.filterLogs", true)
    }
    return message
}

// Masks the values specified in emailSensitiveFields
def Message maskEmailFields(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        // def utils = new Framework_Utils(message, messageLog)
        def props = message.getProperties()
        def maskedLogs = Framework_Utils.maskFields(
            message.getBody(String), props.get("projectName"), props.get("integrationID"), "emailSensitiveFields", message, messageLog)
        message.setBody(maskedLogs)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_METADATA.maskEmailFields", true)
    }
    return message
}

// Masks the values specified in loggerSensitiveFields
def Message maskLoggerFields(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        // def utils = new Framework_Utils(message, messageLog)
        def props = message.getProperties()
        def maskedLogs = Framework_Utils.maskFields(
            message.getBody(String), props.get("projectName"), props.get("integrationID"), "loggerSensitiveFields", message, messageLog)
        message.setBody(maskedLogs)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_METADATA.maskLoggerFields", true)
    }
    return message
}

// Used in the Notifications handler to conver the JSON logs to an HTML table for email
def Message formatEmailBody(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def handler = new Framework_Notifications(message, messageLog)
        handler.formatHtmlBody()
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_METADATA.formatEmailBody", true)
    }
    return message
}

