package org.apache.camel
import java.lang.Throwable
class TypeConversionException extends Throwable {
    private String message
    TypeConversionException(String message) {
        super(message)
        this.message = message
    }

    Throwable getCause() {
        return new Throwable(message)
    }
}