package com.sap.esb.messaging.access;

import java.util.List;
import javax.jms.JMSException;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MessageRetry {
   String ERROR_QUEUE_MARKER = "E.";
   String SAP_RETYAT = "SAPJMSRetryAt";
   String SAP_RETRIES = "SAPJMSRetries";

   void retry() throws JMSException;

   void retryIds(String var1, List<String> var2) throws JMSException, InvalidInputException;

   boolean retryIdsCheck(String var1, List<String> var2) throws JMSException, InvalidInputException;

   void retryQueue(String var1) throws JMSException, InvalidInputException;

   void shutDown();
}
