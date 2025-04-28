package cases.ilcd

import groovy.util.slurpersupport.GPathResult
import spock.lang.Specification
import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_ExceptionHandler
import src.main.resources.script.Constants
import com.sap.it.api.msglog.MessageLog
import cases.ilcd.TestHelper

class FrameworkExceptionHandlerSpec extends Specification {
    def "should parse error from XML with <property id='error'>"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="error"&gt;Something went wrong&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(xml)
        message.headers = [:]
        message.properties = [:]
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should parse error from XML with <property id='status'>FAILED"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="status"&gt;FAILED&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(xml)
        message.headers = [:]
        message.properties = [:]
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should not set error if XML is clean"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="status"&gt;SUCCESS&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(xml)
        message.headers = [:]
        message.properties = [:]
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should detect and set soft error from SoftErrorException"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.headers = [:]
        message.properties = [(src.main.resources.script.Constants.Property.CAMEL_EXC_CAUGHT): new src.main.resources.script.Framework_ExceptionHandler.SoftErrorException("EMPTY_BODY", "Body is empty")]
        message.setBody("")
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.className == 'RuntimeException'
        handler.error.message == 'Internal Server Error'
        handler.error.statusCode == 500
        handler.error.type == 'TECHNICAL'
    }

    def "should unwrap and detect soft error from nested ScriptException"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        def innerEx = new src.main.resources.script.Framework_ExceptionHandler.SoftErrorException("EMPTY_BODY", "Body is empty")
        def scriptEx = new javax.script.ScriptException(innerEx)
        message.headers = [:]
        message.properties = [(src.main.resources.script.Constants.Property.CAMEL_EXC_CAUGHT): scriptEx]
        message.setBody("")
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.className == 'RuntimeException'
        handler.error.message == 'Internal Server Error'
        handler.error.statusCode == 500
        handler.error.type == 'TECHNICAL'
    }

    def "should not set soft error if no exception and body is not empty"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.headers = [:]
        message.properties = [:]
        message.setBody("<root>OK</root>")
        def messageLog = new com.sap.it.api.msglog.MessageLog()
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.className == 'RuntimeException'
        handler.error.message == 'Internal Server Error'
        handler.error.statusCode == 500
        handler.error.type == 'TECHNICAL'
    }

    def "should parse error from JSON body"() {
        given:
        def json = '{"error": {"message": "JSON error occurred", "type": "FUNCTIONAL", "statusCode": 400}}'
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(json)
        message.headers = [:]
        message.properties = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should handle empty body as soft error"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody("")
        message.headers = [:]
        message.properties = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should handle malformed XML body"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody("<root><unclosedTag>")
        message.headers = [:]
        message.properties = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should set hasRetry true if SAP_IS_REDELIVERY_ENABLED is true"() {
        given:
        // def message = new com.sap.gateway.ip.core.customdev.util.Message()
        def message = TestHelper.makeRetrySAPMessage()
        message.headers = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:     
        handler.error.hasRetry == true
    }

    def "should set hasRetry false if SAP_IS_REDELIVERY_ENABLED is missing or false"() {
        given:
        def message = TestHelper.makeErrorSAPMessage()
        message.headers = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:
        handler.error.hasRetry == false
    }

    def "should handle null body gracefully"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(null)
        message.headers = [:]
        message.properties = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should handle messageLog and custom headers"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="error"&gt;Header error&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody(xml)
        message.headers = [:]
        message.properties = [integrationID: 'IF_My_IFlow',projectName: 'IP_My_Package']
        def messageLog = Mock(com.sap.it.api.msglog.MessageLog)
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, messageLog)
        when:
        handler.handleError(true, "TEST_POINT")
        then:
        (1.._) * messageLog.addCustomHeaderProperty(_, _)
    }

    def "should throw and catch custom SoftErrorException"() {
        when:
        src.main.resources.script.Framework_ExceptionHandler.throwCustomException("MY_REASON", "my message")
        then:
        def e = thrown(src.main.resources.script.Framework_ExceptionHandler.SoftErrorException)
        e.reason == "MY_REASON"
        e.message == "my message"
        e.statusCode == 400
    }

    def "should handle body that is neither XML nor JSON"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody("just some text")
        message.headers = [:]
        message.properties = [:]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)
        when:
        handler.setErrorProperties()
        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }

    def "should handle error with adapter-specific logic"() {
        given:
        def message = new com.sap.gateway.ip.core.customdev.util.Message()
        message.setBody('<root/>')
        message.headers = [:]
        message.properties = [(src.main.resources.script.Constants.Property.CAMEL_EXC_CAUGHT): new Exception("Adapter error")]
        def handler = new src.main.resources.script.Framework_ExceptionHandler(message, null)
        when:
        handler.setErrorProperties()
        then:
        handler.error.message == 'Internal Server Error'
        handler.error.type == 'TECHNICAL'
        handler.error.statusCode == 500
    }
}
