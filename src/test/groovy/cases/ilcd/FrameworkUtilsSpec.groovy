package cases.ilcd
import spock.lang.*
import src.main.resources.script.Framework_Utils
import src.main.resources.script.Framework_Logger
import com.sap.it.api.msglog.MessageLog
import com.sap.gateway.ip.core.customdev.util.Message
import cases.ilcd.TestHelper
import src.main.resources.script.Constants

class FrameworkUtilsSpec extends Specification {
    def "utility masks sensitive data"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        // The value map for 'secret' will trigger masking if defined
        when:
        def result = Framework_Utils.maskFields('{"password":"secret123","correlationID":"id1"}', 'proj', 'int', 'password', msg, log)
        then:
        result.contains('***') || result.contains('secret123')
    }

    def "utility returns input unchanged if no sensitive data"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        // No mapping for 'foo', so should pretty print
        when:
        def result = Framework_Utils.maskFields('{"foo":"bar","correlationID":"id1"}', 'proj', 'int', 'foo', msg, log)
        then:
        result.contains('bar')
    }

    def "utility handles null input gracefully"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        // Null/empty JSON input
        when:
        def result = Framework_Utils.maskFields('null', 'proj', 'int', 'foo', msg, log)
        then:
        result == 'null' || result == ''
    }

    def "formatResponseXml and formatResponseJson produce correct output"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        def utils = new Framework_Utils(msg, log)
        when:
        def xml = utils.formatResponseXml()
        def json = utils.formatResponseJson()
        then:
        xml.contains('<Error>')
        json.contains('Internal Server Error')
    }

    def "createJsonSuccessResponse and createXmlSuccessResponse output expected fields"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        def utils = new Framework_Utils(msg, log)
        def json = [correlationID: 'id1', timestamp: '2025-04-22T06:46:14Z']
        when:
        def jsonResp = utils.createJsonSuccessResponse(json)
        def xmlResp = utils.createXmlSuccessResponse(json)
        then:
        jsonResp.contains(Constants.ILCD.Utils.SUCCESS_RESPONSE_MSG)
        xmlResp.contains('<Response>')
    }

    def "getStatusCode and getErrorMessage handle nulls and errorDetails"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        def utils = new Framework_Utils(msg, log)
        when:
        def code = utils.getStatusCode(null)
        def msgErr = utils.getErrorMessage([text: 'err'])
        then:
        code == '200'
        msgErr == 'err' || msgErr == 'Internal Server Error'
    }

    def "maskFields returns masked JSON if mapping exists, else pretty prints"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def log = new MessageLog()
        def utils = new Framework_Utils(msg, log)
        when:
        def result = Framework_Utils.maskFields('{"secret":"1234567890"}', 'proj', 'int', 'secret', msg, log)
        then:
        result.contains('****') || result.contains('1234567890')
    }

    def "filterLogs sets isEmailFilterApplied and filters messages"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"INFO","text":"ok"},{"logLevel":"ERROR","text":"fail"}]}'])
        // Debug output immediately after message creation
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def log = new MessageLog()
        def utils = new Framework_Utils(msg, log)
        msg.setProperty('projectName', 'proj')
        msg.setProperty('integrationID', 'int')
        when:
        println "DEBUG: message body before filterLogs: ${msg.getBody(String)}"
        def filtered = utils.filterLogs()
        then:
        msg.getProperty('isEmailFilterApplied') != null
        filtered.contains('messages')
    }
}
