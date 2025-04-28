package com.sap.esb.messaging.access;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class NoMessageReceivedException extends Exception {
   private static final long serialVersionUID = 1L;

   public NoMessageReceivedException() {
      super("No Message was received within the given timeout");
   }
}
