// --- LEGACY LOGIC (Commented Out: Use YAML-driven section below) ---
/*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.net.HttpURLConnection

// --- CONFIGURATION ---
def valueMappingId = 'VM_Dev_FrameworkMetadata' // External ID of the VM to update
def apiHost = 'https://jnj-im-dev-na.it-cpi019.cfapps.us10-002.hana.ondemand.com'
def tokenUrl = 'https://jnj-im-dev-na.authentication.us10.hana.ondemand.com/oauth/token'

def newSourceValue = 'apiCreatedKey'
def newTargetValue = 'apiCreatedValue'

def getAccessToken(tokenUrl,clientId,clientSecret) {
    def url = new URL(tokenUrl)
    def conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = 'POST'
    def authString = "${clientId}:${clientSecret}".bytes.encodeBase64().toString()
    conn.setRequestProperty('Authorization', "Basic ${authString}")
    conn.doOutput = true
    def body = 'grant_type=client_credentials'
    conn.outputStream.withWriter { it << body }
    def resp = conn.inputStream.getText('UTF-8')
    def json = new JsonSlurper().parseText(resp)
    return json.access_token
}

def getValueMapping(token, valueMappingId, apiHost = 'https://jnj-im-dev-na.it-cpi019.cfapps.us10-002.hana.ondemand.com') {
    def url = new URL("${apiHost}/api/v1/ValueMappings('${valueMappingId}')")
    def conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty('Authorization', "Bearer ${token}")
    conn.setRequestProperty('Accept', 'application/json')
    conn.connect()
    def resp = conn.inputStream.getText('UTF-8')
    return new JsonSlurper().parseText(resp)
}

def updateValueMapping(token, valueMappingId, updatedPayload) {
    def url = new URL("${apiHost}/api/v1/ValueMappings('${valueMappingId}')")
    def conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = 'PUT'
    conn.setRequestProperty('Authorization', "Bearer ${token}")
    conn.setRequestProperty('Content-Type', 'application/json')
    conn.doOutput = true
    conn.outputStream.withWriter('UTF-8') { it << JsonOutput.toJson(updatedPayload) }
    def resp = conn.inputStream.getText('UTF-8')
    return resp
}

// --- MAIN LOGIC ---
def token = getAccessToken(tokenUrl,clientId,clientSecret)
def vm = getValueMapping(token, valueMappingId)

// Add new entry (adjust this logic for your VM structure)
def newEntry = [
    sourceValue: newSourceValue,
    targetValue: newTargetValue,
    // add other fields as required by your VM structure
]

// This assumes your VM has an 'entries' array or similar; adjust as needed
if (!vm.entries) vm.entries = []
vm.entries << newEntry

def result = updateValueMapping(token, valueMappingId, vm)
println "Update result: $result"

// --- END ---

/*
Instructions:
1. Fill in sb-b34800d5-d3a6-4f59-90f0-4beffa8cb8af!b342912|it!b56186, 1c98c445-2c3d-433c-9148-f7e5e265d509$AUgkNVe6o9X0prGm1bgRt6b0e4krtTRhWmMBrrmBPP8=, and VM_Dev_FrameworkMetadata.
2. Adjust newSourceValue/newTargetValue and the entry structure as needed.
3. Run this script in Groovy Console or adapt for SAP CPI.
4. If your VM structure differs (e.g., nested groups), adjust the entry insertion logic accordingly.
*/

// --- END LEGACY LOGIC ---

// --- YAML-driven Value Map Upsert Example ---
// (see below for active logic)
@Grab('org.yaml:snakeyaml:2.0')
import org.yaml.snakeyaml.Yaml
import java.util.UUID

def getAccessToken(tokenurl, clientid, clientsecret) {
    def url = new URL(tokenurl)
    def conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = 'POST'
    def authString = "${clientid}:${clientsecret}".bytes.encodeBase64().toString()
    conn.setRequestProperty('Authorization', "Basic ${authString}")
    conn.doOutput = true
    def body = 'grant_type=client_credentials'
    conn.outputStream.withWriter { it << body }
    def resp = conn.inputStream.getText('UTF-8')
    def json = new groovy.json.JsonSlurper().parseText(resp)
    return json.access_token
}



