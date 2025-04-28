package com.sap.esb.messaging.access;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class QueueBrowserException extends Exception {
   private static final long serialVersionUID = 1L;

   public QueueBrowserException() {
      super("Error using the JMS QueueBrowser. No message found for the specified JMS message Id");
   }
}
