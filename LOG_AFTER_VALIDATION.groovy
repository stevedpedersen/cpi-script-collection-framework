import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Framework_Validator
import org.apache.commons.io.input.BOMInputStream

/**
 * Default behavior is to locate validation errors and log them without throwing
 * an exception. Users can choose to automatically raise a validation exception
 * by specifying "raiseException" as the Script Function. 
 */
def Message processData(Message message) { 
    return handleLog(message, false)
}

def Message raiseException(Message message) {
    return handleLog(message, true)
}

def defaultText() { "Successful payload validation" }

def Message handleLog(Message message, boolean raiseException) {
    def validationErrorMessage = null
    try {
        def validator = new Framework_Validator(message, messageLogFactory.getMessageLog(message))

        // Check for validation errors and log the result
        def xmlInput = message.getBody(java.io.InputStream)
        def bomStream = new BOMInputStream(xmlInput)
        // Force skip the BOM if one is present
        bomStream.getBOM()
        validationErrorMessage = validator.getValidationErrors(bomStream)
        
        // Log the result 
        if (validationErrorMessage) {
            def records = validationErrorMessage.split(/\[\w*?\]:/)
            def recordErrors = records.collect{ it.split("Mandatory Field") }
            message.setProperty("validationErrorMessage", validationErrorMessage)
            log(message, "VALIDATION_ERROR", "WARN", validationErrorMessage)
        } else {
            log(message, "VALIDATION_SUCCESS", "TRACE", message.getProperty("text") ?: defaultText())
        }
    } catch (Exception e) {
        Framework_Logger.handleScriptError(
            message, messageLogFactory.getMessageLog(message), e, "LOG_AFTER_VALIDATION", true, message.getProperty("text") ?: "")
    }

    if (validationErrorMessage && raiseException) {
        Framework_Validator.throwValidationError(validationErrorMessage)
    }

    return message
}


def Message log(Message message, String tracePoint, String logLevel, String logData) {
    try {
        def logger = new Framework_Logger(message, messageLogFactory.getMessageLog(message))
        logger.logMessage(tracePoint, logLevel, logData)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLogFactory.getMessageLog(message), e, tracePoint, true)
    }
    return message
}
