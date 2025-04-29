package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import groovy.json.JsonBuilder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Constants

/**
 * Framework_Logger provides structured logging, error tracking, and metadata enrichment
 * for integration flows. Ensures consistent log formatting, masking, and attachment.
 */
class Framework_Logger {

    Message     message
    MessageLog  messageLog
    String      tracePoint          // Current trace point - e.g. START, END, AFTER_VALIDATION
    String      logLevel            // Current log level - e.g. INFO, ERROR
    Integer     logCounter
    String      overallLogLevel
    Map         systemInfo  = [:]
    Map         settings    = [:]  // Global VM default values

    Framework_Logger(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.logCounter = getLogCounter()
        this.systemInfo = getSystemDetails()
        this.settings   = initSettingsFromGlobalMetadata()
        this.tracePoint = "CONCLUDE"
        this.logLevel   = "INFO"
        this.overallLogLevel = "INFO"
    }

    Map initSettingsFromGlobalMetadata() {
        return [
            environment             : frameworkVM("meta_environment", "Production"),
            attachmentsDisabled     : frameworkVM("setting_attachmentsDisabled", "false"),
            attachmentLimit         : frameworkVM("setting_attachmentLimit", "5"),
            traceAttachmentLimit    : frameworkVM("setting_traceAttachmentLimit", "3"),
            charLimit               : frameworkVM("setting_logCharLimit", "1000"),
            customHeaderCharLimit   : frameworkVM("setting_customHeaderCharLimit", "50"),
            customHeaderExtraKeys   : frameworkVM("setting_customHeaderExtraKeys", "10"),
            defaultLogLevel         : frameworkVM("setting_defaultLogLevel", "INFO"),
            defaultOverallLogLevel  : frameworkVM("setting_defaultOverallLogLevel", "TRACE"),
            allowedPayloadLogPoints : frameworkVM("setting_allowedPayloadLogPoints", ""),
            maxPayloadLines         : frameworkVM("setting_payloadLogMaxLines", "1000"),
        ]
    }

