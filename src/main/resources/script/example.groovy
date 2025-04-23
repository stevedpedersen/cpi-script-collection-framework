// import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
// import com.sap.it.op.mpl.MessageProcessingLogFactory
// import com.sap.it.op.mpl.MessageProcessingLogPart
// import com.sap.it.op.mpl.impl.MPLShared
// import com.sap.it.op.mpl.impl.MessageProcessingLogPartV2Impl
// import com.sap.it.op.mpl.impl.MessageProcessingLogRootPartImpl

// Test Constructor - Run "SimpleTest"
def testInit(msg) {
    println "hellloooo"
    msg.setBody(new String("Hello Groovy World"))
    msg.setHeader("oldHeader", "MyGroovyHeader")
    msg.setProperty("oldProperty", "MyGroovyProperty")
    return msg
}


//Default Groovy CPI Function
def Message processData(Message message) {
    // Set body to match expected result
    message.setBody("Hello World-2")

    // Get header and modify
    def oldHeader = message.getHeaders().get("oldHeader")
    message.setHeader("oldHeader", oldHeader + "modified")
    message.setHeader("newHeader", "newHeader")

    // Get property and modify
    def oldProperty = message.getProperties().get("oldProperty")
    message.setProperty("oldProperty", oldProperty + "modified")
    message.setProperty("newProperty", "newProperty")

    return message
}

testInit()

// Message processDataBad(Message message) {
//     def guid = message.getProperty("SAP_MessageProcessingLogID")
//     MessageProcessingLogFactory factory = MessageProcessingLogFactory.getInstance()
//     MessageProcessingLogPart part = factory.create()
//     MessageProcessingLogRootPartImpl root = new MessageProcessingLogRootPartImpl("[SP]" as byte[], guid as String)
//     MessageProcessingLogPartV2Impl mpl2 = new MessageProcessingLogPartV2Impl(root)
//     println mpl2.controller.id
//     println  mpl2.controller.metaPropertyValues
//     return message
// }
// testInit()