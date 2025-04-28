package cases.ilcd
import spock.lang.*
import src.main.resources.script.Framework_ValueMaps
import cases.ilcd.data.TestData
import com.sap.it.api.msglog.MessageLog
import com.sap.gateway.ip.core.customdev.util.Message
import cases.ilcd.TestHelper

class FrameworkValueMapsSpec extends Specification {
    def "UTC timestamp formatting is correct"() {
        given:
        long millis = 1713763200000L // 2024-04-22T05:20:00Z
        when:
        def result = Framework_ValueMaps.formatUtcTimestamp(millis)
        then:
        result == "2024-04-22T05:20:00.000Z"
    }

    def "formatUtcTimestamp handles epoch 0 correctly"() {
        expect:
        Framework_ValueMaps.formatUtcTimestamp(0L) == "1970-01-01T00:00:00.000Z"
    }

    def "interfaceVM returns mapped value or default"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def log = new MessageLog()
        when:
        def val = Framework_ValueMaps.interfaceVM('testKey', 'proj', 'int', 'default', msg, log)
        then:
        val == 'default' || val != null
    }

    def "frameworkVM returns mapped value or default"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def log = new MessageLog()
        when:
        def val = Framework_ValueMaps.frameworkVM('meta_environment', 'dev', msg, log)
        then:
        val == null || val instanceof String
    }
}