    /**
     * Logs a message with context, error handling, masking, and attaches to message log up to a limit.
     * @param tracePoint   Logical point in the process (e.g. step name)
     * @param logLevel     Log severity (e.g. INFO, ERROR)
     * @param logData      Main log message or details
     */
    def logMessage(String tracePoint, String logLevel, String logData, boolean isStrictErrorCheck = false) {
        def properties    = this.message.getProperties()
        def projectName   = properties.get(Constants.ILCD.VM_SRC_ID)
        def integrationID = properties.get(Constants.ILCD.VM_TRGT_ID)
        def messageLog    = properties.get(Constants.ILCD.LOG_STACK_PROPERTY)
        def mpl_logLevel  = properties.get(Constants.Property.MPL_LEVEL_INTERNAL)
        def vm_logLevel   = interfaceVM("meta_logLevel", projectName, integrationID)

        // Short-circuit: skip soft error scan if already at ERROR level (prevents duplicate detection/status reset)
        def scanResults = Framework_ExceptionHandler.checkForSoftError(this.message)
        debug("soft_err_scan_results", "softError:${scanResults.softError}, isPropagated:${scanResults.isPropagated}", this.messageLog)
        if (isStrictErrorCheck && scanResults.softError && scanResults?.isPropagated != true) {
            Framework_ExceptionHandler.throwCustomException(scanResults.reason, scanResults.message)
        }
        this.overallLogLevel = mpl_logLevel ?: vm_logLevel ?: this.settings.defaultOverallLogLevel
        this.logLevel = (isStrictErrorCheck && scanResults.softError && scanResults?.isPropagated != true ? "ERROR" : logLevel) ?: this.settings.defaultLogLevel
        this.tracePoint = tracePoint
        def attachmentLabel = "#${this.logCounter} ${tracePoint}_LOG"
        // Short-circuit: skip this log if the iFlow's log level is set to a less verbose level
        if (!isLoggable(this.overallLogLevel, this.logLevel)) {
            checkForPayloadLogging(tracePoint, projectName, integrationID) // Still do the payload log though
            return
        }

        try {
            // Begin constructing log entry
            def logEntry = [logLevel: this.logLevel, text: (logData ? logData : "Log created at ${getTimestamp()}")]
            if (this.logLevel == "ERROR") {
                def errorLocation = properties.get("errorLocation")
                def errorStepID = properties.get(Constants.Property.SAP_ERR_STEP_ID)
                def errorType = properties.get(Constants.ILCD.ExceptionHandler.PROP_ERR_TYPE)
                def isSoftError = properties.get(Constants.SoftError.PROP_IS_SOFT_ERROR)
                if (!errorLocation) {
                    def ex = properties.get(Constants.Property.CAMEL_EXC_CAUGHT)
                    if (ex != null) {
                        def exClass = ex?.class ? ex.class?.simpleName ?: ex.class?.canonicalName : ex.message
                        errorLocation = "${Constants.Property.SAP_ERR_STEP_ID}: ${errorStepID} | BTP CI Exception ${exClass}"
                    }
                } // Enhance the error log entry with some extra info.
                logEntry += [errorLocation: errorLocation, errorStepID: errorStepID, errorType: errorType]
            }

            def customLogFields = parseCustomAttributes(properties)
            def combinedLogMessage = [logEntry].collect { it + customLogFields }
            def combinedLogMessages = (messageLog == null) ? combinedLogMessage : (messageLog + combinedLogMessage)
            this.message.setProperty(Constants.ILCD.LOG_STACK_PROPERTY, combinedLogMessages)

            def headerData = prepareHeaderData(projectName, integrationID) ?: [:]
            def jsonData   = headerData + [messages: combinedLogMessages]
            def cleanedJson = new JsonBuilder(removeEmptyFields(jsonData)).toPrettyString()
            def maskedMessage = mask(cleanedJson, projectName, integrationID)
            // Attach log if eligible
            if (isAttachable(tracePoint, attachmentLabel)) {
                attachBodyWithLabel(maskedMessage, attachmentLabel, logEntry?.text, true)
            }
            if (this.messageLog != null) { // Simple debug message for trace mode only
                this.messageLog.setStringProperty(attachmentLabel, logData)
            }
            this.message.setProperty("logLevel", "") // @deprecated - use dedicated scripts instead
            this.message.setProperty("text", "")
        } finally {
            checkForPayloadLogging(tracePoint, projectName, integrationID)
        }
    }

    def String conclude() {
        return conclude(false)
    }

    def String conclude(boolean attachToMpl, boolean includeSystemInfo = false) {
        def properties   = this.message.getProperties()
        def projectName  = properties.get(Constants.ILCD.VM_SRC_ID)
        def integrationID = properties.get(Constants.ILCD.VM_TRGT_ID)
        def messageLog = properties.get(Constants.ILCD.LOG_STACK_PROPERTY)
        def messageLogEntries = []
        if (messageLog instanceof String) {
            messageLogEntries = messageLog.split(',')
        } else if (messageLog instanceof List) {
            messageLogEntries = messageLog
        }
        def headerData = prepareHeaderData(projectName, integrationID, includeSystemInfo)
        def jsonData = headerData + [messages: messageLogEntries]
        def cleanedJson = new JsonBuilder(removeEmptyFields(jsonData)).toPrettyString()
        def maskedMessage = mask(cleanedJson, projectName, integrationID)

        if (attachToMpl && isAttachable("FULL_LOG", "#${this.logCounter} FULL_LOG")) {
            attachBodyWithLabel(maskedMessage, "#${this.logCounter} FULL_LOG", "", true)
        }
        return maskedMessage
    }

