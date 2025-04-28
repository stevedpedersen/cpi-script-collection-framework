package cases.ilcd
import spock.lang.*
import src.main.resources.script.Framework_Notifications
import cases.ilcd.data.TestData
import com.sap.it.api.msglog.MessageLog
import com.sap.gateway.ip.core.customdev.util.Message
import cases.ilcd.TestHelper

class FrameworkNotificationsSpec extends Specification {
    def "escapeHtml escapes special characters"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}], "correlationID":"c1","timestamp":"t1","errorType":"T"}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        expect:
        notif.escapeHtml('<script>"&\'\n') == "&lt;script&gt;&quot;&amp;&#39;<br>"
    }

    def "formatPropertyName formats property names nicely"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}]}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        expect:
        notif.formatPropertyName("SAP_MessageProcessingLogID") == "SAP Message Processing Log ID"
    }

    def "formatHtmlBody sets HTML body and subject"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}]}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        when:
        println "DEBUG: message body before formatHtmlBody: ${msg.getBody(String)}"
        notif.formatHtmlBody()
        then:
        msg.getProperty('emailSubject').contains("[TECHNICAL ERROR]")
    }

    def "createMessagesHtml renders HTML for error messages"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}]}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        def messages = [[logLevel: 'ERROR', text: 'fail', errorLocation: 'SAP_ErrorModelStepID', errorType: 'TECHNICAL']]
        when:
        def html = notif.createMessagesHtml(messages)
        then:
        html.contains('<dl')
        html.contains('fail')
        html.contains('SAP_ErrorModelStepID')
        html.contains('TECHNICAL')
    }

    def "resolveEmailRecipients picks error_type from root"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}], "error_type":"TECHNICAL"}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        def json = [
            error_type: "TECHNICAL",
            messages: [
                [logLevel: "ERROR", text: "fail"]
            ]
        ]
        when:
        def result = notif.resolveEmailRecipients(json, "proj", "intg")
        then:
        result.errorType == "TECHNICAL"
    }

    def "resolveEmailRecipients picks errorType from nested messages"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}]}'])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        def json = [
            messages: [
                [logLevel: "ERROR", text: "fail", errorType: "FUNCTIONAL"]
            ]
        ]
        when:
        def result = notif.resolveEmailRecipients(json, "proj", "intg")
        then:
        result.errorType == "FUNCTIONAL"
    }

    def "formatHtmlBody uses error_type from root in subject"() {
        given:
        def body = '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}], "error_type":"TECHNICAL"}'
        def msg = TestHelper.makeSAPMessage([body: body])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        when:
        println "DEBUG: message body before formatHtmlBody: ${msg.getBody(String)}"
        notif.formatHtmlBody()
        then:
        msg.getProperty('emailSubject').contains("[TECHNICAL ERROR]")
    }

    def "formatHtmlBody uses errorType from nested messages in subject"() {
        given:
        def body = '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail","errorType":"FUNCTIONAL"}]}'
        def msg = TestHelper.makeSAPMessage([body: body])
        println "DEBUG: message body after makeSAPMessage: ${msg.getBody(String)}"
        def notif = new Framework_Notifications(msg, new MessageLog())
        when:
        println "DEBUG: message body before formatHtmlBody: ${msg.getBody(String)}"
        notif.formatHtmlBody()
        then:
        msg.getProperty('emailSubject').contains("[FUNCTIONAL ERROR]")
    }
}
