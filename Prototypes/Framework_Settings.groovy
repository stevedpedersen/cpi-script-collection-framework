package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonBuilder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Constants

class Framework_Settings {
    boolean attachmentsDisabled = false
    int attachmentLimit = 5
    int traceAttachmentLimit = 3
    int charLimit = 1000
    String defaultLogLevel = "INFO"
    String defaultOverallLogLevel = "TRACE"
    String environment = "Production"
    int customHeaderCharLimit = 180
    int customHeaderExtraKeys = 10

    static Framework_Settings initializeFromValueMap(Message message, MessageLog messageLog) {
        def settings = new Framework_Settings()
        Map<String, String> metadataCache = [:]

        // Populate metadataCache using Value Map API
        metadataCache["attachmentsDisabled"]    = frameworkVM("setting_attachmentsDisabled", "false", message, messageLog)
        metadataCache["attachmentLimit"]        = frameworkVM("setting_attachmentLimit", "5", message, messageLog)
        metadataCache["traceAttachmentLimit"]   = frameworkVM("setting_traceAttachmentLimit", "3", message, messageLog)
        metadataCache["charLimit"]              = frameworkVM("setting_logCharLimit", "1000", message, messageLog)
        metadataCache["customHeaderCharLimit"]  = frameworkVM("setting_customHeaderCharLimit", "150", message, messageLog)
        metadataCache["customHeaderExtraKeys"]  = frameworkVM("setting_customHeaderExtraKeys", "10", message, messageLog)
        metadataCache["defaultLogLevel"]        = frameworkVM("setting_defaultLogLevel", "INFO", message, messageLog)
        metadataCache["defaultOverallLogLevel"] = frameworkVM("setting_defaultOverallLogLevel", "TRACE", message, messageLog)
        metadataCache["environment"]            = frameworkVM("meta_environment", "Production", message, messageLog)

        // Initialize settings from metadata cache
        settings.attachmentsDisabled = metadataCache["attachmentsDisabled"]?.toBoolean()
        settings.attachmentLimit = metadataCache["attachmentLimit"]?.toInteger() ?: settings.attachmentLimit
        settings.traceAttachmentLimit = metadataCache["traceAttachmentLimit"]?.toInteger() ?: settings.traceAttachmentLimit
        settings.charLimit = metadataCache["charLimit"]?.toInteger() ?: settings.charLimit
        settings.defaultLogLevel = metadataCache["defaultLogLevel"] ?: settings.defaultLogLevel
        settings.defaultOverallLogLevel = metadataCache["defaultOverallLogLevel"] ?: settings.defaultOverallLogLevel
        settings.environment = metadataCache["environment"] ?: settings.environment
        settings.customHeaderCharLimit = metadataCache["customHeaderCharLimit"]?.toInteger() ?: settings.customHeaderCharLimit
        settings.customHeaderExtraKeys = metadataCache["customHeaderExtraKeys"]?.toInteger() ?: settings.customHeaderExtraKeys

        return settings
    }


    private static String frameworkVM(String key, String defaultValue, Message message, MessageLog messageLog) {

        return defaultValue // Replace with actual fetching logic
    }

    private String frameworkVM(String key, String defaultValue = null) {
        Framework_ValueMaps.frameworkVM(key, defaultValue, message, messageLog)
    }
}