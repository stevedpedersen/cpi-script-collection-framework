package org.apache.camel.impl
class DefaultExchange {
    // Exchange exchange
    Object body
    Map<String, Object> headers
    Map<String, Object> properties

    DefaultExchange() {
        this.body = ""
        this.headers = [:]
        this.properties = [:]
    }
    public getBody() {
        return this.body ?: null
    }

    void setBody(Object body) {
        this.body = body
    }
}