    /**
     * Prepare header data for logs by combining standard and custom metadata
     */
    def prepareHeaderData(String projectName, String integrationID, boolean includeSystemInfo = false) {
        try {
            Framework_ValueMaps.validateMetadataIdentifiers(projectName, integrationID)
            def standardMetadata = buildStandardMetadata(projectName, integrationID)
            def customMetadata = buildCustomMetadata(message.getProperties())
            // Send to Custom Header Properties
            try {
                addStandardMetadataProperties(standardMetadata)
                addCustomMetadataPrimaryKey(customMetadata)
                addUserDefinedCustomHeaders(customMetadata)
            } catch (Exception e) {
                handleScriptError(message, messageLog, e, "Framework_Logger.prepareHeaderData", true)
            }
            // Combine
            def combinedHeader = standardMetadata + customMetadata
            if (includeSystemInfo && this.systemInfo && this.systemInfo.size() > 0) {
                combinedHeader = combinedHeader + [systemInfo: this.systemInfo]
            }
            // Remove empty or null nalues and teturn
            return removeEmptyFields(combinedHeader)
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.prepareHeaderData", true)
        }
    }

    /**
     * Builds the standard metadata map.
     */
    private Map<String, Object> buildStandardMetadata(String projectName, String integrationID) {
        def mpl_logLevel = message.getProperty(Constants.Property.MPL_LEVEL_INTERNAL)
        def tenantName   = this.systemInfo?.tenantName ?: System.getenv("TENANT_NAME") ?: this.settings.environment
        def isRetryAttempt = toBool(message.getProperty(Constants.Property.SAP_IS_REDELIVERY_ENABLED))

        return updateMetadataAndStandardHeaders([
            projectName                : interfaceVM("meta_projectName", projectName, integrationID, projectName),
            systemName                 : interfaceVM("meta_systemName", projectName, integrationID, tenantName),
            messageID                  : message.getProperty(Constants.Property.SAP_MPL_ID),
            correlationID              : message.getHeaders().get(Constants.Header.SAP_CORRELATION_ID),
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
            logLevel                   : Constants.ILCD.normalizeLogLevel(mpl_logLevel ?: this.overallLogLevel, this.settings.defaultOverallLogLevel),
            priority                   : interfaceVM("meta_priority", projectName, integrationID),
            Retry                      : isRetryAttempt
        ])
    }

    /**
     * Adds standard metadata fields to custom header properties, each truncated to the configured char limit.
     * @param log             MessageLog object
     * @param standardMetadata Map of standard metadata fields
     */
    private void addStandardMetadataProperties(Map standardMetadata) {
        if (!standardMetadata) return
        Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS.each { fieldName ->
            writeMetaCustomHeaderProperty("${fieldName}", "${standardMetadata[fieldName]}")
        }
    }

    /**
     * Adds custom header 'meta_businessId' using likely values for it, starting with property 'meta_attribute_businessId'.
     * 
     * @param log           MessageLog object
     * @param customMetadata Map of custom metadata fields
     */
    private void addCustomMetadataPrimaryKey(Map customMetadata) {
        if (!customMetadata) return
        def altKey = message.headers[Constants.Header.SAP_MESSAGE_TYPE] 
            ?: message.properties[Constants.ILCD.Batch.JOB_ID]
            ?: message.properties["businessId"] ?: message.properties["businessID"]
        def pkVal = customMetadata.businessId ?: customMetadata.businessID ?: altKey
        writeMetaCustomHeaderProperty("businessId", "${pkVal}")
    }

