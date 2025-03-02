import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {
    def headers = message.getHeaders()
    def properties = message.getProperties()
    
    try {
        def bodyJson = new JsonSlurper().parseText(message.getBody(String))
        def correlationID = bodyJson?.correlationID ?: headers.get("SAP_MplCorrelationId") ?: properties.get("correlationID")

        // If correlationID is found, place it at the top of the JSON
        if (correlationID) {
            bodyJson = [correlationID: correlationID] + bodyJson
        }

        // Remove retryMessageBody if present within retryParameters
        if (bodyJson.retryParameters) {
            bodyJson.retryParameters.remove("retryMessageBody")
        }

        // Convert the modified JSON back to a string
        def modifiedBody = JsonOutput.prettyPrint(JsonOutput.toJson(bodyJson))

        // Set the modified body back to the message
        message.setBody(modifiedBody)

        return attachBodyWithLabel(message, "JSON Message Log")

    } catch (Exception ignored) {}

    // Fallback - Attach body as a string
    return attachAsPayload(message)
}

def Message attachAsPayload(Message message) {
    return attachBodyWithLabel(message, "Payload")
}

def Message attachBodyWithLabel(Message message, String label) {
    def api = ITApiFactory.getApi(ValueMappingApi.class, null)
    def mplByteLimit = "1024"
    try {
        mplByteLimit = api.getMappedValue("Input", "IP_FoundationFramework", "setting_mplByteLimit", "Output", "VM_Framework_Global_Metadata")
    } catch (Exception ignored) {}
    def byteLimit = mplByteLimit != null ? Integer.parseInt(mplByteLimit) : 1024

    def body = message.getBody(String) as String
    
    // Check if the body size exceeds the byte limit (eg 1024 = 1024 * 1024 bytes = 1mb)
    if (body.size() > byteLimit * 1024) {
        // Truncate the body if it exceeds 1 MB
        body = body.substring(0, byteLimit * 1024)
    }
    
    def messageLog = messageLogFactory.getMessageLog(message)
    messageLog.addAttachmentAsString(label, body, "text/plain")
    
    return message
}