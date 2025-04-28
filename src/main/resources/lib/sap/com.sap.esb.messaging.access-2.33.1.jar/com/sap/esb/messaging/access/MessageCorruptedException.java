package com.sap.esb.messaging.access;

public class MessageCorruptedException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public String getMessage() {
      return "The message is incomplete. Message chunks are missing";
   }
}
