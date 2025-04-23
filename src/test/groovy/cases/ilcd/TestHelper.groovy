import src.main.resources.data.TestData
class TestHelper {
    static String sampleBody = "Hello World"
    static Map<String,Object> sampleHeaders = new TestData().SAP_STANDARD_HEADERS
    static Map<String,Object> sampleProperties = new TestData().SAP_STANDARD_PROPERTIES
    static def makeSAPMessage(Map opts = [:]) {
        def msg = new com.sap.gateway.ip.core.customdev.util.Message()
        def headers = sampleHeaders + (opts.headers ?: [:])
        def props = sampleProperties + (opts.properties ?: [:])
        headers.each { k, v -> msg.setHeader(k, v) }
        props.each { k, v -> msg.setProperty(k, v) }
        msg.setBody(opts.body ?: sampleBody)
        return msg
    }
    static def makeMessage(Map opts = [:]) {
        def msg = new com.sap.gateway.ip.core.customdev.util.Message()
        msg.setBody(opts.body ?: sampleBody)
        (opts.headers ?: sampleHeaders).each { k, v -> msg.setHeader(k, v) }
        (opts.properties ?: sampleProperties).each { k, v -> msg.setProperty(k, v) }
        return msg
    }
    // TODO: Add more helpers/mocks as needed
}
