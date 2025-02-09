import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger

def Message processData(Message message) {
    try {
        def logger = new Framework_Logger(message, messageLogFactory.getMessageLog(message))
        def jsonMessageLog = logger.conclude(true, false)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLogFactory.getMessageLog(message), e, "CONCLUDE", true)
    }
    return message
}

def Message withoutAttachment(Message message) {
    try {
        def logger = new Framework_Logger(message, messageLogFactory.getMessageLog(message))
        def jsonMessageLog = logger.conclude(false, false)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLogFactory.getMessageLog(message), e, "CONCLUDE.withoutAttachment", true)
    }
    return message
}

def Message includeSystemInfo(Message message) {
    try {
        def logger = new Framework_Logger(message, messageLogFactory.getMessageLog(message))
        def jsonMessageLog = logger.conclude(true, true)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLogFactory.getMessageLog(message), e, "CONCLUDE.includeSystemInfo", true)
    }
    return message
}