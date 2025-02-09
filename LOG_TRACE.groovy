import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger

def Message processData(Message message) { 
    return log(message, "TRACE", "TRACE", message.getProperty("text")) 
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
