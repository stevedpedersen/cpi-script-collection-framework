package com.sap.esb.messaging.access;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class MessageInconsistentException extends Exception {
   private static final long serialVersionUID = 1L;

   public MessageInconsistentException(String message) {
      super(message);
   }
}
