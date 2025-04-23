import spock.lang.*
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Constants
import ilcd.TestHelper
import ilcd.MockMessageLog

class FrameworkLoggerSpec extends Specification {
    def "log level normalization works for various inputs"() {
        expect:
        Constants.ILCD.normalizeLogLevel("ERROR") == "ERROR"
        Constants.ILCD.normalizeLogLevel(["WARN"]) == "WARN"
        Constants.ILCD.normalizeLogLevel("foo", "DEBUG") == "DEBUG"
    }

    def "attachable logic: under limit, over limit, trace log, retry, and loop"() {
        given:
        def logger = new Framework_Logger(TestHelper.makeSAPMessage())
        logger.logCounter = 2
        logger.tracePoint = "END"
        expect:
        logger.isAttachable("END", "END_LOG")
        when:
        logger.logCounter = 8
        then:
        !logger.isAttachable("END", "END_LOG")
        when:
        logger.tracePoint = "TRACE"
        logger.logCounter = 2
        logger.message.properties["CamelSplitIndex"] = 1
        then:
        !logger.isAttachable("TRACE", "TRACE_LOG")
        when:
        logger.message.headers[Constants.Header.SAP_DATASTORE_RETRIES] = 1
        then:
        !logger.isAttachable("TRACE", "TRACE_LOG")
        when:
        logger.message.properties[Constants.Property.SAP_IS_REDELIVERY_ENABLED] = false
        logger.message.headers[Constants.Header.SAP_DATASTORE_RETRIES] = 0
        logger.tracePoint = "END"
        logger.logCounter = 3
        then:
        logger.isAttachable("END", "END_LOG")
    }

    def "maskSensitiveData masks known fields and does not mask safe fields"() {
        given:
        def logger = new Framework_Logger(TestHelper.makeSAPMessage())
        expect:
        logger.maskSensitiveData("My password is secret123") != "My password is secret123"
        logger.maskSensitiveData("Nothing sensitive here") == "Nothing sensitive here"
    }

    def "logger formats message body, headers, and properties correctly"() {
        given:
        def logger = new Framework_Logger(TestHelper.makeSAPMessage())
        when:
        def body = logger.message.getBody()
        def headers = logger.message.getHeaders()
        def props = logger.message.getProperties()
        then:
        body == TestHelper.sampleBody
        headers["SAP_ApplicationID"] == TestHelper.sampleHeaders["SAP_ApplicationID"]
        props["SAP_ProjectName"] == TestHelper.sampleProperties["SAP_ProjectName"]
    }

    def "logger handles missing headers and properties gracefully"() {
        given:
        def logger = new Framework_Logger(TestHelper.makeSAPMessage(headers: [:], properties: [:]))
        when:
        def headers = logger.message.getHeaders()
        def props = logger.message.getProperties()
        then:
        headers.isEmpty()
        props.isEmpty()
    }

    def "logMessage appends to message log stack"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def mockLog = new MockMessageLog()
        def logger = new Framework_Logger(msg, mockLog)
        when:
        logger.logMessage("TRACE", "INFO", "Test log entry")
        then:
        def stack = msg.getProperty(Constants.ILCD.LOG_STACK_PROPERTY)
        stack != null
        stack.toString().contains("Test log entry")
    }

    def "handleScriptError sets error properties and headers"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def mockLog = new MockMessageLog()
        def logger = new Framework_Logger(msg, mockLog)
        when:
        Framework_Logger.handleScriptError(msg, mockLog, new Exception("fail!"), "testFunction", true)
        then:
        msg.getProperties().any { k, v -> k.toString().toLowerCase().contains("exc") }
        mockLog.headers.any { k, v -> k.toString().toLowerCase().contains("exc") }
    }
}
