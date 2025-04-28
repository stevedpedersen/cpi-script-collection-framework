import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger

/**
 * Sets soft error properties:
 *   - isSoftError
 *   - softErrorReason
 *   - softErrorMessage
 * Use `logStrict` to throw if a soft error is found.
 */
def Message processData(Message message) { 
    return log(message, "CUSTOM", "INFO", message.getProperty("text"))
}

/**
 * Same as processData, but throws on soft error (for fail/branch logic).
 */
def logStrict(Message message) {
    return log(message, "CUSTOM", "INFO", message.getProperty("text"), true)
}

def Message log(Message message, String tracePoint, String logLevel, String logData, boolean isStrict = false) {
    try {
        def logger = new Framework_Logger(message, messageLogFactory.getMessageLog(message))
        logger.logMessage(tracePoint, logLevel, logData, isStrict)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLogFactory.getMessageLog(message), e, tracePoint, true)
    }
    return message
}

def Message info(Message message) { 
    return log(message, "INFO_CUSTOM", "INFO", message.getProperty("text")) 
}
def Message debug(Message message) { 
    return log(message, "DEBUG_CUSTOM", "DEBUG", message.getProperty("text")) 
}
def Message trace(Message message) { 
    return log(message, "TRACE_CUSTOM", "TRACE", message.getProperty("text")) 
}
def Message warn(Message message) { 
    return log(message, "WARN_CUSTOM", "WARN", message.getProperty("text")) 
}