import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Framework_Logger

/**
 * Sets error details on the custom headers and makes the following properties available:
 *  - errorResponseMessage
 *  - errorStatusCode
 *  - 
 */
def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def handler = new Framework_ExceptionHandler(message, messageLog)
        
        // parse & set error details, but no log:
        handler.handleError(false, "ERROR_PARSE_ONLY")
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "PARSE_ERROR", true)
    }

    return message
}
