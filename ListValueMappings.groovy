import groovy.json.JsonSlurper
import java.net.HttpURLConnection

def clientId = ''
def clientSecret = ''
def apiHost = 'https://jnj-im-dev-na.it-cpi019.cfapps.us10-002.hana.ondemand.com'
def tokenUrl = 'https://jnj-im-dev-na.authentication.us10.hana.ondemand.com/oauth/token'

def getAccessToken(tokenUrl, clientId, clientSecret) {
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

def listValueMappings(token, apiHost) {
    def url = new URL("${apiHost}/api/v1/ValueMappingDesigntimeArtifacts")
    def conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty('Authorization', "Bearer ${token}")
    conn.setRequestProperty('Accept', 'application/xml')
    conn.connect()
    def resp = conn.inputStream.getText('UTF-8')
    println "--- Value Mappings XML ---\n${resp}"
}

// --- MAIN ---
def token = getAccessToken(tokenUrl, clientId, clientSecret)
listValueMappings(token, apiHost)
