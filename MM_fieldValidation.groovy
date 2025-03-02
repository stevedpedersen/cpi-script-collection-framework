import com.sap.it.api.mapping.*

def String fieldValidation(String fieldValue, String fieldLength, String fieldXpath, String mandatory) {

    String xPath = fieldXpath != null && !fieldXpath.trim().isEmpty() ? fieldXpath + " " : "";
    
    // Check if the field value is not blank
    if (fieldValue != null && !fieldValue.trim().isEmpty()) {
        if (!fieldLength.equals("NA")) {
            if (fieldValue.length() > Integer.parseInt(fieldLength)) {
                return "Mandatory Field " + xPath + "exceeds the required length";
            }
        }
    } else {
        if (mandatory.equals("X")) {
            return "Mandatory Field " + xPath + "is blank";
        }
    }

    return fieldValue
}