package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import src.main.resources.script.Framework_ExceptionHandler

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
    static final String VM_INPUT          = "Input"
    static final String VM_OUTPUT         = "Output"
    static final String VM_GLOBAL_INPUT   = "IP_FoundationFramework"
    static final String VM_GLOBAL_OUTPUT  = "VM_Framework_Global_Metadata"
    static final String CONTENT_TYPE_JSON = "application/json"
    static final String LOG_COUNTER_PROPERTY = "framework_internal_log_counter"
    static final String CUSTOM_STATUS_FAILED   = "Failed"
    static final String ILCD_ERROR_PREFIX      = "ILCD_EXC"
    static final String ILCD_ERROR_SUFFIX      = "SCRIPT_ERROR"
    static final int CUSTOM_HEADER_CHAR_LIMIT  = 200
    static final String ATTACH_DISABLED_WARNING     = "LOG_ATTACHMENTS_DISABLED"
    static final String ATTACH_DISABLED_WARNING_MSG = 
        "Log attachments are currently DISABLED. To enable, change the `setting_attachmentsDisabled` flag in the ${VM_GLOBAL_OUTPUT} value map."

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
            metadataCache["attachmentsDisabled"]    = safeGetValueMapping("setting_attachmentsDisabled", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "false")
            metadataCache["attachmentLimit"]        = safeGetValueMapping("setting_attachmentLimit", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "5")
            metadataCache["charLimit"]              = safeGetValueMapping("setting_logCharLimit", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "1000")
            metadataCache["customHeaderCharLimit"]  = safeGetValueMapping("setting_customHeaderCharLimit", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "50")
            metadataCache["customHeaderExtraKeys"]  = safeGetValueMapping("setting_customHeaderExtraKeys", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "10")
            metadataCache["defaultLogLevel"]        = safeGetValueMapping("setting_defaultLogLevel", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "INFO")
            metadataCache["defaultOverallLogLevel"] = safeGetValueMapping("setting_defaultOverallLogLevel", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "TRACE")
            metadataCache["environment"]            = safeGetValueMapping("meta_environment", VM_GLOBAL_INPUT, VM_GLOBAL_OUTPUT, "Production")
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
        def projectName = properties.get("projectName")
        def integrationID = properties.get("integrationID")

        def mpl_logLevel = this.message.getProperty("SAP_MPL_LogLevel_Internal")
        def vm_logLevel  = getValueMapping(VM_INPUT, projectName, "meta_logLevel", VM_OUTPUT, integrationID)
        this.overallLogLevel = mpl_logLevel ?: vm_logLevel ?: this.settings.defaultOverallLogLevel

        def messageLog = properties.get("messageLog")

        if (isLoggable(this.overallLogLevel, this.logLevel)) {
            def messageId = this.message.getProperty("SAP_MessageProcessingLogID")
            def errorLocation = properties.get("errorLocation")
            def errorStepID = message.getProperty("SAP_ErrorModelStepID")
            
            if (this.logLevel?.toUpperCase() == "ERROR" && !errorLocation) {
                def ex = this.message.getProperty("CamelExceptionCaught")
                if (ex != null) {
                    def exClass = ex?.getClass() ? ex.getClass()?.getCanonicalName() : ex?.getMessage()
                    errorLocation = "SAP_ErrorModelStepID: ${errorStepID} | BTP CI Exception ${exClass}"
                }
            }

            def itemData = [logLevel: this.logLevel]
            if (logData) {
                itemData += [text: logData]
            }
            if (this.logLevel == "ERROR") {
                itemData += [errorLocation: errorLocation, errorStepID: errorStepID]
            }

            def customLogFields = parseCustomAttributes(properties)
            def combinedLogMessage = [itemData].collect { it + customLogFields }
            def combinedLogMessages = (messageLog == null) ? combinedLogMessage : (messageLog + combinedLogMessage)
            this.message.setProperty("messageLog", combinedLogMessages)

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
        def projectName  = properties.get("projectName")
        def integrationID = properties.get("integrationID")

        def headerData = prepareHeaderData(projectName, integrationID, includeSystemInfo)

        def messageLog = properties.get("messageLog")
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
        def ATTACH_TRACE_LIMIT = 3  // Limit for trace attachments 

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
                    log.setStringProperty(ATTACH_DISABLED_WARNING, ATTACH_DISABLED_WARNING_MSG)
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

        metadata["SAP_MessageType"]     = headers.get("SAP_MessageType")
        metadata["SAP_ApplicationID"]   = headers.get("SAP_ApplicationID")

        def replicationGroupId = metadata["SAP_MessageType"]?.startsWith("rmid") ? metadata["SAP_MessageType"] :
                                 metadata["SAP_ApplicationID"]?.startsWith("rmid") ? metadata["SAP_ApplicationID"] : null
        if (replicationGroupId) {
            metadata["ReplicationGroupMessageID"] = replicationGroupId
        }

        if (!headers.get("SAP_Sender") || headers.get("SAP_Sender") != metadata.sourceSystemName) {
            this.message.setHeader("SAP_Sender", metadata.sourceSystemName)
        }
        if (!headers.get("SAP_Receiver") || headers.get("SAP_Receiver") != metadata.targetSystemName) {
            this.message.setHeader("SAP_Receiver", metadata.targetSystemName)
        }

        if (!properties.get("SAP_MessageProcessingLogCustomStatus") && this.logLevel == "ERROR") {
            this.message.setProperty("SAP_MessageProcessingLogCustomStatus", CUSTOM_STATUS_FAILED)
        }

        return metadata
    }

    /**
     * This is to ensure that the required fields are set by developers. 
     */
    private void validateMetadataIdentifiers(String projectName, String integrationID) {
        if (!projectName || !integrationID) {
            throw new FrameworkMetadataException(
                "Undefined framework identifier values - projectName: ${projectName} | integrationID: ${integrationID}."
            )
        }
    }

    def prepareHeaderData(String projectName, String integrationID, boolean includeSystemInfo = false) {
        try {
            validateMetadataIdentifiers(projectName, integrationID)

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

        def mpl_logLevel = properties.get("SAP_MPL_LogLevel_Internal")
        def vm_logLevel  = getValueMapping(VM_INPUT, projectName, "meta_logLevel", VM_OUTPUT, integrationID)
        def tenantName   = this.systemInfo?.tenantName ?: System.getenv("TENANT_NAME") ?: this.settings.environment
        def isRetryAttempt = properties.SAP_isComponentRedeliveryEnabled != null &&
                             properties.SAP_isComponentRedeliveryEnabled.toBoolean() != false

        return [
            projectName                : safeGetValueMapping("meta_projectName", projectName, integrationID, projectName),
            systemName                 : safeGetValueMapping("meta_systemName", projectName, integrationID, tenantName),
            messageID                  : properties.get("SAP_MessageProcessingLogID"),
            correlationID              : headers.get("SAP_MplCorrelationId"),
            environment                : this.settings.environment ?: safeGetValueMapping("meta_environment", projectName, integrationID, tenantName),
            timestamp                  : LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                                             .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
            sourceSystemName           : safeGetValueMapping("meta_sourceSystemName", projectName, integrationID),
            targetSystemName           : safeGetValueMapping("meta_targetSystemName", projectName, integrationID),
            sourceSystemConnectionType : safeGetValueMapping("meta_sourceSystemConnectionType", projectName, integrationID),
            targetSystemConnectionType : safeGetValueMapping("meta_targetSystemConnectionType", projectName, integrationID),
            processArea                : safeGetValueMapping("meta_processArea", projectName, integrationID,    // Check for processArea key first and fallback to stream
                                            safeGetValueMapping("meta_stream", projectName, integrationID)),    // `stream` gets mapped to `processArea`
            integrationID              : safeGetValueMapping("meta_integrationID", projectName, integrationID, integrationID),
            integrationName            : safeGetValueMapping("meta_integrationName", projectName, integrationID),
            packageName                : safeGetValueMapping("meta_packageName", projectName, integrationID),
            iFlowName                  : safeGetValueMapping("meta_iFlowName", projectName, integrationID),
            logLevel                   : normalizeLogLevel(mpl_logLevel ?: vm_logLevel ?: this.settings.defaultOverallLogLevel),
            priority                   : safeGetValueMapping("meta_priority", projectName, integrationID),
            Retry                      : isRetryAttempt
        ]
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
        def jsonObject   = new JsonSlurper().parseText(jsonString)
        def mappedValue  = getValueMapping(VM_INPUT, projectName, "loggerSensitiveFields", VM_OUTPUT, integrationID) ?: ""
        def sensitiveFields = mappedValue.split(',').collect { it.trim().toLowerCase() }
        jsonObject = maskSensitiveFields(jsonObject, sensitiveFields)
        return JsonOutput.prettyPrint(JsonOutput.toJson(jsonObject))
    }

    private def maskSensitiveFields(def json, List<String> sensitiveFields) {
        if (json instanceof Map) {
            json.each { key, value ->
                if (sensitiveFields.contains(key.toLowerCase())) {
                    int maskLength = (value instanceof String) ? Math.min(16, value.length()) : 8
                    json[key] = '*' * maskLength
                } else {
                    json[key] = maskSensitiveFields(value, sensitiveFields)
                }
            }
        } else if (json instanceof List) {
            json = json.collect { maskSensitiveFields(it, sensitiveFields) }
        }
        return json
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

    private String safeGetValueMapping(String key, String projectName, String integrationID, String defaultValue = null) {
        try {
            return getValueMapping(VM_INPUT, projectName, key, VM_OUTPUT, integrationID) ?: defaultValue
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.safeGetValueMapping", false)
            return defaultValue
        }
    }

    def getValueMapping(String srcAgency, String srcId, String srcKey, String targetAgency, String targetId) {
        try {
            def api = ITApiFactory.getApi(ValueMappingApi.class, null)
            return api?.getMappedValue(srcAgency, srcId, srcKey, targetAgency, targetId) ?: ""
        } catch (Exception e) {
            def fields = """\n\n
                Source Agency:      ${srcAgency}\n
                Source Identifier:  ${srcId}\n
                Source Value:       ${srcKey}\n
                Target Agency:      ${targetAgency}\n
                Target Identifier:  ${targetId}\n
            """
            handleScriptError(message, messageLog, e, "Framework_Logger.getValueMapping", true, fields)
            throw e
        }
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
            def current = this.message.getProperty(LOG_COUNTER_PROPERTY)?.toInteger() ?: 0
            this.logCounter = current + 1
            this.message.setProperty(LOG_COUNTER_PROPERTY, "${this.logCounter}")
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

    /**
     * Now actually returns property info (not headers).
     */
    private def List getFullPropertyList() {
        try {
            return message.getProperties()
                .findAll { String key, Object value -> !(value instanceof InputStream) }
                .collect { String key, Object value ->
                    ([(key): "${value}"] + (value instanceof String ? [] : [type: value?.getClass()?.getName()]))
                }
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.getFullPropertyList", true)
        }
    }

    private def List getFullHeaderList() {
        try {
            return message.getHeaders().collect { String key, Object entry ->
                def value = (entry instanceof InputStream) ? message.getHeader(key, Reader)?.getText() : entry
                return ([(key): "${value}"] + (value instanceof String ? [] : [type: value.getClass().getName()]))
            }
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.getFullHeaderList", true)
        }
    }

    private void handleValidationError(Exception e, String function, boolean critical = false) {
        handleScriptError(this.message, this.messageLog, e, function, true)
        if (critical) throw e
    }

    static void handleScriptError(Message message, MessageLog messageLog, Exception e, String function,
                                  boolean printStackTrace = false, String customData = "") {
        def logger = LoggerFactory.getLogger("Framework_Logger")
        def errorLabel = "${ILCD_ERROR_PREFIX}-${function}_${ILCD_ERROR_SUFFIX}"
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
            messageLog.addCustomHeaderProperty("${ILCD_ERROR_PREFIX}", "true")
            messageLog.addCustomHeaderProperty("${ILCD_ERROR_PREFIX}-ScriptName", function)
            messageLog.addCustomHeaderProperty("${ILCD_ERROR_PREFIX}-ErrorClass", e.getClass()?.getName())
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

    static class FrameworkMetadataException extends RuntimeException {
        FrameworkMetadataException(String message) {
            super(message)
        }
        Throwable getCause() {
            return new Throwable("FrameworkMetadataException: Invalid metadata configuration for projectName/integrationID.")
        }
    }
}
