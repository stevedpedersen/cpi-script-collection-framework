import com.sap.gateway.ip.core.customdev.util.Message
import src.main.resources.script.Framework_Logger
import src.main.resources.script.Framework_Validator

/**
 * By default, validation failures will cause an exception to be thrown.
 * No Function Name required.
 */
def Message processData(Message message) {
    return validateFields(message, true)
}

/**
 * Use Function Name "withoutException" to validate without throwing an exception.
 * The validationErrorMessage property will be set unless validation passes.
 */
def Message withoutException(Message message) {
    return validateFields(message, false)
}

/**
 * Validates fields and conditionally throws an exception for failures.
 */
def Message validateFields(Message message, boolean throwException) {
    def validator = new Framework_Validator(message, messageLogFactory.getMessageLog(message))
    validator.assertPayloadValid(throwException)
    return message
}

/* The below code can be copied to create your own Message Map function for payload validation by uploading it as a .groovy file. */
/*
import com.sap.it.api.mapping.*

def String fieldValidation(String fieldValue, String fieldLength, String fieldXpath, String mandatory) {
    // Initialize validation result
    String validationResult

    // Trim the field value to remove leading/trailing spaces
    fieldValue = fieldValue?.trim()

    // Check if the field value is not blank
    if (fieldValue) {
        if (fieldLength != "NA") {
            // Check if the field value length is within the provided limit
            if (fieldValue.length() <= Integer.parseInt(fieldLength)) {
                validationResult = fieldValue
            } else {
                // Field value length exceeds the provided limit
                validationResult = "Mandatory Field ${fieldXpath} exceeds the required length"
            }
        } else {
            // No length validation required
            validationResult = fieldValue
        }
    } else {
        // Field value is blank
        if (mandatory == "X") {
            validationResult = "Mandatory Field ${fieldXpath} is blank"
        } else {
            validationResult = fieldValue
        }
    }

    return validationResult
}

*/
