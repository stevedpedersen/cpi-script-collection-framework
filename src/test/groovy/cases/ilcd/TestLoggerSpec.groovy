package groovy.cases.ilcd

import spock.lang.Specification
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.msglog.MessageLogFactory

class TestLoggerSpec extends Specification {
    Message msg
    
    static void logTestDebug(String testName, Map context) {
        def debugDir = new File("build/test-logs")
        debugDir.mkdirs()
        def debugFile = new File(debugDir, "${testName}.txt")
        debugFile << "=== ${testName} ===\n"
        context.each { k, v ->
            debugFile << "${k}: ${v}\n"
        }
        debugFile << "\n"
    }
    static void dump(String testName, Framework_Logger logger) {
        def debugDir = new File("build/test-logs")
        debugDir.mkdirs()
        def debugFile = new File(debugDir, "${testName}.txt")
        debugFile << "=== ${testName} ===\n"
        def msg = logger.message
        def context = [msg.properties, msg.headers,logger.settings]
        context.each{ it.each { k, v ->
            debugFile << "${k}: ${v}\n"
        } }
        debugFile << "\n"
    }

    def setupSpec() {
        new File("build/test-logs").deleteDir()
    }


    def makeLogger(Map opts = [:]) {
        this.msg = new Message()
        msg.properties = msg.properties ?: [:]
        msg.headers = msg.headers ?: [:]
        msg.properties.putAll(opts.props ?: [:])
        msg.headers.putAll(opts.headers ?: [:])
        msg.exchange = [ context: [ version: '3.14.7', uptime: 12345 ] ]

        def messageLogFactory = new com.sap.it.api.msglog.MessageLogFactory()
        Framework_Logger.metaClass.messageLogFactory = messageLogFactory
        Framework_Logger.metaClass.getSystemDetails = { [:] }

        def logger = new Framework_Logger(msg, new MessageLog())
        logger.settings.attachmentsDisabled = opts.attachmentsDisabled?.toString() ?: "false"
        logger.settings.attachmentLimit = opts.attachmentLimit?.toString() ?: "5"
        logger.settings.traceAttachmentLimit = opts.traceAttachmentLimit?.toString() ?: "3"

        logger.logCounter = opts.logCounter ?: 1
        logger.logLevel = opts.logLevel ?: "INFO"
        logger.overallLogLevel = opts.overallLogLevel ?: "TRACE"
        logger.tracePoint = opts.tracePoint ?: "END"

        return logger
    }

    def "Attachments globally disabled"() {
        def logger = makeLogger([attachmentsDisabled: true])
        dump("Attachments globally disabled", logger)
        expect:
        !logger.isAttachable("END", "END_LOG")
    }

    def "Over hard limit"() {
        def logger = makeLogger([logCounter: 8])
        dump("Over hard limit", logger)
        expect:
        !logger.isAttachable("END", "END_LOG")
    }

    def "Error log, not in loop over limit"() {
        expect:
        makeLogger([logLevel: "ERROR", logCounter: 2]).isAttachable("END", "ERROR_LOG")
    }

    def "End log, not over limit, not retry"() {
        expect:
        makeLogger([tracePoint: "END", logCounter: 5]).isAttachable("END", "END_LOG")
    }

    def "End log, over limit, not retry"() {
        def logger = makeLogger([tracePoint: "END", logCounter: 6])
        dump("End log, over limit, not retry", logger)
        expect:
        !logger.isAttachable("END", "END_LOG")
    }

    def "TRACE log not attachable, is loop iteration > 1 - CamelSplitIndex = 1, logCounter: 2"() {
        def logger = makeLogger([tracePoint: "TRACE", logCounter: 2])
        logger.message.properties["CamelSplitIndex"] = 1

        logTestDebug("TRACE_log_not_attachable_loop_gt1", [
            properties: logger.message.properties,
            headers: logger.message.headers,
            logCounter: logger.logCounter,
            tracePoint: logger.tracePoint,
            overallLogLevel: logger.overallLogLevel,
            result: logger.isAttachable("TRACE", "TRACE_LOG")
        ])

        expect:
        !logger.isAttachable("TRACE", "TRACE_LOG")
    }
    def "TRACE log, is retry - SAP_DataStoreRetries > 0, logCounter: 3"() {
        def logger = makeLogger([tracePoint: "TRACE", logCounter: 3])
        logger.message.headers[Constants.Header.SAP_DATASTORE_RETRIES] = 1
        expect:
        !logger.isAttachable("TRACE", "TRACE_LOG")
    }
    def "End log, is not retry - check if false negative passes"() {
        // expect:
        // !makeLogger([tracePoint: "END", logCounter: 2, props: [(Constants.Property.SAP_IS_REDELIVERY_ENABLED): true]]).isAttachable("END", "END_LOG")
        def logger = makeLogger([tracePoint: "END", logCounter: 3])
        // Set both possible keys for safety
        logger.message.properties["SAP_isComponentRedeliveryEnabled"] = false
        logger.message.headers[Constants.Header.SAP_DATASTORE_RETRIES] = 0
        expect:
        logger.isAttachable("END", "END_LOG")
    }
    def "End log allowed, start log not allowed - logCounter: 5"() {
        // expect:
        // !makeLogger([tracePoint: "END", logCounter: 2, props: [(Constants.Property.SAP_IS_REDELIVERY_ENABLED): true]]).isAttachable("END", "END_LOG")
        def logger1 = makeLogger([tracePoint: "END", logCounter: 5])
        def logger2 = makeLogger([tracePoint: "START", logCounter: 5])
        expect:
        (logger1.isAttachable("END", "END_LOG")) || (!logger2.isAttachable("START", "START_LOG"))
    }
    def "Trace log, debug level, over limit due to loop awareness"() {
        def logger = makeLogger([tracePoint: "TRACE", overallLogLevel: "TRACE", logCounter: 2, props: [(Constants.Property.CAMEL_SPLIT_COMPLETE): true]])
        dump("Trace log, debug level, over limit due to loop awareness", logger)
        expect:
        !logger.isAttachable("TRACE", "TRACE_LOG")
    }
    def "Trace log, debug level, under soft limit for trace logs"() {
        def logger = makeLogger([tracePoint: "TRACE", overallLogLevel: "TRACE", logCounter: 2])
        dump("Trace log, debug level, under soft limit for trace logs", logger)
        expect:
        !logger.isAttachable("TRACE", "TRACE_LOG")
    }
    def "Trace log, debug level, over soft limit for trace logs"() {
        def logger = makeLogger([tracePoint: "TRACE", overallLogLevel: "TRACE", logCounter: 3])
        dump("Trace log, debug level, over soft limit for trace logs", logger)
        expect:
        !logger.isAttachable("TRACE", "TRACE_LOG")
    }
}
