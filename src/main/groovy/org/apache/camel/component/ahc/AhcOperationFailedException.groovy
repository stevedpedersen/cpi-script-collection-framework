package org.apache.camel.component.ahc
import java.lang.Throwable
class AhcOperationFailedException extends Throwable {
    public String message
    public java.lang.Throwable cause
    public int statusCode
    public String requestUri
    AhcOperationFailedException(String message) {
        super(message)
        this.message = message
        this.cause = new Throwable(message)
        this.statusCode = 500
        this.requestUri = '/'
    }
    AhcOperationFailedException(java.lang.Throwable cause) {
        super(cause)
        this.cause = cause
        this.message = cause.message
        this.statusCode = 500
        this.requestUri = '/'
    }

    Throwable getCause() {
        return cause
    }
}