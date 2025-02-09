package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Constants

class Framework_Logger {
    Map entries = [
        headers   : [],
        properties: [],
        history   : []
    ]
    Message message
    MessageLog messageLog
    String tracePoint
    String logLevel
    Integer logCounter
    String overallLogLevel

    // Default settings + new fields for custom header keys and char limit
    Map settings = [
        attachmentsDisabled         : false,
        attachmentLimit             : "5",
        charLimit                   : "1000",
        defaultLogLevel             : "INFO",
        defaultOverallLogLevel      : "TRACE",
        environment                 : "Production",
        customHeaderCharLimit       : "50",
        customHeaderExtraKeys       : "10",
        customHeaderBatchSummaryLimit  : "2000",
    ]
    Map systemInfo = [:]

    // Value-mapping agencies / placeholders
    static final String CONTENT_TYPE_JSON = "application/json"
    static final String CUSTOM_STATUS_FAILED   = "Failed"
    static final int CUSTOM_HEADER_CHAR_LIMIT  = 200
    private Map<String, String> metadataCache = [:]

    Framework_Logger(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.systemInfo = getSystemDetails()
        initSettingsFromGlobalMetadata()
    }

    /**
     * Initialize various settings and defaults using the VM_Framework_Global_Metadata value mapping in
     * the IP_FoundationFramework package. Cache the values in memory.
     */
    private def void initSettingsFromGlobalMetadata() {
        if (metadataCache.isEmpty()) {
            metadataCache["attachmentsDisabled"]    = frameworkVM("setting_attachmentsDisabled", "false")
            metadataCache["attachmentLimit"]        = frameworkVM("setting_attachmentLimit", "5")
            metadataCache["charLimit"]              = frameworkVM("setting_logCharLimit", "1000")
            metadataCache["customHeaderCharLimit"]  = frameworkVM("setting_customHeaderCharLimit", "50")
            metadataCache["customHeaderExtraKeys"]  = frameworkVM("setting_customHeaderExtraKeys", "10")
            metadataCache["defaultLogLevel"]        = frameworkVM("setting_defaultLogLevel", "INFO")
            metadataCache["defaultOverallLogLevel"] = frameworkVM("setting_defaultOverallLogLevel", "TRACE")
            metadataCache["environment"]            = frameworkVM("meta_environment", "Production")
        }
        this.settings.attachmentsDisabled = metadataCache["attachmentsDisabled"]
        this.settings.attachmentLimit = metadataCache["attachmentLimit"]
        this.settings.charLimit = metadataCache["charLimit"]
        this.settings.defaultLogLevel = metadataCache["defaultLogLevel"]
        this.settings.defaultOverallLogLevel = metadataCache["defaultOverallLogLevel"]
        this.settings.environment = metadataCache["environment"]
        this.settings.customHeaderCharLimit = metadataCache["customHeaderCharLimit"]
        this.settings.customHeaderExtraKeys = metadataCache["customHeaderExtraKeys"]
    }

