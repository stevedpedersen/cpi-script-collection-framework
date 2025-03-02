package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonBuilder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Constants

/**
 *
 */
class Framework_Logger {

    Message message
    MessageLog messageLog
    String tracePoint
    String logLevel
    Integer logCounter
    String overallLogLevel
    Framework_ValueMaps vmApi
    Map systemInfo = [:]

    Framework_Logger(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.settings = Framework_Settings.initializeFromValueMap(message, messageLog)
        this.systemInfo = getSystemDetails()
    }

    def logMessage(String tracePoint, String logLevel, String logData) {
        updateLogCounter()
        this.tracePoint = tracePoint
        this.logLevel = logLevel ?: this.settings.defaultLogLevel

        def label = "#${this.logCounter} ${tracePoint}_LOG"

        def properties = this.message.getProperties()
        def projectName = properties.get(Constants.ILCD.VM_SRC_ID)
        def integrationID = properties.get(Constants.ILCD.VM_TRGT_ID)
        def messageLog = properties.get(Constants.ILCD.LOG_STACK_PROPERTY)
        def mpl_logLevel = this.message.getProperty(Constants.Property.MPL_LEVEL_INTERNAL)
        def vm_logLevel  = interfaceVM("meta_logLevel", projectName, integrationID)
        this.overallLogLevel = mpl_logLevel ?: vm_logLevel ?: this.settings.defaultOverallLogLevel

        // Begin constructing the log entry
        if (isLoggable(this.overallLogLevel, this.logLevel)) {
            def itemData = [logLevel: this.logLevel, text: (logData ? logData : "Log created at ${getTimestamp()}")]
            if (logData) {
                itemData += [text: logData]
            }

            if (this.logLevel == "ERROR") {
                def errorLocation = properties.get("errorLocation")
                def errorStepID = message.getProperty(Constants.Property.SAP_ERR_STEP_ID)
                if (!errorLocation) {
                    def ex = this.message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT)
                    if (ex != null) {
                        def exClass = ex?.getClass() ? ex.getClass()?.getCanonicalName() : ex?.getMessage()
                        errorLocation = "${Constants.Property.SAP_ERR_STEP_ID}: ${errorStepID} | BTP CI Exception ${exClass}"
                    }
                }
                itemData += [errorLocation: errorLocation, errorStepID: errorStepID]
            }

            def customLogFields = parseCustomAttributes(properties)
            def combinedLogMessage = [itemData].collect { it + customLogFields }
            def combinedLogMessages = (messageLog == null) ? combinedLogMessage : (messageLog + combinedLogMessage)
            this.message.setProperty(Constants.ILCD.LOG_STACK_PROPERTY, combinedLogMessages)

            // def itemMessageLog = itemData + customLogFields
            def headerData = prepareHeaderData(projectName, integrationID) ?: [:]
            def jsonData   = headerData + [messages: combinedLogMessages]
            def cleanedJson = new JsonBuilder(removeEmptyFields(jsonData)).toPrettyString()
            def maskedMessage = mask(cleanedJson, projectName, integrationID)

            if (isAttachable(tracePoint)) {
                attachBodyWithLabel(maskedMessage, label, itemData?.text)
            } 
            this.messageLog.setStringProperty(label, logData)

            // Clean up ephemeral properties
            this.message.setProperty("logLevel", "") // @deprecated - use dedicated scripts instead
            this.message.setProperty("text", "")
        }
    }

    def String conclude() {
        return conclude(false)
    }

    def String conclude(boolean attachToMpl, boolean includeSystemInfo = false) {
        updateLogCounter()

        def properties   = this.message.getProperties()
        def projectName  = properties.get(Constants.ILCD.VM_SRC_ID)
        def integrationID = properties.get(Constants.ILCD.VM_TRGT_ID)

        def headerData = prepareHeaderData(projectName, integrationID, includeSystemInfo)

        def messageLog = properties.get(Constants.ILCD.LOG_STACK_PROPERTY)
        def messageLogEntries = []

        if (messageLog instanceof String) {
            messageLogEntries = messageLog.split(',')
        } else if (messageLog instanceof List) {
            messageLogEntries = messageLog
        }

        def jsonData = headerData + [messages: messageLogEntries]
        def cleanedJson = new JsonBuilder(removeEmptyFields(jsonData)).toPrettyString()
        def maskedMessage = mask(cleanedJson, projectName, integrationID)

        if (attachToMpl && isAttachable("FULL_LOG")) {
            attachBodyWithLabel(maskedMessage, "#${this.logCounter} FULL_LOG")
        }

        return maskedMessage
    }

    def boolean isAttachable(String tracePoint) {
        def ATTACH_HARD_LIMIT = 10 // Hard limit for all attachments
        def ATTACH_SOFT_LIMIT = settings?.attachmentLimit?.isInteger() ? settings.attachmentLimit.toInteger() : 5
        def ATTACH_TRACE_LIMIT = settings?.traceAttachmentLimit?.isInteger() ? settings.traceAttachmentLimit.toInteger() : 3

        if (toBool(settings.attachmentsDisabled) || this.logCounter >= ATTACH_HARD_LIMIT) {
            return false
        }
        if (tracePoint == "ERROR" ) {
            return true
        }
        if (tracePoint == "END") {
            return this.logCounter <= (ATTACH_SOFT_LIMIT + 1) // +1 allows for remaining end log
        }
        // everything but trace/debug or conclude logs if overall count < soft limit
        if (!["TRACE", "FULL_LOG", "TRACE_CUSTOM", "DEBUG_CUSTOM"].contains(tracePoint)) { 
            return this.logCounter <= ATTACH_SOFT_LIMIT
        }
        // everything but conclude logs if overall count < trace limit
        if (["TRACE", "TRACE_CUSTOM", "DEBUG_CUSTOM"].contains(tracePoint)) {
            return (["TRACE", "DEBUG"].contains(this.overallLogLevel) && this.logCounter <= ATTACH_TRACE_LIMIT)
        }
        return false
    }

    def attachBodyWithLabel(String body, String label, String fallbackSimpleLogMsg = "") {
        try {
            def log = getOrCreateMessageLog()
            if (log != null) {
                if (!toBool(settings.attachmentsDisabled)) {
                    log.addAttachmentAsString(label, body, Constants.Header.CONTENT_TYPE_JSON)
                } else {
                    def simpleLog = fallbackSimpleLogMsg ? fallbackSimpleLogMsg : body
                    log.setStringProperty(Constants.ILCD.ATTACH_DISABLED, Constants.ILCD.ATTACH_DISABLED_MSG)
                    log.setStringProperty(label, 
                        simpleLog?.size() <= 30 ? simpleLog.substring(0, 27) + "..." : simpleLog)
                }
            }
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.attachBodyWithLabel", true)
        }
    }

    def isLoggable(String overallLogLevel, String logLevel) {
        def configuredLogLevel = normalizeLogLevel(overallLogLevel)
        def itemLogLevel       = normalizeLogLevel(logLevel) ?: this.settings.defaultLogLevel

        def logLevelsHierarchy = [
            "TRACE": 0,
            "DEBUG": 0,
            "INFO" : 1,
            "WARN" : 2,
            "ERROR": 3,
            "SUCCESS": 1
        ]
        return logLevelsHierarchy[itemLogLevel] >= logLevelsHierarchy[configuredLogLevel]
    }

    def static String normalizeLogLevel(String level, Framework_Logger loggerInstance) {
        return loggerInstance?.normalizeLogLevel(level) ?: loggerInstance?.settings?.defaultLogLevel
    }

    def String normalizeLogLevel(String level) {
        switch (level?.toUpperCase()) {
            case ["E", "ERROR"]:
                return "ERROR"
            case ["W", "WARN"]:
                return "WARN"
            case ["I", "INFO", "S", "SUCCESS"]:
                return "INFO"
            case ["D", "DEBUG", "T", "TRACE"]:
                return "TRACE"
            default:
                return this.settings?.defaultLogLevel ?: "INFO"
        }
    }

    def updateMetadataAndStandardHeaders(Map metadata) {
        Map headers    = this.message.getHeaders()
        Map properties = this.message.getProperties()

        def msgType = headers.get(Constants.Header.SAP_MESSAGE_TYPE)
        def appId   = headers.get(Constants.Header.SAP_APP_ID)
        metadata[Constants.Header.SAP_MESSAGE_TYPE] = msgType
        metadata[Constants.Header.SAP_APP_ID]       = appId

        def replicationGroupId = msgType?.startsWith("rmid") ? msgType : (appId?.startsWith("rmid") ? appId : null)
        if (replicationGroupId) {
            metadata[Constants.Property.AEM_RMID] = replicationGroupId
        }
        if (!headers.get(Constants.Header.SAP_SENDER) || headers.get(Constants.Header.SAP_SENDER) != metadata.sourceSystemName) {
            this.message.setHeader(Constants.Header.SAP_SENDER, metadata.sourceSystemName)
        }
        if (!headers.get(Constants.Header.SAP_RECEIVER) || headers.get(Constants.Header.SAP_RECEIVER) != metadata.targetSystemName) {
            this.message.setHeader(Constants.Header.SAP_RECEIVER, metadata.targetSystemName)
        }
        if (!properties.get(Constants.Property.MPL_CUSTOM_STATUS) && this.logLevel == "ERROR") {
            this.message.setProperty(Constants.Property.MPL_CUSTOM_STATUS, "Failed")
        }
        return metadata
    }

    def prepareHeaderData(String projectName, String integrationID, boolean includeSystemInfo = false) {
        try {
            Framework_ValueMaps.validateMetadataIdentifiers(projectName, integrationID)

            Map<String, Object> standardMetadata = buildStandardMetadata(projectName, integrationID)
            standardMetadata = updateMetadataAndStandardHeaders(standardMetadata)

            Map properties = message.getProperties()
            def customMetadata = buildCustomMetadata(properties)

            try {
                addStandardMetadataProperties(standardMetadata)
                addCustomMetadataPrimaryKey(customMetadata)
                addUserDefinedCustomHeaders(customMetadata)
            } catch (Exception e) {
                handleScriptError(message, messageLog, e, "Framework_Logger.prepareHeaderData", true)
            }

            def combinedHeader = standardMetadata + customMetadata
            if (includeSystemInfo && this.systemInfo && this.systemInfo.size() > 0) {
                combinedHeader = combinedHeader + [systemInfo: this.systemInfo]
            }

            return removeEmptyFields(combinedHeader)
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.prepareHeaderData", true)
        }
    }

    /**
     * Builds the standard metadata map.
     */
    private Map<String, Object> buildStandardMetadata(String projectName, String integrationID) {
        Map properties = message.getProperties()
        Map headers    = message.getHeaders()

        def mpl_logLevel = properties.get(Constants.Property.MPL_LEVEL_INTERNAL)
        def tenantName   = this.systemInfo?.tenantName ?: System.getenv("TENANT_NAME") ?: this.settings.environment
        def isRetryAttempt = toBool(properties.get(Constants.Property.SAP_IS_REDELIVERY_ENABLED))

        return [
            projectName                : interfaceVM("meta_projectName", projectName, integrationID, projectName),
            systemName                 : interfaceVM("meta_systemName", projectName, integrationID, tenantName),
            messageID                  : properties.get(Constants.Property.SAP_MPL_ID),
            correlationID              : headers.get(Constants.Header.SAP_CORRELATION_ID),
            environment                : this.settings.environment ?: interfaceVM("meta_environment", projectName, integrationID, tenantName),
            timestamp                  : getTimestamp(),
            sourceSystemName           : interfaceVM("meta_sourceSystemName", projectName, integrationID),
            targetSystemName           : interfaceVM("meta_targetSystemName", projectName, integrationID),
            sourceSystemConnectionType : interfaceVM("meta_sourceSystemConnectionType", projectName, integrationID),
            targetSystemConnectionType : interfaceVM("meta_targetSystemConnectionType", projectName, integrationID),
            processArea                : interfaceVM("meta_processArea", projectName, integrationID,    // Check for processArea key first and fallback to stream
                                            interfaceVM("meta_stream", projectName, integrationID)),    // `stream` gets mapped to `processArea`
            integrationID              : interfaceVM("meta_integrationID", projectName, integrationID, integrationID),
            integrationName            : interfaceVM("meta_integrationName", projectName, integrationID),
            packageName                : interfaceVM("meta_packageName", projectName, integrationID),
            iFlowName                  : interfaceVM("meta_iFlowName", projectName, integrationID),
            logLevel                   : normalizeLogLevel(mpl_logLevel ?: this.overallLogLevel ?: this.settings.defaultOverallLogLevel),
            priority                   : interfaceVM("meta_priority", projectName, integrationID),
            Retry                      : isRetryAttempt
        ]
    }

    private String getTimestamp(String format = "yyyy-MM-dd HH:mm:ss.SSS") {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format))
    }

    /**
     * Adds standard metadata fields to custom header properties
     */
    private void addStandardMetadataProperties(MessageLog log, Map<String, Object> standardMetadata) {
        if (!standardMetadata) return
        Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS.each { fieldName ->
            writeMetaCustomHeaderProperty(fieldName, standardMetadata[fieldName])
        }
    }

    /**
     * Adds custom header 'meta_businessId' using likely values for it, starting with property
     * 'meta_attribute_businessId'.
     */
    private void addCustomMetadataPrimaryKey(Map customMetadata) {
        if (!customMetadata) return
        def altKey = message.headers[Constants.Header.SAP_MESSAGE_TYPE] 
            ?: message.properties[Constants.ILCD.Batch.JOB_ID]
            ?: message.properties["businessId"] ?: message.properties["businessID"]
        def pkVal = customMetadata.businessId ?: customMetadata.businessID ?: altKey
        writeMetaCustomHeaderProperty("businessId", pkVal)
    }

    /**
     * Adds meta_attributes plus any additional specified properties as custom header properties. 
     * Property names specified on property 'customHeaderPropertyKeys' (comma-sep), are used to do
     * a lookup at runtime. 
     */
    private void addUserDefinedCustomHeaders(Map customMetadata) {
        if (!customMetadata) return
        def customKeysProp = properties["customHeaderPropertyKeys"] as String
        def userKeys = (customKeysProp ?: "").split(",").findAll { message.properties[it] != null } 
        def userProps = (userKeys ?: []).collectEntries { [(it): message.properties[it]] }
        def finalProps = userProps + customMetadata
        
        finalProps.remove("businessId")                         // already added businessId, exclude it 
        finalProps.keySet()                                     // use keys for uniqueness check
            .toUnique()                                         // and don't repeat standard metadata
            .minus(Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS)
            .take(settings.customHeaderExtraKeys)               
            .each { key -> 
                writeMetaCustomHeaderProperty(key, finalProps[key]) 
            }
    }
    /**
     * SAP limits custom header props to 200 chars, but we check for any lower limit set in global VM.
     */
    private void writeMetaCustomHeaderProperty(String key, String value) {
        MessageLog log = getOrCreateMessageLog()
        if (!log || !key || !value) return
        def metaKey = "${Constants.ILCD.META_CH_PREFIX}${key}"
        def metaValue = prepareString(value.toString(), settings.customHeaderCharLimit) 
        log.addCustomHeaderProperty(metaKey, metaValue)
    }

    def removeEmptyFields(def object) {
        if (object instanceof Map) {
            return object
                .findAll { it.value != null && !(it.value instanceof String && it.value.trim().isEmpty()) }
                .collectEntries { [it.key, removeEmptyFields(it.value)] }
        } else if (object instanceof List) {
            return object.collect { removeEmptyFields(it) }.findAll { it != null }
        } else {
            return object
        }
    }

    def buildCustomMetadata(Map properties) {
        def header = [:]
        properties.each { key, value ->
            if (key.startsWith("meta_attribute_")) {
                def propertyName = key.substring(15)
                header[propertyName] = value
            }
        }
        return header
    }

    def mask(String jsonString, String projectName, String integrationID) {
        return Framework_Utils.maskFields(jsonString, projectName, integrationID, "loggerSensitiveFields", message, messageLog)
    }

    def parseCustomAttributes(def properties) {
        def logCharacterLimit = this.settings.charLimit ?: "1000"
        def attrs = [:]
        properties.each { key, value ->
            if (key.startsWith("log_") && value != null && value != "") {
                def propertyName = key.substring(4)
                attrs[propertyName] = prepareString(value, logCharacterLimit)
                this.message.setProperty(key, "")
            }
        }
        return attrs
    }

    private String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null) {
        Framework_ValueMaps.interfaceVM(key, projectName, integrationID, defaultValue, message, messageLog)
    }

    private String frameworkVM(String key, String defaultValue = null) {
        Framework_ValueMaps.frameworkVM(key, defaultValue, message, messageLog)
    }

    def static String getStackTrace(Exception e) {
        return Framework_ExceptionHandler.getStackTrace(e)
    }

    private def String prepareString(String value, String logCharacterLimit) {
        return escapeQuotes(truncateString(value, logCharacterLimit))
    }

    private def String truncateString(String value, String logCharacterLimit) {
        def limit = (logCharacterLimit != null) ? Integer.parseInt(logCharacterLimit) : 1000
        return (value?.length() > limit) ? value.substring(0, limit) : value
    }

    private def String escapeQuotes(String value) {
        return value?.replaceAll("\"", "\\\\\"")
    }

    private def void updateLogCounter() {
        try {
            this.logCounter = (this.message.getProperty(Constants.ILCD.LOG_COUNT)?.toInteger() ?: 0) + 1
            this.message.setProperty(Constants.ILCD.LOG_COUNT, "${this.logCounter}")
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.updateLogCounter")
        }
    }

    private MessageLog getOrCreateMessageLog() {
        if (this.messageLog == null) {
            def logFactory = this.message.getProperty("messageLogFactory")
            if (logFactory) {
                this.messageLog = logFactory.getMessageLog(this.message)
            } else {
                throw new IllegalStateException("MessageLogFactory is not available in this context.")
            }
        }
        return this.messageLog
    }

    private void handleValidationError(Exception e, String function, boolean critical = false) {
        handleScriptError(this.message, this.messageLog, e, function, true)
        if (critical) throw e
    }

    private boolean toBool(Object rawValue) {
        if (rawValue == null) return false 
        if (rawValue instanceof Boolean) return (Boolean) rawValue
        return Boolean.parseBoolean(rawValue.toString())
    }

    private Map getSystemDetails() {
        try {
            return [
                javaVersion: System.properties['java.version'],
                groovyVersion: GroovySystem.version,
                tenantName: System.getenv("TENANT_NAME") ?: System.properties['com.sap.it.node.tenant.name'], 
                tenantID: System.getenv("TENANT_NAME") ?: System.properties['com.sap.it.node.tenant.id'],
                cfInstanceID: System.getenv("CF_INSTANCE_INDEX"),
                camelVersion: this.message.exchange.context.version,
                camelUptime: this.message.exchange.context.uptime
            ]
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.getSystemDetails", false)
        }
    }
    /**
     * The default fallback handler in case of exceptions thrown within ILCD Framework code. 
     * Can be queried in the Message Processing Logs via custom headers: "ILCD_EXC=true"
     * 
     * The intent is to log the internal exception while allowing for execution 
     * to continue. This does have a downside in that we get a limited stacktrace.
     */
    static void handleScriptError(
        Message message, 
        MessageLog messageLog, 
        Exception e, 
        String function, 
        boolean printStackTrace = false, 
        String customData = ""
    ) {
        def log4j = LoggerFactory.getLogger("Framework_Logger")
        if (!messageLog) {
            log4j.warn("handleScriptError called with a null MPL reference. Unable to log the exception.")
            return
        }
        if (message.getProperty("ILCD_EXC_inProgress") == true) {
            log4j.error("handleScriptError called recursively. Skipping second attempt to avoid infinite loop.", e)
            return
        }
        message.setProperty("ILCD_EXC_inProgress", true)

        def divider = "—"*25
        def errorLabel = "${Constants.ILCD.EXC_PREFIX}-${function}"
        def errorMessage = new StringBuilder()
        errorMessage << "Script/Function:\t${function}\nException Message:\t${e.message}\n"
        errorMessage << "Cause:\t" + (e.cause ?: "Unknown") + "\n"
        errorMessage << customData ? "Extra Details:\t${customData}\n" : ""
        errorMessage << printStackTrace ? "Stack Trace:\n${truncate(Framework_ExceptionHandler.getStackTrace(e), 2000)}\n" : ""

        // Include Properties and Headers
        appendProperties(errorMessage, "${divider} PROPERTIES ${divider}", message.properties)
        appendProperties(errorMessage, "${divider}  HEADERS  ${divider}—", message.headers)

        try {
            messageLog.addAttachmentAsString(errorLabel, errorMessage.toString(), "text/plain")
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}", "true")
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.getClass()?.getName())
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.getClass()?.getName())
        } catch (Exception innerEx) {
            log4j.error("handleScriptError: Could not attach error; skipping to avoid recursion.", innerEx)
        }
        log4j.error("${errorLabel} occurred: ${e.message}", e)
    }

    static String truncate(String input, int maxLength) {
        return input.length() > maxLength ? input.substring(0, maxLength) + "... (truncated)" : input
    }

    static void appendProperties(StringBuilder sb, String title, Map map) {
        sb.append(title + "\n")
        map.each { key, value ->
            try {
                String truncatedValue = truncate(value?.toString(), 100)
                sb.append(String.format(" %-40s: %-100s\n", key, truncatedValue))
            } catch (Exception ignored) {
                sb.append("Exc for ${key} - ${ignored.message}\n")
            }
        }
        sb.append("\n")
    }
}
