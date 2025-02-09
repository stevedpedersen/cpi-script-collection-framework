import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Framework_Logger

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)
    try {
        def utils = new Framework_Utils(message, messageLog)
        def formattedXml = utils.formatResponseXml()
        message.setBody(formattedXml as String)
    } catch (Exception e) {
        Framework_Logger.handleScriptError(message, messageLog, e, "FORMAT_RESPONSE_XML", true)
    }
    return message
}