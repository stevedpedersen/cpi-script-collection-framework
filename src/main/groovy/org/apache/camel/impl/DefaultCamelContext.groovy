package org.apache.camel.impl
class DefaultCamelContext {
    // Exchange exchange
    Object body
    Map<String, Object> headers
    Map<String, Object> properties
    String version
    String uptime

    DefaultCamelContext() {
        this.body = ""
        this.headers = [:]
        this.properties = [:]
        this.version = "3.14.7"
        this.uptime = "forever"
    }

    public Object getBody() {
        return this.body ?: null
    }
    public Object getBody(Class type) {
        // Only String supported for now; extend as needed
        if(type == String) return this.body as String
        return this.body
    }

    void setBody(Object body) {
        this.body = body
    }

    public Object getHeader(String name) {
        return this.headers.get(name)
    }

    void setHeader(String name, Object value) {
        this.headers.put(name, value)
    }

    void setProperty(String name, Object value) {
        this.properties.put(name, value)
    }

    Object getProperty(String name) {
        return this.properties.get(name)
    }
}