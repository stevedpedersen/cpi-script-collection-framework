package cases.ilcd

import groovy.util.slurpersupport.GPathResult
import spock.lang.Specification
import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_ExceptionHandler
import com.sap.it.api.msglog.MessageLog

class FrameworkExceptionHandlerSpec extends Specification {
    def "should parse error from XML with <property id='error'>"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="error"&gt;Something went wrong&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = Mock(Message)
        def messageLog = Mock(MessageLog)
        message.getBody(String) >> xml
        message.getHeaders() >> [:]
        message.getProperty(_) >> null
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Something went wrong'
        handler.error.type == 'XML_PARSE'
        handler.error.statusCode == 500
    }

    def "should parse error from XML with <property id='status'>FAILED"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="status"&gt;FAILED&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = Mock(Message)
        def messageLog = Mock(MessageLog)
        message.getBody(String) >> xml
        message.getHeaders() >> [:]
        message.getProperty(_) >> null
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Response status is FAILED.'
        handler.error.type == 'XML_PARSE'
        handler.error.statusCode == 500
    }

    def "should not set error if XML is clean"() {
        given:
        def xml = '''<root><processActionReturn>&lt;root&gt;&lt;property id="status"&gt;SUCCESS&lt;/property&gt;&lt;/root&gt;</processActionReturn></root>'''
        def message = Mock(Message)
        def messageLog = Mock(MessageLog)
        message.getBody(String) >> xml
        message.getHeaders() >> [:]
        message.getProperty(_) >> null
        def handler = new Framework_ExceptionHandler(message, messageLog)

        when:
        handler.setErrorProperties()

        then:
        handler.error.message == 'Internal Server Error' // default, not changed
        handler.error.type == ''
        handler.error.statusCode == 500
    }
}
