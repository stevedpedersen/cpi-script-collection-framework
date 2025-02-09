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

    public static String frameworkVM(String key, String defaultValue = null) {
        try {
            return getValueMapping(key, Constants.ILCD.VM_GLOBAL_SRC_ID, Constants.ILCD.VM_GLOBAL_TRGT_ID) ?: defaultValue
        } catch (Exception e) {
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.frameworkVM", false)
            return defaultValue
        }
    }

    public static String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null) {
        try {
            return getValueMapping(key, projectName, integrationID) ?: defaultValue
        } catch (Exception e) {
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ValueMaps.safeGetValueMapping", false)
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
}