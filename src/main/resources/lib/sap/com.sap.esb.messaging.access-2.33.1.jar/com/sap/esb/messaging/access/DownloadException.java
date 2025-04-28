package com.sap.esb.messaging.access;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class DownloadException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public DownloadException(Throwable t) {
      super(t);
   }
}