    /**
     * Adds meta_attributes plus any additional specified properties as custom header properties. 
     * Properties found on 'customHeaderPropertyKeys' (comma-sep) will be looked up at runtime.
     *  
     * @param log             MessageLog object
     * @param customMetadata  Map of custom metadata fields
     * @param customKeysProp  Comma-separated string of user-specified keys
     */
    private void addUserDefinedCustomHeaders(Map customMetadata) {
        if (!customMetadata) return
        def customKeysProp = properties["customHeaderPropertyKeys"] as String
        def userKeys = (customKeysProp ?: "").split(",").findAll { message.properties[it] != null } 
        def userProps = (userKeys ?: []).collectEntries { [(it): message.properties[it]] }
        def finalProps = userProps + customMetadata
        int maxKeys = ("${settings.customHeaderExtraKeys}").toInteger() ?: 10
        
        finalProps.remove("businessId")                         // already added businessId, exclude it 
        finalProps.keySet()                                     // use keys for uniqueness check
            .toUnique()                                         // and don't repeat standard metadata
            .minus(Constants.ILCD.META_FIELDS_TO_CUSTOM_HEADERS)
            .take(maxKeys)               
            .each { key -> 
                writeMetaCustomHeaderProperty("${key}", "${finalProps[key]}") 
            }
    }

