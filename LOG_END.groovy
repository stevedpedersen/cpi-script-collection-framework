import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger

def Message processData(Message message) { 
    return info(message) 
}

def defaultText() { "END of iFlow transaction" }

def Message info(Message message) { 
    return log(message, "END", "INFO", (message.getProperty("text") ?: defaultText())) 
}
def Message debug(Message message) { 
    return log(message, "END", "DEBUG", (message.getProperty("text") ?: defaultText())) 
}
def Message trace(Message message) { 
    return log(message, "END", "TRACE", (message.getProperty("text") ?: defaultText())) 
}
def Message warn(Message message) { 
    return log(message, "END", "WARN", (message.getProperty("text") ?: defaultText())) 
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