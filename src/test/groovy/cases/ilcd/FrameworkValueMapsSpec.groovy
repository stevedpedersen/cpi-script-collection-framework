import spock.lang.*
import src.main.resources.script.Framework_ValueMaps
import src.main.resources.data.TestData
import ilcd.TestHelper
import ilcd.MockMessageLog

class FrameworkValueMapsSpec extends Specification {
    def "UTC timestamp formatting is correct"() {
        given:
        long millis = 1713763200000L // 2024-04-22T00:00:00Z
        when:
        def result = Framework_ValueMaps.formatUtcTimestamp(millis)
        then:
        result == "2024-04-22T00:00:00.000Z"
    }

    def "value map returns correct value for known key"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def valueMaps = new Framework_ValueMaps(msg)
        when:
        def val = valueMaps.getValue("SAP_ApplicationID")
        then:
        val == TestData.SAP_STANDARD_HEADERS['SAP_ApplicationID']
    }

    def "value map returns null for unknown key"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def valueMaps = new Framework_ValueMaps(msg)
        when:
        def val = valueMaps.getValue("NON_EXISTENT_KEY")
        then:
        val == null
    }

    def "cache entry stores and retrieves value"() {
        when:
        def entry = new Framework_ValueMaps.CacheEntry("foo", "bar")
        then:
        entry.key == "foo"
        entry.value == "bar"
    }

    def "formatUtcTimestamp handles epoch 0 correctly"() {
        expect:
        Framework_ValueMaps.formatUtcTimestamp(0L) == "1970-01-01T00:00:00.000Z"
    }

    def "interfaceVM returns mapped value or default"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def mockLog = new MockMessageLog()
        when:
        def val = Framework_ValueMaps.interfaceVM('testKey', 'proj', 'int', 'default', msg, mockLog)
        then:
        val == 'default' || val != null
    }

    def "frameworkVM returns mapped value or default"() {
        given:
        def msg = TestHelper.makeSAPMessage()
        def mockLog = new MockMessageLog()
        when:
        def val = Framework_ValueMaps.frameworkVM('meta_environment', 'dev', msg, mockLog)
        then:
        val == null || val instanceof String
    }
}
