package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants
import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi
import groovy.xml.*

class Framework_Validator {
    Message message
    MessageLog messageLog
    Map<String, List<String>> failedRecords  // Track validation errors per record
    String projectName
    String integrationID


    static final String ID_UNKNOWN = "unknown"

    Framework_Validator(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        this.failedRecords = [:]  // Initialize empty map for failed records
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
                def recordFailures = []

                // Check for validation errors for this record
                recordNode.depthFirst().findAll { node -> node.text()?.startsWith(Constants.ILCD.Validator.MM_ERROR_PREFIX) }.each { node ->
                    if (node.children().isEmpty()) {
                        def fieldValue = node.text()
                        recordFailures.add(fieldValue)
                    }
                }

                // If the record has validation errors, track them
                if (recordFailures.size() > 0) {
                    if (this.failedRecords.containsKey(recordId)) {
                        this.failedRecords[recordId].addAll(recordFailures)
                    } else {
                        this.failedRecords[recordId] = recordFailures
                    }
                }
            }
        } catch (SchemaValidationException ve) {
            // Re-throw intentional validation errors
            throw ve

        } catch (Exception e) {
            // Handle unexpected errors with Framework_Logger
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
                // input is presumably a String fallback
                xml = slurper.parseText(input.toString())
            }

        } catch (Exception slurperError) {
            // fallback with XmlParser: read the entire input as string, do any cleaning, parse
            String fallbackText
            if (input instanceof InputStream) {
                // we already tried reading it once, so we have to re-wind or re-get it
                // typically you can re-get the body or store the bytes from earlier
                fallbackText = input.bytes?.decodeToString()
            } else {
                // if it's already a String, just do it
                fallbackText = input.toString()
            }

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

    // Fetch XML fields from the value map
    def List<String> fetchXmlFieldsFromValueMap() {
        def vm = ITApiFactory.getApi(ValueMappingApi.class, null)
        def fields = [
            Constants.ILCD.Validator.VM_KEYS_XML_ROOT, 
            Constants.ILCD.Validator.VM_KEYS_XML_PK, 
            Constants.ILCD.Validator.VM_KEYS_XML_HEADERS, 
            Constants.ILCD.Validator.VM_KEYS_XML_SPLIT
        ]
        initValueMapIdentifiers()

        return fields.collect { fieldKey ->
            try {
                vm.getMappedValue("Input", this.projectName, fieldKey, "Output", this.integrationID)
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
            this.failedRecords
                .collect { key, value -> 
                    "[" + (key.startsWith(ID_UNKNOWN) ? key.substring(ID_UNKNOWN.size() + 1) : key) + "]: ${value.join(', ')}" }
                .join('\n')
        } catch (Exception e) {
            this.failedRecords.collect { key, value -> ${value.join(', ')} }.join('\n')
        }
    }

    // Throw a custom validation error if validation fails
    def handleValidationError() {
        if (this.failedRecords) {
            handleValidationError(constructValidationErrorReport())
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

        SchemaValidationException(String message) {
            super(message)
        }

        Throwable getCause() {
            return new Throwable(Constants.ILCD.Validator.EXC_CAUSE)
        }
    }
}
