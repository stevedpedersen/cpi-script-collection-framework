package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import org.apache.commons.io.input.BOMInputStream
import groovy.xml.*

import src.main.resources.script.Framework_ValueMaps
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants

class Framework_Validator {
    Message message
    MessageLog messageLog
    Map<String, List<String>> failedRecords  // Track validation errors per record
    String projectName
    String integrationID
    Framework_Logger logger

    static final String ID_UNKNOWN = "unknown"

    Framework_Validator(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.failedRecords = [:]  // Initialize empty map for failed records
        this.logger = new Framework_Logger(message, messageLog)
    }

    def void assertPayloadValid(boolean raiseException = false) {
        def validationErrorMessage = (message.getProperty(Constants.ILCD.Validator.ERR_MSG_PROP) ?: "")?.trim()
        if (!validationErrorMessage) {
            logValidationResults(raiseException, false)
        } else if (validationErrorMessage && raiseException) {
            throw new SchemaValidationException(validationErrorMessage)
        }
        if (messageLog) {
            messageLog.setStringProperty("VALIDATION_SUCCESS", Constants.ILCD.Validator.MM_ERROR_PREFIX + " validation success")
        }
    }

    def void logValidationResults(boolean raiseException = false, boolean shouldLog = true) {
        def validationErrorMessage
        try {
            def xmlInput = message.getBody(java.io.InputStream)
            def bomStream = new BOMInputStream(xmlInput)
            // Force skip the BOM if one is present
            bomStream.getBOM()
            validationErrorMessage = getValidationErrors(bomStream)

            // Log the result
            if (validationErrorMessage) {
                def records = validationErrorMessage.split(/\[\w*?\]:/)
                def recordErrors = records.collect{ it.split(Constants.ILCD.Validator.MM_ERROR_PREFIX) }

                message.setProperty(Constants.ILCD.Validator.ERR_MSG_PROP, validationErrorMessage)
                if (shouldLog) {
                    this.logger.logMessage("VALIDATION_ERROR", "WARN",
                        Constants.ILCD.Validator.LOG_WARN_MSG + validationErrorMessage.substring(0, 20) + "...")
                }
            } else {
                if (shouldLog) {
                    this.logger.logMessage("VALIDATION_SUCCESS", "TRACE",
                        message.properties.text ?: Constants.ILCD.Validator.LOG_INFO_MSG)
                }
            }
        } catch (Exception e) {
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_Validator.logValidationResults", true)
        }

        if (validationErrorMessage && raiseException) {
            throw new SchemaValidationException(validationErrorMessage)
        }
    }

    def String getValidationErrors(def xmlInput) {
        def (rootNode, pkNode, headerNodes, splitterNode) = fetchXmlFieldsFromValueMap()

        try {
            def xml = safeParseXmlInput(xmlInput)

            // Handle looping through nodes in a generalized way, but avoid redundant depthFirst searches
            def nodesToProcess = splitterNode ? 
                xml.depthFirst().findAll { it.name().equals(splitterNode) } : 
                xml.depthFirst().findAll { it.children().size() > 0 }

            nodesToProcess.eachWithIndex { recordNode, i ->
                def recordId = pkNode ? getNodeOrAttributeValue(recordNode, pkNode) : "${ID_UNKNOWN}-${i}"
                def recordFailures = new LinkedHashSet()

                // Check for validation errors for this record
                recordNode.depthFirst().findAll { node -> node.text()?.startsWith(Constants.ILCD.Validator.MM_ERROR_PREFIX) }.each { node ->
                    if (node.children().isEmpty()) {
                        def fieldValue = node.text()
                        recordFailures.add(fieldValue)
                    }
                }

                // If the record has validation errors, track them
                if (recordFailures.size() > 0) {
                    def merged = new LinkedHashSet(recordFailures)
                    if (this.failedRecords.containsKey(recordId)) {
                        merged.addAll(this.failedRecords[recordId])
                    }
                    this.failedRecords[recordId] = merged as List
                }
            }
        } catch (SchemaValidationException ve) {
            // Re-throw intentional validation errors
            throw ve
        } catch (Exception e) {
            // Handle unexpected errors
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_Validator.getValidationErrors", true)
            throw e
        }

        // Construct and return the validation error report
        if (!this.failedRecords.isEmpty()) {
            return constructValidationErrorReport()
        }
        return null
    }