    def logMessage(String tracePoint, String logLevel, String logData) {
        updateLogCounter()
        this.tracePoint = tracePoint
        this.logLevel = logLevel ?: this.settings.defaultLogLevel

        def label = "#${this.logCounter} ${tracePoint}_LOG"

        def properties = this.message.getProperties()
        def headers    = this.message.getHeaders()
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

            def errorLocation = properties.get("errorLocation")
            def errorStepID = message.getProperty(Constants.Property.SAP_ERR_STEP_ID)
            if (this.logLevel == "ERROR") {
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
        def ATTACH_SOFT_LIMIT = 5  // Softer limit for normal attachments (start/end logs)
        def ATTACH_TRACE_LIMIT = -1  // Limit for trace attachments (DISABLED!)

        if (this.settings.attachmentsDisabled?.toBoolean() || this.logCounter >= ATTACH_HARD_LIMIT) {
            return false
        }
        if (tracePoint == "ERROR" ) {
            return true
        }
        if (tracePoint == "END") {
            return this.logCounter <= (ATTACH_SOFT_LIMIT + 1)
        }
        if (["START", "LOG_BATCH_SUMMARY", "INFO_CUSTOM", "WARN_CUSTOM"].contains(tracePoint)) {
            return this.logCounter <= ATTACH_SOFT_LIMIT
        }
        if (tracePoint == "TRACE") {
            return (this.overallLogLevel == "TRACE" && this.logCounter <= ATTACH_TRACE_LIMIT)
        }
        return false
    }

    def attachBodyWithLabel(String body, String label, String fallbackSimpleLogMsg = "") {
        try {
            def log = getOrCreateMessageLog()
            if (log != null) {
                if (!this.settings.attachmentsDisabled?.toBoolean()) {
                    log.addAttachmentAsString(label, body, CONTENT_TYPE_JSON)
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

    def static String normalizeLogLevel(String level) {
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
                MessageLog log = getOrCreateMessageLog()
                addStandardMetadataProperties(log, standardMetadata)
                addCustomMetadataPrimaryKey(log, customMetadata)

                def customKeysProp = properties.get("customHeaderPropertyKeys") as String
                addUserDefinedCustomHeaders(log, customMetadata, customKeysProp)
            } catch (Exception e) {
                handleScriptError(message, messageLog, e, "Framework_Logger.addHeaderProperties", false)
            }

            def combinedHeader = standardMetadata + customMetadata
            if (includeSystemInfo && this.systemInfo && this.systemInfo.size() > 0) {
                combinedHeader = combinedHeader + [systemInfo: this.systemInfo]
            }

            return removeEmptyFields(combinedHeader)
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.prepareHeaderData")
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
        def isRetryAttempt = properties.get(Constants.Property.SAP_IS_REDELIVERY_ENABLED) != null &&
                             properties.get(Constants.Property.SAP_IS_REDELIVERY_ENABLED).toBoolean() != false

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
     * Adds standard metadata fields to custom header properties, each truncated to the configured char limit.
     */
    private void addStandardMetadataProperties(MessageLog log, Map<String, Object> standardMetadata) {
        if (!log || !standardMetadata) return

        Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS.each { f ->
            def val = standardMetadata[f]
            if (val) {
                def safeVal = prepareString(val.toString(), this.settings.customHeaderCharLimit)
                log.addCustomHeaderProperty("meta_${f}", safeVal)
            }
        }
    }

    /**
     * If the user set 'meta_attribute_businessId', add it as 'meta_businessId' to the header.
     */
    private void addCustomMetadataPrimaryKey(MessageLog log, Map customMetadata) {
        if (!log || !customMetadata) return

        def pkVal = customMetadata.businessId ?: customMetadata.businessID
        if (pkVal) {
            def safeVal = prepareString(pkVal.toString(), this.settings.customHeaderCharLimit)
            log.addCustomHeaderProperty("meta_businessId", safeVal)
        }
    }

    /**
     * Adds meta_attributes as custom header properties, prioritizing any keys specified by exchange 
     * property 'customHeaderPropertyKeys' (comma-sep), up to a configured number (default 5) and 
     * values up to a configured character limit (default 50) for each value.
     */
    private void addUserDefinedCustomHeaders(MessageLog log, Map customMetadata, String customKeysProp) {
        if (!log || !customMetadata || !customKeysProp) return

        // Parse user keys, only keep valid ones from customMetadata
        def userKeys = customKeysProp
            .split(",")
            .collect { it.trim() }
            .findAll { it && customMetadata.containsKey(it) }

        // Subtract user keys from the set of all metadata keys
        def leftoverKeys = customMetadata.keySet() - userKeys

        // Concatenate user keys + leftover, up to configured limit
        def maxKeys = (this.settings.customHeaderExtraKeys ?: "5").toInteger()
        def finalKeys = (userKeys + leftoverKeys).take(maxKeys)

        // Add each to custom header properties
        finalKeys.each { k ->
            def val = customMetadata[k]
            if (val) {
                def safeVal = prepareString(val.toString(), this.settings.customHeaderCharLimit)
                log.addCustomHeaderProperty("meta_${k}", safeVal)
            }
        }
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
        Framework_ValueMaps.interfaceVM(key, defaultValue, message, messageLog)
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

    static void handleScriptError(
        Message message, 
        MessageLog messageLog, 
        Exception e, 
        String function, 
        boolean printStackTrace = false, 
        String customData = ""
    ) {
        def errorLabel = "${Constants.ILCD.EXC_PREFIX}-${function}_${Constants.ILCD.EXC_SUFFIX}"
        def errorMessage = "Function: ${function}\nMessage: ${e.message}\n"

        if (customData) {
            errorMessage += "${customData}\n"
        }
        if (printStackTrace) {
            errorMessage += "Stack Trace:\n" + Framework_ExceptionHandler.getStackTrace(e)
        }

        def charLimit = message.getProperty("charLimit")?.toInteger() ?: 1000
        if (errorMessage.length() > charLimit) {
            errorMessage = errorMessage.substring(0, charLimit) + "... (truncated)"
        }

        if (messageLog) {
            messageLog.addAttachmentAsString(errorLabel, errorMessage, "text/plain")
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}", "true")
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.getClass()?.getName())
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.getClass()?.getName())
        }
        // logger.error("${errorLabel} occurred: ${e.message}", e) // If needed
    }

    private def Map getSystemDetails() {
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
}
