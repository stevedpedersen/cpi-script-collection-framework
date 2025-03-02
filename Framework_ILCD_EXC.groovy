import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants

/**
 * This script is intended for internal ILCD errors only.
 */
def Message processData(Message message) {
    return ilcdException(message, true)
}

def Message withoutStackTrace(Message message) {
    return ilcdException(message, false)
}

def Message ilcdException(Message message, includeStackTrace = true) {
    def properties = message.getProperties()
    def scriptErrorLocation = properties.get("scriptErrorLocation") ?: "unknown"
    
    def ex = properties.get(Constants.Property.CAMEL_EXC_CAUGHT)
    if (ex != null) {
        Framework_Logger.handleScriptError(
            message, messageLogFactory.getMessageLog(message), ex, scriptErrorLocation, true)
    }

    return message
}