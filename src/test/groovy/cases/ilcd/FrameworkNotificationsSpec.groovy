import spock.lang.*
import src.main.resources.script.Framework_Notifications
import src.main.resources.data.TestData
import ilcd.TestHelper
import ilcd.MockMessageLog

class FrameworkNotificationsSpec extends Specification {
    def "notification triggers on error property"() {
        given:
        def msg = TestHelper.makeSAPMessage(properties: [SAP_isComponentRedeliveryEnabled: true])
        def notifications = new Framework_Notifications(msg)
        when:
        def triggered = notifications.shouldNotify()
        then:
        triggered == true // or whatever the logic requires
    }

    def "notification does not trigger when property is false"() {
        given:
        def msg = TestHelper.makeSAPMessage(properties: [SAP_isComponentRedeliveryEnabled: false])
        def notifications = new Framework_Notifications(msg)
        when:
        def triggered = notifications.shouldNotify()
        then:
        triggered == false // or whatever the logic requires
    }

    def "notification handles missing property gracefully"() {
        given:
        def msg = TestHelper.makeSAPMessage(properties: [:])
        def notifications = new Framework_Notifications(msg)
        when:
        def triggered = notifications.shouldNotify()
        then:
        !triggered // or whatever the logic requires
    }

    def "escapeHtml escapes special characters"() {
        given:
        def notif = new Framework_Notifications(TestHelper.makeSAPMessage(), null)
        expect:
        notif.escapeHtml('<script>"&\'\n') == "&lt;script&gt;&quot;&amp;#39;<br>"
    }

    def "formatPropertyName formats property names nicely"() {
        given:
        def notif = new Framework_Notifications(TestHelper.makeSAPMessage(), null)
        expect:
        notif.formatPropertyName("SAP_MessageProcessingLogID") == "Sap message processing log id"
        notif.formatPropertyName("errorType") == "Error type"
    }

    def "formatHtmlBody sets HTML body and subject"() {
        given:
        def msg = TestHelper.makeSAPMessage([body: '{"correlationID":"c1","timestamp":"t1","errorType":"E"}'])
        def mockLog = new MockMessageLog()
        def notif = new Framework_Notifications(msg, mockLog)
        when:
        notif.formatHtmlBody()
        then:
        msg.getBody(String).contains('<table')
        msg.getProperty('emailSubject')
    }

    def "createMessagesHtml renders HTML for error messages"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def mockLog = new MockMessageLog()
        def notif = new Framework_Notifications(msg, mockLog)
        def messages = [[logLevel: 'ERROR', text: 'fail', foo: 'bar']]
        when:
        def html = notif.createMessagesHtml(messages)
        then:
        html.contains('<dl')
        html.contains('fail')
        html.contains('bar')
    }
    // TODO: Add more tests for Framework_Notifications.groovy
}
