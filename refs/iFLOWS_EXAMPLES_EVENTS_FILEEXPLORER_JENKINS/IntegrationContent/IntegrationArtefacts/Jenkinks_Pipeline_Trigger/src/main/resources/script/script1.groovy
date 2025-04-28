import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.*

def Message processData(Message message) {
    def json = new JsonSlurper().parseText(message.getBody(String));
    message.setHeader('name',json.bundleName)
    message.setHeader('Authorization','Bearer 6c095ff7-a27d-4efc-88e5-51b86f379d06')
    return message;
}