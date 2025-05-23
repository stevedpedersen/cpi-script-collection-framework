package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi

import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants

class Framework_ValueMaps {
	Message message
    MessageLog messageLog
    Framework_Logger logger

    Framework_ValueMaps(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
    }

    /**
     * This is to ensure that the required fields are set by developers. 
     */
    public static void validateMetadataIdentifiers(String projectName, String integrationID) {
        if (!projectName || !integrationID) {
            throw new FrameworkMetadataException(
                "Undefined framework identifier values - projectName: ${projectName} | integrationID: ${integrationID}."
            )
        }
    }

    public static String frameworkVM(String key, String defaultValue = null, Message message, MessageLog messageLog) {
        try {
            def valueMapsInstance = new Framework_ValueMaps(message, messageLog) // ✅ Create an instance
            return valueMapsInstance.getValueMapping(key, Constants.ILCD.VM_GLOBAL_SRC_ID, Constants.ILCD.VM_GLOBAL_TRGT_ID) ?: defaultValue
        } catch (Exception e) {
            // Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.frameworkVM", false)
            return defaultValue
        }
    }

    public static String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null, Message message, MessageLog messageLog) {
        try {
            def valueMapsInstance = new Framework_ValueMaps(message, messageLog)  // ✅ Create an instance
            return valueMapsInstance.getValueMapping(key, projectName, integrationID) ?: defaultValue
        } catch (Exception e) {
            // Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.interfaceVM", false)
            return defaultValue
        }
    }

    def getValueMapping(String srcKey, String srcId, String targetId) {
        try {
            def api = ITApiFactory.getApi(ValueMappingApi.class, null)
            return api?.getMappedValue(Constants.ILCD.VM_SRC_AGENCY, srcId, srcKey, Constants.ILCD.VM_TRGT_AGENCY, targetId) ?: ""
        } catch (Exception e) {
            def fields = """\n\n
                Source Agency:      ${srcAgency}\n
                Source Identifier:  ${srcId}\n
                Source Value:       ${srcKey}\n
                Target Agency:      ${targetAgency}\n
                Target Identifier:  ${targetId}\n
            """
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.getValueMapping", true, fields)
            throw e
        }
    }

    // New method to initialize logger settings from VM
    public Map<String, Object> initializeLoggerSettings() {
        Map<String, Object> settings = [:]
        settings["attachmentsDisabled"] = frameworkVM("setting_attachmentsDisabled", "false", message, messageLog)?.toBoolean()
        settings["attachmentLimit"] = frameworkVM("setting_attachmentLimit", "5", message, messageLog)?.toInteger() ?: 5
        settings["traceAttachmentLimit"] = frameworkVM("setting_traceAttachmentLimit", "3", message, messageLog)?.toInteger() ?: 3
        settings["charLimit"] = frameworkVM("setting_logCharLimit", "1000", message, messageLog)?.toInteger() ?: 1000
        settings["defaultLogLevel"] = frameworkVM("setting_defaultLogLevel", "INFO", message, messageLog) ?: "INFO"
        settings["defaultOverallLogLevel"] = frameworkVM("setting_defaultOverallLogLevel", "TRACE", message, messageLog) ?: "TRACE"
        settings["environment"] = frameworkVM("meta_environment", "Production", message, messageLog) ?: "Production"
        settings["customHeaderCharLimit"] = frameworkVM("setting_customHeaderCharLimit", "150", message, messageLog)?.toInteger() ?: 150
        settings["customHeaderExtraKeys"] = frameworkVM("setting_customHeaderExtraKeys", "10", message, messageLog)?.toInteger() ?: 10

        return settings
    }

    static class FrameworkMetadataException extends RuntimeException {
        FrameworkMetadataException(String message) {
            super(message)
        }
        Throwable getCause() {
            return new Throwable("FrameworkMetadataException: Invalid metadata configuration for projectName/integrationID.")
        }
    }
}