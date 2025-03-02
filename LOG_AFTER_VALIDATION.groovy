import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Validator

/**
 * Default behavior is to locate validation errors and log them without throwing
 * an exception. Users can choose to automatically raise a validation exception
 * by specifying "raiseException" as the Script Function.
 */
def Message processData(Message message) {
    def validator = new Framework_Validator(message, messageLogFactory.getMessageLog(message))
    validator.logValidationResults(false) // don't throw exception afterwards
    message
}

def Message raiseException(Message message) {
    def validator = new Framework_Validator(message, messageLogFactory.getMessageLog(message))
    validator.logValidationResults(true) // throw exception afterwards
    message
}