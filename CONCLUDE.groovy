import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def logger = new Framework_Logger(message, messageLog)
        def jsonMessageLog = logger.conclude(true, false)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "CONCLUDE", true)
    }
    return message
}

def Message withoutAttachment(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def logger = new Framework_Logger(message, messageLog)
        def jsonMessageLog = logger.conclude(false, false)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "CONCLUDE.withoutAttachment", true)
    }
    return message
}

def Message includeSystemInfo(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def logger = new Framework_Logger(message, messageLog)
        def jsonMessageLog = logger.conclude(true, true)
        message.setBody(jsonMessageLog)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "CONCLUDE.includeSystemInfo", true)
    }
    return message
}