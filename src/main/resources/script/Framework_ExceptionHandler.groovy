package src.main.resources.script

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
// import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.io.PrintWriter
import java.io.RandomAccessFile
import groovy.json.JsonSlurper
import src.main.resources.script.Constants

class Framework_ExceptionHandler {
    Message message
    MessageLog messageLog
    Map error = [
        className: "",
        statusCode: 500,
        message: "Internal Server Error",
        details: "",
        exceptionMessage: "",
        requestUri: "",
        stackTrace: "",
        headers: null,
        hasRetry: false,
        location: "BTP CI",
        type: "",
    ]

    Framework_ExceptionHandler(Message message, MessageLog messageLog) {
        this.message = message
        this.messageLog = messageLog
        setErrorProperties()
    }

    /**
     * Unified method to set message properties, custom headers, and optionally log.
     * In the iFlow log scripts, call this with `handleError(true)` to log,
     * or `handleError(false)` to just parse & set props.
     */
    def handleError(boolean shouldLog = false, String tracePoint = "ERROR") {
        try {
            // Set message properties
            message.setProperty("log_statusCode", error.statusCode.toString())
            message.setProperty("log_exceptionClass", error.className)
            if (!error.message || error.message != error.exceptionMessage) {
                message.setProperty("log_exceptionMessage", error.exceptionMessage)
            }
            // Add custom headers if we have a messageLog
            if (messageLog) {
                if (error.message != null) { 
                    if (error.message.contains(Constants.ILCD.Validator.MM_ERROR_PREFIX)) {
                        error.message.split("\n").eachWithIndex { msg, i -> messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_MSG, msg.replace("[0]", "[${i}]")) }
                    } else {
                        messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_MSG, "${error.message}")
                    }
                }
                if (error.className != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_EXC_CLASS, "${error.className}") }
                if (error.statusCode != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_STATUS_CODE, "${error.statusCode.toString()}")}
                if (error.type != null) { messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_TYPE, "${error.type}") }
                if (error.exceptionMessage != null && (!error.message || error.message != error.exceptionMessage)) {
                    messageLog.addCustomHeaderProperty(Constants.ILCD.ExceptionHandler.MPL_CH_ERR_EXC_MSG, "${error.exceptionMessage}")
                }
            }
            // If we want to log, do it now
            if (shouldLog) {
                def logger = new Framework_Logger(message, messageLog)
                logger.logMessage(tracePoint, "ERROR", error.message ?: "Unknown error")
            }
        } catch (Exception e) {
            Framework_Logger.handleScriptError(message, messageLog, e, "Framework_ExceptionHandler.handleError", true)
        }
    }

    def Map getErrorDetails() {
        return this.error
    }

    private void setErrorProperties() {
        this.error.hasRetry = toBool(this.message.getProperty(Constants.Property.SAP_IS_REDELIVERY_ENABLED))
        this.error.headers = this.message.getHeaders()
        
        def errorType = Constants.ILCD.ExceptionHandler.resolveErrorTypeProperty(this.message.getProperties())
        def ex = this.message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT)
        def customEx = ex ? findCauseByClass(ex, [Constants.ILCD.Validator.EXC_CLASS, Constants.SoftError.EXC_CLASS]) : null
        this.error.type = errorType ?: (customEx ? Constants.ILCD.ExceptionHandler.ERR_TYPE_FUNC : null)

        if (ex instanceof Throwable) {
            this.error.exceptionMessage = ex.getMessage() ?: "Internal Server Error"
            this.error.className = ex.class?.canonicalName ?: "RuntimeException"
            this.error.message = this.error.exceptionMessage
        } else {
            this.error.exceptionMessage = "Internal Server Error"
            this.error.className = "RuntimeException"
            this.error.message = ex?.toString() ?: "Internal Server Error"
        }

        // Override base info for custom exception types
        if (customEx != null) {
            this.message.setHeader(Constants.Header.CAMEL_RESPONSE_CODE, "400")
            this.error.statusCode = 400
            this.error.className = customEx.getClass().getCanonicalName()
            this.error.type = Constants.ILCD.ExceptionHandler.ERR_TYPE_FUNC
            this.error.message = customEx.getMessage()
            this.messageLog.setStringProperty("exceptionCanonicalName", "${this.error.className}")
            try { // Clean up error message
                def lines = this.error.message.replaceAll(/@.*/, '').trim().split("\\r?\\n")
                if (lines && lines.size() > 0 && lines[0].contains('$')) {
                    lines[0] = lines[0].replaceFirst(/^java\.lang\.Exception: [^:]+: /, "")
                }
                if (this.error.message.contains(Constants.ILCD.Validator.MM_ERROR_PREFIX)) {
                    lines = lines?.toList()?.withIndex()?.collect { msg, i -> msg.replace("[0]", "[${i}]") }
                }
                this.error.message = lines.join("\n")
            } catch(Exception ignored) {}
        }

        // Use adapter-specific handlers to parse remaining error details
        Constants.ILCD.ExceptionHandler.ADAPTER_EXC_CLASSES.each { adapter, exceptions ->
            if (exceptions.containsKey(this.error.className)) {
                def methodName = exceptions.get(this.error.className)
                this."$methodName"(ex)
            }
        }

        // Also resolve the status code, giving preference to anything < 500
        def existingCode = this.error.headers.get(Constants.Header.CAMEL_RESPONSE_CODE) 
            ?: this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)
            ?: this.message.getProperty(Constants.Header.CAMEL_RESPONSE_CODE)
        if (existingCode && (!this.error.statusCode || this.error.statusCode == 500)) {
            this.error.statusCode = existingCode.toString().isInteger() ? existingCode.toInteger() : 500
        }

        // Store in message for further steps
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_RESP_MSG, this.error.message)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_STATUS_CODE, this.error.statusCode)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_EXC_MSG, this.error.exceptionMessage)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_EXC_CLASS, this.error.className)
        this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_ERR_TYPE, this.error.type)
        // this.message.setProperty(Constants.ILCD.ExceptionHandler.PROP_META_ATTR_ERR_TYPE, this.error.type)
    }
    
    private void handleHttpAdapterException(Exception ex) {
        def statusCode = ex.getStatusCode()
        def headerCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
        this.error.statusCode = statusCode ?: headerCode ?: this.error.statusCode
        def body = ex.getResponseBody()
        if (body && body.trim().length() > 0) {
            this.error.message = parseMessageFromBody(body) ?: this.error.message
        }
        this.error.details = ex?.getStatusText() ?: ""
    }

    private void handleODataV2AdapterException(Exception ex) {
        def camelCode = this.message.getHeaders().get(Constants.Header.CAMEL_RESPONSE_CODE)?.toString()
        this.error.statusCode = camelCode ? camelCode : this.error.statusCode
        def body = this.message.getBody(String)
        if (body && body.trim().length() > 0) {
            this.error.message = parseMessageFromBody(body) ?: this.error.message
        }
        this.error.requestUri = ex.getRequestUri()
        this.error.details = ex.getLocalizedMessage() ?: ex.getHttpStatus() ?: ex.getErrorCode()
    }

    def String parseMessageFromBody(String body) {
        def altKey = this.message.getProperty(Constants.ILCD.ExceptionHandler.ERR_KEY_PROP) ?: ""
        def messages = []
        if (body.startsWith('{')) {
            try {
                def json = new JsonSlurper().parseText(body)
                def findMessages
                findMessages = { node ->
                    if (node instanceof Map) {
                        node.each { key, value ->
                            if (value instanceof String &&
                                (key.equalsIgnoreCase(Constants.ILCD.ExceptionHandler.ERR_MSG_KEY) || key.equalsIgnoreCase(altKey))) {
                                if (!messages.contains(value)) {
                                    messages.add(value)
                                }
                            }
                            findMessages(value)
                        }
                    } else if (node instanceof List) {
                        node.each { findMessages(it) }
                    }
                }
                findMessages(json)
            } catch (Exception e) {}
        } else if (body.startsWith('<')) {
            try {
                def safeBody = escapeAmpersands(body)
                def xml = new XmlSlurper().parseText(safeBody)
                xml.depthFirst().findAll { node -> 
                    node.name().equalsIgnoreCase(Constants.ILCD.ExceptionHandler.ERR_MSG_KEY) || node.name().equalsIgnoreCase(altKey)
                }.each { node -> 
                    if (node.children().isEmpty()) {
                        if (!messages.contains(node.text())) {
                            messages.add(node.text())
                        }
                    } else if (node.value?.children()?.isEmpty()) {
                        messages.add(node.value.text())
                    }
                }
            } catch (Exception e) {

            }
        }
        return messages.join(", ")
    }

    /**
     * Recursively walks the cause chain of an exception to find the first cause whose class matches any of the provided canonical or simple class names.
     * @param ex The root exception to start from
     * @param targetClassNames List of class names (canonical or simple) to match
     * @return The first matching cause exception, or null if none found
     */
    static Throwable findCauseByClass(Throwable ex, List<String> targetClassNames) {
        Throwable current = ex
        while (current != null) {
            String canonicalName = current.class?.canonicalName
            String simpleName = current.class?.simpleName
            if (targetClassNames.any { it == canonicalName || it == simpleName }) {
                return current
            }
            current = current.cause
        }
        return null
    }

    // Defensive overload: handle non-Throwable ex
    static Throwable findCauseByClass(Object ex, List<String> targetClassNames) {
        if (ex instanceof Throwable) {
            return findCauseByClass((Throwable) ex, targetClassNames)
        }
        return null
    }

    // Utility: Find a cause in the exception chain matching any class name in the list
    static Throwable findCause(Throwable ex, List<String> classNames) {
        while (ex != null) {
            def exClass = ex.getClass().getCanonicalName()
            if (classNames.any { exClass.contains(it) }) {
                return ex
            }
            ex = ex.getCause()
        }
        return null
    }

    /**
     * Fetches the last N lines from the server trace log at /app/log/ljs_trace.log.
     * @param numLines Number of lines to read from the end of the log (default 100)
     * @return String containing the last N lines, or error message if file not found/readable.
     */
    static String getServerTraceTail(int numLines = 100) {
        String logPath = "/app/log/ljs_trace.log"
        File logFile = new File(logPath)
        if (!logFile.exists() || !logFile.canRead()) {
            return "Error: Log file not found or not readable: ${logPath}"
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(logFile, "r")
            long fileLength = raf.length() - 1
            int linesRead = 0
            StringBuilder sb = new StringBuilder()
            for(long pointer = fileLength; pointer >= 0; pointer--) {
                raf.seek(pointer)
                int readByte = raf.readByte()
                if(readByte == 0xA) { // LF: \n
                    if(pointer < fileLength) {
                        linesRead++
                        if(linesRead > numLines) break
                    }
                }
                sb.append((char) readByte)
            }
            raf.close()
            String tailStr = sb.reverse().toString()
            List<String> tailLines = tailStr.split("\n")
            if (tailLines.size() > numLines) {
                tailLines = tailLines[-numLines..-1]
            }
            return tailLines.join("\n")
        } catch(Exception e) {
            return "Error reading log file: ${e.getMessage()}"
        }
    }

    def static String getStackTrace(Exception e) {
        try {
            StringWriter sw = new StringWriter()
            PrintWriter pw = new PrintWriter(sw)
            e.printStackTrace(pw)
            return sw.toString()
        } catch (Exception innerEx) {
            // LoggerFactory.getLogger("Framework_ExceptionHandler").error("Error generating stack trace: ${innerEx.message}", innerEx)
            return "Error generating stack trace: ${innerEx.message}"
        }
    }

    boolean toBool(Object rawValue) {
        if (rawValue == null) return false 
        if (rawValue instanceof Boolean) return (Boolean) rawValue
        return Boolean.parseBoolean(rawValue.toString())
    }

    public static Exception getInternalExceptionType(Message message) {
        return findCauseByClass(message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT), [
            Constants.ILCD.Validator.EXC_CLASS,
            Constants.SoftError.EXC_CLASS,
            Constants.ILCD.ValueMaps.EXC_CLASS
        ])
    }

    public static String escapeAmpersands(String xml) {
        xml.replaceAll(/&(?!amp;|lt;|gt;|apos;|quot;|#\d+;|#x[a-fA-F0-9]+;)/, '&amp;')
    }

    /**
     * Checks for soft error conditions in the message body (empty, empty XML root, XML error/status nodes, etc).
     * Optionally, can check for embedded XML inside a specified wrapper node (e.g. <processActionReturn>), if provided.
     * Accepts a list of predicates for soft error detection (each predicate gets the parsed XML root node).
     * Returns a map: [softError: true/false, reason: code, message: details]
     * @param message         The CPI Message object
     * @param wrapperNodeName (Optional) The node name to check for embedded XML (default: null)
     * @param predicates      (Optional) List of [reason: String, predicate: Closure] maps. Each predicate returns [matched: true/false, message: String] or false/null if not matched.
     */
    public static Map checkForSoftError(
        Message message,
        String wrapperNodeName = null,
        List<Map> predicates = null
    ) {
        if (message.getProperty(Constants.SoftError.PROP_IS_SOFT_ERROR) == true) {
            // Already handled, skip further checks
            return [softError: false, isPropagated: true]
        }
        def customEx = getInternalExceptionType(message)
        if (customEx != null) {
            message.setProperty(Constants.SoftError.PROP_IS_SOFT_ERROR, true)
            message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_REASON, Constants.SoftError.EMPTY_BODY)
            message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_MESSAGE, "Body is empty")
            return [softError: true, reason: customEx?.reason, message: customEx.message, isPropagated: false]
        }
        def ex = message.getProperty(Constants.Property.CAMEL_EXC_CAUGHT)
        if (ex == null) {
            def body = message.getBody(String)
            if (!body || body.trim().isEmpty()) {
                message.setProperty(Constants.SoftError.PROP_IS_SOFT_ERROR, true)
                message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_REASON, Constants.SoftError.EMPTY_BODY)
                message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_MESSAGE, "Body is empty")
                return [softError: true, reason: Constants.SoftError.EMPTY_BODY, message: "Body is empty", isPropagated: true]
            }
            if (body.startsWith('<')) {
                try {
                    def parser = new XmlSlurper(false, false)
                    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                    def safeBody = escapeAmpersands(body)
                    def parsed = parser.parseText(safeBody)
                    if (wrapperNodeName) {
                        def encodedXml = parsed.'**'.find { it.name() == wrapperNodeName }?.text()
                        if (encodedXml && encodedXml.trim()) {
                            def decodedXml = encodedXml.replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('&amp;', '&')
                            parsed = parser.parseText(decodedXml)
                        }
                    }
                    predicates = predicates ?: getDefaultXmlPredicates()
                    for (p in predicates) {
                        def result = p.predicate(parsed)
                        if (result?.matched) {
                            // Set soft error properties for iFlow use
                            message.setProperty(Constants.SoftError.PROP_IS_SOFT_ERROR, true)
                            message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_REASON, p.reason)
                            message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_MESSAGE, result.message)
                            return [softError: true, reason: p.reason, message: result.message, isPropagated: false]
                        }
                    }
                } catch (Exception e) {
                    message.setProperty("isPayloadInvalid", "true")
                    message.setProperty("payloadParseError", e.message)
                    // Continue, so the flow can handle the technical error elsewhere
                }
            }
        }
        // Clear soft error properties if no soft error found
        message.setProperty(Constants.SoftError.PROP_IS_SOFT_ERROR, false)
        message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_REASON, null)
        message.setProperty(Constants.SoftError.PROP_SOFT_ERROR_MESSAGE, null)
        return [softError: false, isPropagated: false]
    }

    public static void throwCustomException(String reason, String msg = "") {
        throw new SoftErrorException(reason, msg)
    }

    public static class SoftErrorException extends RuntimeException {
        String reason
        int statusCode
        SoftErrorException(String reason, String message, int statusCode = 400) {
            super(message)
            this.reason = reason
            this.statusCode = statusCode
        }
        String getReason() { return reason }
        int getStatusCode() { return statusCode }
    }

    static List getDefaultXmlPredicates() {
        return [
            [reason: Constants.SoftError.EMPTY_XML_ROOT, predicate: { node ->
                if (node.children().isEmpty() && node.text().trim().isEmpty()) {
                    return [matched: true, message: "XML root node is empty"]
                }
                return [matched: false]
            }],
            [reason: Constants.SoftError.XML_ERROR_NODE, predicate: { node ->
                def errorNode = node.'**'.find { it.name() == 'property' && it.@id == 'error' }
                if (errorNode && errorNode.text().trim()) {
                    return [matched: true, message: errorNode.text().trim()]
                }
                return [matched: false]
            }],
            [reason: Constants.SoftError.XML_STATUS_FAILED, predicate: { node ->
                def statusNode = node.'**'.find { it.name() == 'property' && it.@id == 'status' }
                if (statusNode && statusNode.text().toUpperCase() == "FAILED") {
                    return [matched: true, message: "Response status is FAILED"]
                }
                return [matched: false]
            }]
        ]
    }
}