    def safeParseXmlInput(def input) {
        def xml
        try {
            // Primary parse with XmlSlurper
            def slurper = new XmlSlurper()
            slurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

            if (input instanceof InputStream) {
                xml = slurper.parse(input)
            } else {
                // String fallback
                xml = slurper.parseText(input.toString())
            }
        } catch (Exception slurperError) {
            // fallback with XmlParser: read the entire input as string, do any cleaning, parse
            String fallbackText = input instanceof InputStream ? new String(input.bytes) : input.toString()
            // optional trimming or BOM removal again
            fallbackText = fallbackText.replaceFirst(/^\uFEFF/, '').trim()
            fallbackText = fallbackText.replaceFirst('^([\\W]+)<', '<')

            def parser = new XmlParser()
            xml = parser.parseText(fallbackText)
        }
        return xml
    }

    // Extract a value from node or attribute text
    def String getNodeOrAttributeValue(def node, String nodeName) {
        def (tag, attr) = nodeName.contains("@") ? nodeName.split("@") : [nodeName, null]

        // Direct check if the current node matches
        if (node?.children()?.isEmpty() || node?.name().equals(tag)) {
            return (attr ? node["@" + attr] : node?.text()) as String
        }

        // Traverse children to find the matching tag and attribute
        def result = null
        node.depthFirst().find { child ->
            child.name().equals(tag) && (attr ? child["@" + attr] != null : true)
        }?.with { match ->
            result = attr ? match["@" + attr] : match?.text()
        }

        return result
    }

    // Users can provide expected XML node names/attributes, allowing for a more advanced parsing result
    def List<String> fetchXmlFieldsFromValueMap() {
        def fields = [
            Constants.ILCD.Validator.VM_KEYS_XML_ROOT, 
            Constants.ILCD.Validator.VM_KEYS_XML_PK, 
            Constants.ILCD.Validator.VM_KEYS_XML_HEADERS, 
            Constants.ILCD.Validator.VM_KEYS_XML_SPLIT
        ]
        initValueMapIdentifiers()

        return fields.collect { fieldKey ->
            try {
                Framework_ValueMaps.interfaceVM(
                    fieldKey, this.projectName, this.integrationID, this.message, this.messageLog)
            } catch (Exception ignored) {
                null
            }
        }
    }

    void initValueMapIdentifiers() {
        this.projectName = this.projectName ? this.projectName : this.message.getProperty(Constants.ILCD.VM_SRC_ID)
        this.integrationID = this.integrationID ? this.integrationID : this.message.getProperty(Constants.ILCD.VM_TRGT_ID)
    }

    // Attempts to parse multi-record payload first and falls back to single-record report
    def String constructValidationErrorReport() {
        try {
            // For each unique error message, keep the first record index where it appears
            def errorToIndex = [:]
            this.failedRecords.each { key, value ->
                value.each { v ->
                    if (v && !errorToIndex.containsKey(v)) {
                        errorToIndex[v] = key
                    }
                }
            }
            return errorToIndex.collect { v, k -> "[${k.startsWith(ID_UNKNOWN) ? k.substring(ID_UNKNOWN.size() + 1) : k}]: ${v}" }.join('\n')
        } catch (Exception e) {
            return ""
        }
    }

    def handleValidationError(String errorMessage) {
        throw new SchemaValidationException(errorMessage)
    }

    def static throwValidationError(String errorMessage) {
        throw new SchemaValidationException(errorMessage)
    }

    // Static inner class for the custom exception
    public static class SchemaValidationException extends RuntimeException {
        String reason
        SchemaValidationException(String message) {
            super(message)
            this.reason = Constants.ILCD.Validator.EXC_CAUSE
        }
        SchemaValidationException(String message, String reason) {
            super(message)
            this.reason = reason
        }
        Throwable getCause() {
            return new Throwable(Constants.ILCD.Validator.EXC_CAUSE)
        }
        String getReason() { return this.reason }
    }
}
