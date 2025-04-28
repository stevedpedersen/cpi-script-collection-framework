package cases.ilcd
import cases.ilcd.data.TestData
import src.main.resources.script.Framework_Logger
class TestHelper {
    static {
        // Global mock: disables getSystemDetails errors in ALL tests
        Framework_Logger.metaClass.getSystemDetails = { -> [:] }
    }
    static String sampleBody = '{"projectName":"proj","integrationID":"int","messages":[{"logLevel":"ERROR","text":"fail"}]}'
    static Map sampleHeaders = [SAP_MplCorrelationId: 'Abc123deF456GHi789_jKl0']
    static Map sampleProperties = [integrationID: "I-0123-MasterData", projectName: "Transcend", SAP_MessageProcessingLogID: 'Abc123deF456GHi789_jKl0'] 
    static Map sampleErrorProperties = sampleProperties +[CamelExceptionCaught: new Exception("fail!")]
    static Map sampleRetryProperties = sampleProperties + [SAP_isComponentRedeliveryEnabled: true]
    static Map sampleErrorDetails = [text: "fail", statusCode: 500, errorLocation: "errorLocation", errorStepID: "errorStepID", timestamp: "2023-01-01T00:00:00Z"]
    static def makeSAPMessage(Map opts = [:]) {
        def msg = new com.sap.gateway.ip.core.customdev.util.Message()
        def ctx = new org.apache.camel.impl.DefaultCamelContext()
        def exch = new org.apache.camel.support.DefaultExchange(ctx)
        msg.exchange = exch
        // Always provide a valid JSON body if not explicitly set or if blank/null/whitespace
        def body = opts.body
        if (!(body instanceof String) || !body?.trim()) {
            body = sampleBody
        }
        msg.setBody(body)
        // Debug output after setting body
        println "DEBUG: (makeSAPMessage) body after setBody: ${msg.getBody(String)}"
        (opts.headers ?: sampleHeaders).each { k, v -> msg.setHeader(k, v) }
        (opts.properties ?: sampleProperties).each { k, v -> msg.setProperty(k, v) }
        return msg
    }
    static def makeErrorSAPMessage(Map opts = [:]) {
        def msg = makeSAPMessage(opts)
        (opts.properties ?: sampleErrorProperties).each { k, v -> msg.setProperty(k, v) }
        return msg
    }
    static def makeRetrySAPMessage(Map opts = [:]) {
        def msg = makeSAPMessage(opts)
        (opts.properties ?: sampleRetryProperties).each { k, v -> msg.setProperty(k, v) }
        return msg
    }
    static def makeMessage(Map opts = [:]) {
        def msg = new com.sap.gateway.ip.core.customdev.util.Message()
        def ctx = new org.apache.camel.impl.DefaultCamelContext()
        def exch = new org.apache.camel.support.DefaultExchange(ctx)
        msg.exchange = exch
        def body = opts.body
        if (!(body instanceof String) || !body?.trim()) {
            body = sampleBody
        }
        msg.setBody(body)
        (opts.headers ?: sampleHeaders).each { k, v -> msg.setHeader(k, v) }
        (opts.properties ?: sampleProperties).each { k, v -> msg.setProperty(k, v) }
        return msg
    }
    // TODO: Add more helpers/mocks as needed
}
