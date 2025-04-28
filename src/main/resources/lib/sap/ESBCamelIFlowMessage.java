package com.sap.esb.monitoring.messages;

import com.sap.it.commons.layering.SubsystemPart;
import com.sap.it.commons.logging.Message;

public enum ESBCamelIFlowMessage implements Message {
   ERROR("Integration flow failed."),
   EXCEPTION("{0}"),
   MULTI_EXCEPTION("The integration flow has multiple exceptions for the following contexts: {0}"),
   CAUSE("Cause: {0}"),
   INSTALLED("Integration flow is not started on purpose."),
   OK("Integration flow is started."),
   POLL_FAILED("Polling from {0} failed."),
   NO_MORE_POLLS("No more polls in schedule."),
   STARTING("Integration flow is starting. Please wait."),
   STOPPING("Integration flow is stopping. Please wait."),
   CAMEL_CONTEXT_NOT_STARTED("The CamelContext did not start. Please check the message monitoring for details."),
   TIMEOUT_CONTEXT("Integration flow has timed out. Probably a dependent service has not started."),
   TIMEOUT_START("Integration flow has not been started for {0} seconds. Try to restart the integration flow."),
   UNRESOLVED("Unresolved dependency: {0}"),
   WAITING("Integration flow is waiting. Please wait.");

   private final String text;

   private ESBCamelIFlowMessage(String txt) {
      this.text = txt;
   }

   public String getMessageId() {
      return this.name();
   }

   public SubsystemPart getSubsystemPart() {
      return ESBCamelSubystemPart.IFLOW;
   }

   public String getMessageText() {
      return this.text;
   }
}
