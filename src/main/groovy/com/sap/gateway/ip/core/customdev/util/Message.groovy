package com.sap.gateway.ip.core.customdev.util

import org.apache.camel.Exchange
import org.apache.camel.TypeConversionException
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange

class Message {

    Exchange exchange
    Object body
    Map<String, Object> headers
    Map<String, Object> properties

    Message() {
        def ctx = new DefaultCamelContext()
        this.exchange = new DefaultExchange(ctx)
        this.headers = [:]
        this.properties = [:]
    }

    public <K> K getBody(Class<K> klass) throws TypeConversionException {
        // FIX: Return from this.body if set, fallback to exchange.getIn().body
        if (this.body != null) {
            if (klass == String) {
                return this.body.toString()
            }
            return this.body as K
        }
        def inObj = this.exchange.getIn()
        if (klass == String) {
            return inObj.body?.toString() ?: null
        }
        return inObj.body as K ?: null
    }

    void setBody(Object body) {
        this.exchange.getIn().setBody(body)
        this.body = body
    }

    public <K> K getHeader(String name, Class<K> klass) throws TypeConversionException {
        def value = this.exchange.getIn().getHeader(name)
        if (!value) {
            return null
        } else {
            return value as K ?: null
        }
    }

    void setHeader(String name, Object value) {
        this.exchange.getIn().setHeader(name, value)
        this.headers.put(name, exchange.getIn().getHeader(name))
    }

    void setProperty(String name, Object value) {
        // Always use string keys for consistency
        this.properties.put(name?.toString(), value)
    }

    Object getProperty(String name) {
        // Always use string keys for consistency
        // println "[Message] key - ${name}, value - ${this.properties.get(name?.toString())}"
        return this.properties.get(name?.toString())
    }
}