import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Framework_Logger

/**
 * Sets error details on the custom headers and makes the following properties available:
 *  - errorResponseMessage
 *  - errorStatusCode
 *  - errorExceptionMessage
 *  - errorExceptionClass
 */
def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {  
        def handler = new Framework_ExceptionHandler(message, messageLog)

        // Parse and log error
        handler.handleError(true, "ERROR")
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "LOG_ERROR", true)
    }
    return message
}
