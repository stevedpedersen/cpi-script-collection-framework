package com.sap.esb.messaging.access;

import com.sap.esb.messaging.solace.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.SDTException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MessageAccess {
   String JMS_MESSAGE_ID = "JMSMessageID";
   String JMS_PIECE_ID = "CamelJmsChunkCollectionId";
   String JMS_COUNT = "CamelJmsCount";
   String JMS_HEAD = "CamelJmsHead";
   String JMS_COUNTER = "CamelJmsCounter";
   String JMS_CHUNK_QUEUE = "CamelJmsChunkQueue";
   /** @deprecated */
   @Deprecated
   String HEAD_SELECTOR = "CamelJmsHead = 'CamelJmsHead'";
   long HEAD_RECEIVE_TIMEOUT = 1000L;
   String CHUNK_QUEUE_MARKER = "C.";
   String ERROR_QUEUE_MARKER = "E.";

   QueueBrowser browseMessages(Session var1, String var2, String var3) throws JMSException;

   Enumeration<byte[]> downloadMessage(Session var1, String var2, String var3) throws JMSException, IOException;

   void moveMessage(Session var1, String var2, String var3, String var4, PropertiesChange var5) throws JMSException;

   void moveMessage(Session var1, String var2, String var3, List<String> var4, PropertiesChange var5) throws JMSException, InvalidInputException;

   boolean moveMessageCheck(Session var1, String var2, String var3, List<String> var4, PropertiesChange var5) throws JMSException, InvalidInputException;

   void consumeMessage(Session var1, String var2, List<String> var3) throws JMSException, NoMessageReceivedException;

   Map<String, String> consumeMessageRef(Session var1, String var2, List<String> var3) throws JMSException, NoMessageReceivedException;

   Map<String, String> jcsmpDeleteRef(JCSMPSession var1, String var2, List<String> var3) throws SDTException, JCSMPException, JMSException;

   void deleteQueue(Session var1, String var2) throws JMSException, NoMessageReceivedException;

   Map<String, String> deleteQueueRef(Session var1, String var2) throws JMSException, NoMessageReceivedException;

   long getTimeout();

   void setTimeout(long var1);

   List<String> getOverdueMessages(Session var1, String var2) throws JMSException;

   boolean moveCompleteMessages(Session var1, String var2, String var3, String var4, PropertiesChange var5) throws JMSException, MessageInconsistentException;

   Map<String, String> getAllRefsForQueue(Session var1, String var2) throws JMSException;
}
