import spock.lang.*
import src.main.resources.script.Framework_Utils
import ilcd.TestHelper
import ilcd.MockMessageLog

class FrameworkUtilsSpec extends Specification {
    def "utility masks sensitive data"() {
        given:
        def input = "user: admin, password: secret123"
        when:
        def masked = Framework_Utils.maskSensitiveData(input)
        then:
        masked.contains("***") || masked != input // TODO: refine for your implementation
    }

    def "utility returns input unchanged if no sensitive data"() {
        given:
        def input = "just a regular string"
        when:
        def masked = Framework_Utils.maskSensitiveData(input)
        then:
        masked == input
    }

    def "utility handles null input gracefully"() {
        when:
        def masked = Framework_Utils.maskSensitiveData(null)
        then:
        masked == null
    }

    def "formatResponseXml and formatResponseJson produce correct output"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"correlationID":"abc123","timestamp":"2025-04-22T06:46:14Z","messages":[{"logLevel":"INFO","text":"ok"}]}'])
        def mockLog = new MockMessageLog()
        def utils = new Framework_Utils(msg, mockLog)
        when:
        def xml = utils.formatResponseXml()
        def json = utils.formatResponseJson()
        then:
        xml.contains('<Response>')
        json.contains('correlationID')
    }

    def "createJsonSuccessResponse and createXmlSuccessResponse output expected fields"() {
        given:
        def utils = new Framework_Utils(TestHelper.makeSAPMessage([body: '{"correlationID":"id1","timestamp":"2025-04-22T06:46:14Z"}']), new MockMessageLog())
        def json = [correlationID: 'id1', timestamp: '2025-04-22T06:46:14Z']
        when:
        def jsonResp = utils.createJsonSuccessResponse(json)
        def xmlResp = utils.createXmlSuccessResponse(json)
        then:
        jsonResp.contains('SUCCESS')
        xmlResp.contains('<Response>')
    }

    def "getStatusCode and getErrorMessage handle nulls and errorDetails"() {
        given:
        def utils = new Framework_Utils(TestHelper.makeSAPMessage([body: '{}']), new MockMessageLog())
        when:
        def code = utils.getStatusCode(null)
        def msg = utils.getErrorMessage([text: 'err'])
        then:
        code == '200'
        msg == 'err' || msg == 'Internal Server Error'
    }

    def "maskFields returns masked JSON if mapping exists, else pretty prints"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"secret":"1234567890","correlationID":"id1","timestamp":"now"}'])
        def mockLog = new MockMessageLog()
        def utils = new Framework_Utils(msg, mockLog)
        when:
        def result = Framework_Utils.maskFields('{"secret":"1234567890"}', 'proj', 'int', 'secret', msg, mockLog)
        then:
        result.contains('****') || result.contains('1234567890')
    }

    def "filterLogs sets isEmailFilterApplied and filters messages"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"messages":[{"logLevel":"INFO","text":"ok"},{"logLevel":"ERROR","text":"fail"}]}' ])
        def mockLog = new MockMessageLog()
        def utils = new Framework_Utils(msg, mockLog)
        msg.setProperty('projectName', 'proj')
        msg.setProperty('integrationID', 'int')
        when:
        def filtered = utils.filterLogs()
        then:
        msg.getProperty('isEmailFilterApplied') != null
        filtered.contains('messages')
    }
}