def updateValueMappingParameters(url, accessToken, paramValue) {
    def payload = groovy.json.JsonOutput.toJson([ ParameterValue: paramValue ])
    def connection = new URL(url).openConnection() as HttpURLConnection
    connection.setRequestMethod('POST')
    connection.setDoOutput(true)
    connection.setRequestProperty('Content-Type', 'application/json')
    connection.setRequestProperty('Accept', 'application/json')
    connection.setRequestProperty('Authorization', "Bearer ${accessToken}")
    connection.outputStream.withWriter { it << payload }
    def responseCode = connection.responseCode
    String response
    try {
        response = connection.inputStream.text
    } catch (IOException e) {
        response = connection.errorStream?.text
    }
    println "[YAML Upsert] Response code: ${responseCode}"
    println "[YAML Upsert] Response body: ${response?.take(2000)}"
    connection.disconnect()
}

// Helper to wrap non-boolean values in single quotes for URL params
String wrapForUrl(val) {
    if (val instanceof Boolean) {
        return val.toString()
    }
    return "'${val}'"
}

def run = {
def yamlFile = './Resources/dev_valueMappings.yaml' // Update as needed
// def cpiHost = 'your-cpi-host.example.com' // Update as needed
// def credentialsId = 'your-credentials-id'  // Update as needed
def jsonKey = new File("./.env.im-pq.json")
def sk = new groovy.json.JsonSlurper().parseText(jsonKey.getText("UTF-8"))
def clientid = sk.oauth.clientid
def clientsecret = sk.oauth.clientsecret
def url = sk.oauth.url
def tokenurl = sk.oauth.tokenurl
// def clientid = ''
// def clientsecret = ''
// def valueMappingId = 'VM_Dev_FrameworkMetadata' // External ID of the VM to update
// def url = 'https://jnj-im-dev-na.it-cpi019.cfapps.us10-002.hana.ondemand.com'
// def tokenurl = 'https://jnj-im-dev-na.authentication.us10.hana.ondemand.com/oauth/token'
def accessToken = getAccessToken(tokenurl, clientid, clientsecret)
def yaml = new Yaml()
def envParams = yaml.load(new File(yamlFile).text)
envParams.each { valueMapsID ->
    println "[YAML Upsert] envParams:valueMapsID::: ${valueMapsID.key}"
    valueMapsID.value.each { valueMapSchema ->
        println "[YAML Upsert] valueMapSchema::valueMapSchema:: ${valueMapSchema}"
        def UrlParams = ''
        def valueMapSchemaValues = valueMapSchema.value as Map
        valueMapSchemaValues.each { key, value ->
            if (key == 'IsConfigured') {
                UrlParams = "IsConfigured=${value}"
            } else if (key == 'SrcId') {
                UrlParams += "&SrcId=${wrapForUrl(value)}"
            } else if (key == 'SrcAgency') {
                UrlParams += "&SrcAgency=${wrapForUrl(value)}"
            } else if (key == 'TgtId') {
                UrlParams += "&TgtId=${wrapForUrl(value)}"
            } else if (key == 'TgtAgency') {
                UrlParams += "&TgtAgency=${wrapForUrl(value)}"
            } else if (key == 'Version') {
                UrlParams += "&Version=${wrapForUrl(value)}"
            } else if (key == 'valueMaps') {
                value.each { valMaps ->
                    def valueMapValues = valMaps.value as Map
                    valueMapValues.each { mapKey, mapValue ->
                        def valMapId = valMaps.key == 'new' ? UUID.randomUUID().toString() : valMaps.key
                        def finalUrl = "${url}/api/v1/UpsertValMaps?Id=${wrapForUrl(valueMapsID.key)}&${UrlParams}&SrcValue=${wrapForUrl(mapKey)}&TgtValue=${wrapForUrl(mapValue)}"
                        println "[YAML Upsert] Final URL: ${finalUrl}"
                        // updateValueMappingParameters(finalUrl, accessToken, mapValue)
                    }
                }
            }
        }
    }
}
}
run()

// --- END YAML-driven Example ---