    /**
     * Updates standard headers with metadata and sets status for errors
     */
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
            this.message.setProperty(Constants.Property.MPL_CUSTOM_STATUS, Constants.ILCD.CUST_STATUS_FAILED)
        }
        return metadata
    }

    /**
     * SAP limits custom header props to 200 chars, but we check for any lower limit set in global VM.
     */
    private void writeMetaCustomHeaderProperty(String key, Object value) {
        MessageLog log = getOrCreateMessageLog()
        if (!log || !key || value == null) return
        def stringValue = value.toString()
        if (stringValue == null || stringValue.trim().isEmpty() || stringValue.trim().toLowerCase() == "null") return
        def metaKey = "${Constants.ILCD.META_CH_PREFIX}${key}"
        def metaValue = prepareString(stringValue, settings.customHeaderCharLimit) 
        log.addCustomHeaderProperty("${metaKey}", "${metaValue}")
    }

    /**
     *  Unsets any null or empty strings from the resulting json logs map
     */
    def removeEmptyFields(def object) {
        if (object instanceof Map) {
            return object
                .findAll { it.value != null && !(it.value instanceof String && it.value.trim().isEmpty()) }
                .collectEntries { [it.key, removeEmptyFields(it.value)] }
        }
        return object instanceof List ? object.collect { removeEmptyFields(it) }.findAll { it != null } : object
    }

    /**
     * Builds custom metadata from message properties.
     * @param properties Map of message properties
     * @return Map of custom metadata fields
     */
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

    /**
     * Extract custom log attributes from properties with log_ prefix
     */
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

    /**
     * Masks sensitive fields in a JSON string.
     */
    def mask(String jsonString, String projectName, String integrationID) {
        return Framework_Utils.maskFields(jsonString, projectName, integrationID, "loggerSensitiveFields", message, messageLog)
    }

    private static String getTimestamp(String format = "yyyy-MM-dd HH:mm:ss.SSS") {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format))
    }

    private String interfaceVM(String key, String projectName, String integrationID, String defaultValue = null) {
        Framework_ValueMaps.interfaceVM(key, projectName, integrationID, defaultValue, message, messageLog)
    }

    private String frameworkVM(String key, String defaultValue = null) {
        Framework_ValueMaps.frameworkVM(key, defaultValue, message, messageLog)
    }

    private static String getStackTrace(Exception e) {
        return Framework_ExceptionHandler.getStackTrace(e)
    }

    private String prepareString(String value, String logCharacterLimit) {
        return escapeQuotes(truncateString(value, logCharacterLimit))
    }

    private String truncateString(String value, String logCharacterLimit) {
        def limit = (logCharacterLimit != null) ? Integer.parseInt(logCharacterLimit) : 1000
        return (value?.length() > limit) ? value.substring(0, limit) : value
    }

    private String escapeQuotes(String value) {
        return value?.replaceAll("\"", "\\\\\"")
    }

    /**
     * @param overallLogLevel resolves to external first and internal (MPL) afterwards
     * @param logLevel to compare against the overall one
     * @return boolean
     */
    def isLoggable(String overallLogLevel, String logLevel) {
        def configuredLogLevel = Constants.ILCD.normalizeLogLevel(overallLogLevel)
        def itemLogLevel       = Constants.ILCD.normalizeLogLevel(logLevel, this.settings.defaultLogLevel)
        return Constants.ILCD.LOG_LEVEL_HIERARCHY[itemLogLevel] >= Constants.ILCD.LOG_LEVEL_HIERARCHY[configuredLogLevel]
    }

    /**
     * Check if a log entry is attachable with configured limits, taking into
     * consideration any current loops or remaining retry attempts. 
     */
    def boolean isAttachable(String tracePoint, String label) {
        def GLOBAL_HARD_LIMIT = 8 // Secret hard limit for all scripts
        def ATTACH_SOFT_LIMIT = settings?.attachmentLimit?.isInteger() ? settings.attachmentLimit.toInteger() : 5
        def ATTACH_TRACE_LIMIT = settings?.traceAttachmentLimit?.isInteger() ? settings.traceAttachmentLimit.toInteger() : 3
        def props = this.message.getProperties()
        def headers = this.message.getHeaders()

        def splitComplete = toBool(props.get(Constants.Property.CAMEL_SPLIT_COMPLETE)) // true only on last split
        def loopIndex = props.get(Constants.Property.CAMEL_LOOP_INDEX)
        def splitIndex = props.get(Constants.Property.CAMEL_SPLIT_INDEX)
        def splitSize = props.get(Constants.Property.CAMEL_SPLIT_SIZE)
        def currentIndex = (loopIndex ?: splitIndex)?.toString()?.isInteger() ? (loopIndex ?: splitIndex).toInteger() : 0
        def totalSplits = splitSize?.toString()?.isInteger() ? splitSize.toInteger() : GLOBAL_HARD_LIMIT
        def isLoopScope = !splitComplete && totalSplits > 1
        def normalizedCount = this.logCounter ?: currentIndex
        def isActiveLoopOverLimit = isLoopScope && normalizedCount >= totalSplits

        def payloadLogPoints = ["MESSAGE"].any { label?.toUpperCase()?.contains(it) }
        def endTypes = ["END", "SUMMARY"].any { tracePoint?.toUpperCase()?.contains(it) || label?.toUpperCase()?.contains(it) }
        def traceTypes = ["TRACE", "DEBUG"].any { tracePoint?.toUpperCase()?.contains(it) || label?.toUpperCase()?.contains(it) }
        def traceDebugLevel = ["TRACE", "DEBUG"].contains(this.overallLogLevel?.toUpperCase())
        def isRetry = toBool(props.get(Constants.Property.SAP_IS_REDELIVERY_ENABLED)) || (headers.get(Constants.Header.SAP_DATASTORE_RETRIES)?.toInteger() ?: 0) > 0
        def isError = (tracePoint?.toUpperCase() == "ERROR") || (this.logLevel?.toUpperCase() == "ERROR") || 
            (props.get(Constants.Property.CAMEL_EXC_CAUGHT) != null) || toBool(props.get(Constants.ILCD.PROP_ILCD_EXC_IN_PROGRESS))

        // DEBUG: Print all relevant state for troubleshooting
        if(tracePoint?.toUpperCase() == "END" && label?.toUpperCase()?.contains("MESSAGE")) {
            def debugMsg = "isAttachable debug: tracePoint=${tracePoint}, label=${label}, payloadLogPoints=${payloadLogPoints}, isLoopScope=${isLoopScope}, normalizedCount=${normalizedCount}, totalSplits=${totalSplits}, isActiveLoopOverLimit=${isActiveLoopOverLimit}, attachmentsDisabled=${toBool(settings.attachmentsDisabled)}, hardLimit=${normalizedCount >= GLOBAL_HARD_LIMIT}, labelFullLog=${label?.contains('FULL_LOG')}, isError=${isError}, endTypes=${endTypes}, isRetry=${isRetry}, splitComplete=${splitComplete}"
            debug("isAttachable_debug_log", debugMsg, this.messageLog)
        }

        // Block if attachments are globally disabled, or hard limit reached, or label is a full log
        if (toBool(settings.attachmentsDisabled) || normalizedCount >= GLOBAL_HARD_LIMIT || label?.toUpperCase()?.contains("FULL_LOG")) return false
        // Always allow attachment for errors or configured payload points, except if we're in a loop and over hard limit
        if ((isError || payloadLogPoints)) {
            // Allow if not over limit, or if this is the final split iteration (splitComplete==true and normalizedCount==totalSplits)
            if (!isActiveLoopOverLimit || (splitComplete && normalizedCount == totalSplits)) return true
            return false
        }
        // End logs
        if (endTypes && !isActiveLoopOverLimit && !isRetry) return normalizedCount < (ATTACH_SOFT_LIMIT + 1)
        // All other non-trace logs
        if (!traceTypes) return normalizedCount < ATTACH_SOFT_LIMIT
        // Trace/debug logs are ok when not inside loops/splits (let's first iteration attach)
        if (traceTypes && traceDebugLevel && currentIndex > 0) return normalizedCount < ATTACH_TRACE_LIMIT
        return false
    }

    /**
     * Smart counter increment: only on actual attachment
     */
    private int getLogCounter() {
        try {
            if (!this.logCounter) {
                this.logCounter = (this.message.getProperty(Constants.ILCD.LOG_ATTACH_COUNTER) ?: 0).toString().toInteger() + 1
            }
            return this.logCounter
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.setLogCounterOnAttach")
        }
    }

    /**
     * Smart counter increment: only on actual attachment
     */
    private void setLogCounterOnAttach() {
        try {
            this.message.setProperty(Constants.ILCD.LOG_ATTACH_COUNTER, "${this.logCounter ?: getLogCounter()}")
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.setLogCounterOnAttach")
        }
    }

    def attachBodyWithLabel(String body, String label, String fallbackSimpleLogMsg = "", boolean incrementCounter = true) {
        try {
            def log = getOrCreateMessageLog()
            if (log != null) {
                if (isAttachable(this.tracePoint, label)) {
                    if (incrementCounter) setLogCounterOnAttach() // increment only when attaching and allowed
                    createAttachment(label, body, "text/plain", log)
                } else if (fallbackSimpleLogMsg) {
                    log.setStringProperty(label, fallbackSimpleLogMsg)
                }
            }
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.attachBodyWithLabel")
        }
    }
    // Also update createAttachment static method
    public static void createAttachment(String label, String content, String contentType, MessageLog messageLog) {
        if (messageLog) {
            def title = label ?: "Log ${getTimestamp()}"
            // Only increment counter if not skipped
            messageLog.addAttachmentAsString("${title}", "${content ?: ''}", "${contentType ?: 'text/plain'}")
        }
    }

    /**
     * Returns the MessageLog object for the current integration flow.
     * Tries to resolve from binding first (as in CPI Groovy context), then from message property.
     */
    private MessageLog getOrCreateMessageLog() {
        if (this.messageLog == null) {
            // Try binding first (preferred in CPI)
            def binding = this.binding ?: null
            def logFactory = null
            if (binding && binding.hasVariable('messageLogFactory')) {
                logFactory = binding.getVariable('messageLogFactory')
            }
            // Fallback: try from message property
            if (!logFactory) {
                logFactory = this.message.getProperty("messageLogFactory")
            }
            if (logFactory) {
                this.messageLog = logFactory.getMessageLog(this.message)
            } else {
                throw new IllegalStateException("MessageLogFactory is not available in binding or message properties.")
            }
        }
        return this.messageLog
    }

    /**
     * The default fallback handler in case of exceptions thrown within ILCD Framework code. 
     * Can be queried in the Message Processing Logs via custom headers: "ILCD_EXC=true"
     * 
     * The intent is to log the internal exception while allowing for execution 
     * to continue. This does have a downside in that we get a limited stacktrace.
     */
    public static void handleScriptError(
        Message message, 
        MessageLog messageLog, 
        Exception e, 
        String function, 
        boolean printStackTrace = false, 
        String customData = ""
    ) {
        final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("Framework_Logger")
        // --- Unwrap and re-throw custom soft error exceptions if found ---
        def softErrorEx = src.main.resources.script.Framework_ExceptionHandler.findCauseByClass(e, [
            Constants.SoftError.EXC_CLASS
        ])
        debug("handleScriptError.softErrorEx", "${softErrorEx} | ${e.getClass()}", messageLog)
        if (softErrorEx != null) {
            LOG.debug("handleScriptError caught a custom soft error exception. Re-throwing ${softErrorEx.class} - ${softErrorEx.message}")
            throw softErrorEx
        }
        if (!messageLog) {
            LOG.warn("handleScriptError called with a null MPL reference. Unable to log the exception.")
            return
        }
        if (message.getProperty(Constants.ILCD.PROP_ILCD_EXC_IN_PROGRESS) == true) {
            LOG.error("handleScriptError called recursively. Skipping second attempt to avoid infinite loop.", e)
            return
        }
        message.setProperty(Constants.ILCD.PROP_ILCD_EXC_IN_PROGRESS, true)
        
        def logLevel = message.getProperty(Constants.Property.MPL_LEVEL_OVERALL)
        def serverTraceLogsOrEmpty = logLevel == "TRACE" ? ["Server Trace Log Tail": Framework_ExceptionHandler.getServerTraceTail(100)] : [] 
        def errorLabel = "${Constants.ILCD.EXC_PREFIX}-${function}"
        def errorMessage = new StringBuilder()
        def errorMap = [
            "Script/Function": function,
            "Exception Message": e.message,
            "Cause": e.cause ?: "Unknown",
            "Details": customData ?: "N/A",
            "Stack Trace": !printStackTrace ? "N/A" : ("\n" + Framework_ExceptionHandler.getStackTrace(e))
        ] + serverTraceLogsOrEmpty

        // Include Exception Info, Properties, Headers
        appendProperties(errorMessage, "EXCEPTION SUMMARY", errorMap)
        appendProperties(errorMessage, "PROPERTIES", message.properties)
        appendProperties(errorMessage, "HEADERS", message.headers)

        try {
            createAttachment(errorLabel, errorMessage.toString(), "text/plain", messageLog)
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}", "true")
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.addCustomHeaderProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.class.canonicalName)
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ScriptName", function)
            messageLog.setStringProperty("${Constants.ILCD.EXC_PREFIX}-ErrorClass", e.class.canonicalName)
        } catch (Exception innerEx) {
            LOG.error("handleScriptError: Could not attach error; skipping to avoid recursion.", innerEx)
        } finally {
            // message.setProperty(Constants.ILCD.PROP_ILCD_EXC_IN_PROGRESS, false)
            LOG.error("${errorLabel} occurred: ${e.message}", e)
        }
    }

    static String truncate(String input, int maxLength) {
        return ((input?.size() ?: 0) > maxLength) ? input.substring(0, maxLength) + "... (truncated)" : input
    }

    static void appendProperties(StringBuilder sb, String title, Map map) {
        sb.append(formatTitle(title))
        map.each { key, value ->
            try {
                String truncatedValue = truncate(value?.toString(), (key != "Stack Trace" ? 100 : 3000))
                sb.append(String.format(" %-40s: %-100s\n", key, truncatedValue))
            } catch (Exception ignored) {
                sb.append("Exc for ${key} - ${ignored.message}\n")
            }
        }
        sb.append("\n")
    }

    static String formatTitle(String input) {
        String border = "—" * input.length()
        return "${border}\n${input}\n${border}\n" // Titles are underlined with a border
    }

    // Utility: robust toBool for test and prod
    static boolean toBool(def v) {
        if (v == null) return false
        if (v instanceof Boolean) return v
        if (v instanceof String) return v.equalsIgnoreCase('true') || v == '1'
        if (v instanceof Number) return v != 0
        return false
    }

    private void handleValidationError(Exception e, String function, boolean critical = false) {
        handleScriptError(this.message, this.messageLog, e, function, true)
        if (critical) throw e
    }

    /**
     * Check for payload logging settings and attach payload if configured
     */
    private void checkForPayloadLogging(String tracePoint, String projectName, String integrationID) {
        def normalizedTracePoint = tracePoint?.trim()?.toUpperCase()
        def allowedPayloadLogPoints = []
        def interfaceLogPoints = []
        try {
            int maxLines = this.settings.maxPayloadLines.toString().isInteger() ? this.settings.maxPayloadLines.toString().toInteger() : 1000
            allowedPayloadLogPoints = this.settings.allowedPayloadLogPoints
                .split(",")
                .collect { it.trim().toUpperCase() }.findAll { it }
            interfaceLogPoints = interfaceVM("loggerPayloadLogPoints", projectName, integrationID, "")
                .split(",")
                .collect { it.trim().toUpperCase() }.findAll { it }
        } catch (Exception e) {
            this.messageLog.setStringProperty("PAYLOAD_LOG_NOT_ENABLED", "Failed to attach full payload. ${e.message}")
        }
        def result = allowedPayloadLogPoints.contains(normalizedTracePoint) && interfaceLogPoints.contains(normalizedTracePoint)
        debug("checkForPayloadLogging_debug", "result - ${result}, interfaceLogPoints - ${interfaceLogPoints}, allowedPayloadLogPoints - ${allowedPayloadLogPoints},   normalizedTracePoint - ${normalizedTracePoint}", this.messageLog)
        if (!normalizedTracePoint || !allowedPayloadLogPoints || !interfaceLogPoints) return
        
        if (allowedPayloadLogPoints.contains(normalizedTracePoint) && interfaceLogPoints.contains(normalizedTracePoint)) {
            try {
                def reader = message.getBody(java.io.InputStream)
                if (reader == null) return

                def payload = new StringBuilder()
                int linesRead = 0
                reader.eachLine { String line ->
                    if (linesRead++ < maxLines) {
                        payload.append(line).append(System.lineSeparator())
                    } else {
                        payload.append("...truncated...").append(System.lineSeparator())
                        return false
                    }
                }
                attachBodyWithLabel(payload.toString(), "MESSAGE_${tracePoint}", "Attempted to log payload...", false)
            } catch (Exception e) {
                try { // try one more time
                    attachBodyWithLabel(this.message.getBody(String.class), "MESSAGE_${tracePoint}",  "Attempted to log payload again...", false)
                } catch (Exception ignored) {
                    this.messageLog.setStringProperty("PAYLOAD_LOG_FAILED", "Failed to attach full payload. ${e.message}")
                }
            }
        }
    }

    /**
     * Get system details for logging purposes
     */
    private Map getSystemDetails() {
        try {
            def ctx = this.message?.exchange?.context
            return [
                javaVersion: System.properties['java.version'],
                groovyVersion: GroovySystem.version,
                tenantName: System.getenv("TENANT_NAME") ?: System.properties['com.sap.it.node.tenant.name'], 
                tenantID: System.getenv("TENANT_NAME") ?: System.properties['com.sap.it.node.tenant.id'],
                cfInstanceID: System.getenv("CF_INSTANCE_INDEX"),
                camelVersion: ctx?.version,
                camelUptime: ctx?.uptime
            ]
        } catch (Exception e) {
            handleScriptError(message, messageLog, e, "Framework_Logger.getSystemDetails", false)
            return [:]
        }
    }

    static void debug(key, value, log) {
        if (!log || !key || !value) return
        log.setStringProperty("${key}", "${value}")
    }
}
