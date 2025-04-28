package com.sap.esb.messaging.access;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class InvalidInputException extends Exception {
   private static final long serialVersionUID = 1L;

   public InvalidInputException(String message) {
      super(message);
   }
}
