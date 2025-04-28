package ilcd.validation;

import com.sap.aii.mapping.api.*;
import com.sap.aii.mapping.lookup.*;
import com.sap.aii.mappingtool.tf7.rt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import com.sap.aii.mappingtool.functionlibrary.*;

public class ValidateField extends AFunctionLibrary {

    @Init
    public void init(GlobalContainer container) throws StreamTransformationException {
    }

    @CleanUp
    public void cleanUp(GlobalContainer container) throws StreamTransformationException {
    }

    //The key property of the UDFs should not be changed. If it is changed it cannot be used
    //in the Message Mappings which are imported from ESR system.
    @FunctionLibraryMethod(category = "ValidateField", title = "FieldValidationTitle", executionType = "SINGLE_VALUE", key = "b07a3130-38c6-4f3b-a723-82860a16c433")
    public String fieldValidation(
        @UDFParam(paramCategory = "Argument", title = "FieldValue") String fieldValue, 
        @UDFParam(paramCategory = "Argument", title = "FieldLength") String fieldLength,
        @UDFParam(paramCategory = "Argument", title = "FieldXpath") String fieldXpath,
        @UDFParam(paramCategory = "Argument", title = "Mandatory") String mandatory,
        Container container) throws StreamTransformationException {

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

        return fieldValue;
    }
}
