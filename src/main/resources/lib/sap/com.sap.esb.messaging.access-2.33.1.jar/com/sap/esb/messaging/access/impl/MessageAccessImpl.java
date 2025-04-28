package com.sap.esb.messaging.access.impl;

import com.sap.esb.messaging.access.ChunkMessageHelper;
import com.sap.esb.messaging.access.DownloadException;
import com.sap.esb.messaging.access.InvalidInputException;
import com.sap.esb.messaging.access.MessageAccess;
import com.sap.esb.messaging.access.MessageCorruptedException;
import com.sap.esb.messaging.access.MessageInconsistentException;
import com.sap.esb.messaging.access.NoMessageReceivedException;
import com.sap.esb.messaging.access.PropertiesChange;
import com.sap.esb.messaging.access.QueueBrowserException;
import com.sap.esb.messaging.solace.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MessageAccessImpl implements MessageAccess {
   private static final String SAP_PREGENERATED_MPL_ID = "SAP_PregeneratedMplId";
   private static final String SAP_MESSAGE_PROCESSING_LOG_ID = "SAP_MessageProcessingLogID";
   private static final String SAP_PROPERTY = "SAPProperty_";
   private static final Logger LOG = LoggerFactory.getLogger(MessageAccessImpl.class);
   private static final long QUERY_TIMEOUT = 700L;
   public static final String JMS_MESSAGE_ID = "JMSMessageID";
   public static final String JMS_PIECE_ID = "CamelJmsChunkCollectionId";
   public static final String JMS_COUNT = "CamelJmsCount";
   public static final String JMS_HEAD = "CamelJmsHead";
   public static final String JMS_TYPE = "JMSType";
   public static final String JMS_COUNTER = "CamelJmsCounter";
   private static final String ALERT_TIME = "SAPAlertTime";
   public static final String JMS_CHUNK_QUEUE = "CamelJmsChunkQueue";
   public static final String DEAD_LETTER = "(JMSType <> 'DL' OR JMSType IS NULL)";
   public static final int DEFAULT_HEAD_RECEIVE_TIMEOUT = 5000;
   public static final String HEAD_RECEIVE_TIMEOUT_PROPERTY = "com.sap.it.messaging.jms.retry.receive.timeout";
   private int headReceiveTimeout;
   private static long timeout = 10000L;

   public MessageAccessImpl() {
      String headReceiveTimeoutProperty = System.getProperty("com.sap.it.messaging.jms.retry.receive.timeout");
      if (headReceiveTimeoutProperty != null) {
         try {
            this.headReceiveTimeout = Integer.decode(headReceiveTimeoutProperty);
         } catch (NumberFormatException var3) {
            this.headReceiveTimeout = 5000;
         }
      } else {
         this.headReceiveTimeout = 5000;
      }

   }

   public QueueBrowser browseMessages(Session session, String queue, String selector) throws JMSException {
      return session.createBrowser(session.createQueue(queue), selector);
   }

   public Enumeration<byte[]> downloadMessage(Session session, String queue, String id) throws JMSException, IOException {
      return new MessageAccessImpl.MessageDownload(session, queue, id);
   }

   public void moveMessage(Session session, String fromQueue, String toQueue, String fromSelector, PropertiesChange propChange) throws JMSException {
      if (fromSelector != null && !fromSelector.isEmpty()) {
         fromSelector = "(" + fromSelector + ") AND " + "(JMSType <> 'DL' OR JMSType IS NULL)";
      } else {
         fromSelector = "(JMSType <> 'DL' OR JMSType IS NULL)";
      }

      this.moveMessages(session, fromQueue, toQueue, fromSelector, propChange);
   }

   private boolean moveMessages(Session session, String fromQueue, String toQueue, String fromSelector, PropertiesChange propChange) throws JMSException {
      MessageConsumer consumer = null;
      MessageProducer producer = null;
      boolean messageFound = false;

      boolean var18;
      try {
         try {
            consumer = session.createConsumer(session.createQueue(fromQueue), fromSelector);
            producer = session.createProducer(session.createQueue(toQueue));
            BytesMessage message = null;

            do {
               message = (BytesMessage)consumer.receive((long)this.headReceiveTimeout);
               if (message != null) {
                  messageFound = true;
                  LOG.info(MessageFormat.format("Message with ID {0} and retry timestamp {1} is retried", message.getJMSMessageID(), message.getStringProperty("SAPJMSRetryAt")));
                  BytesMessage newMessage = session.createBytesMessage();
                  Util.copyBytesBody(message, newMessage);
                  Util.setMessageProperties(newMessage, Util.getMessageProperties(message));
                  if (propChange != null) {
                     newMessage = (BytesMessage)propChange.change(newMessage);
                     LOG.debug("JMS message ID in error queue" + newMessage.getJMSMessageID());
                  }

                  long ttl = message.getJMSExpiration() - System.currentTimeMillis();
                  producer.send(newMessage, producer.getDeliveryMode(), producer.getPriority(), ttl > 0L ? ttl : 0L);
                  LOG.info(MessageFormat.format("New JMS message ID {0}, MPL ID {1}", newMessage.getJMSMessageID(), this.getMplId((Message)newMessage)));
                  session.commit();
               }
            } while(message != null);
         } catch (Exception var16) {
            LOG.error("error during retry processing. Message is rolled back and processed next time again", var16);
            session.rollback();
         }

         var18 = messageFound;
      } finally {
         this.tryClose(producer);
         this.tryClose(consumer);
      }

      return var18;
   }

   public void moveMessage(Session session, String fromQueue, String toQueue, List<String> ids, PropertiesChange propChange) throws JMSException, InvalidInputException {
      this.moveMessageCheck(session, fromQueue, toQueue, ids, propChange);
   }

   public void consumeMessage(Session session, String queue, List<String> ids) throws JMSException, NoMessageReceivedException {
      this.consumeMessageRef(session, queue, ids);
   }

   public void deleteQueue(Session session, String queueName) throws JMSException, NoMessageReceivedException {
      this.deleteQueueRef(session, queueName);
   }

   private void tryClose(MessageConsumer consumer) {
      if (consumer != null) {
         try {
            consumer.close();
         } catch (Exception var3) {
            LOG.error("unable to close consumer", var3);
         }
      }

   }

   private void tryClose(QueueBrowser browser) {
      if (browser != null) {
         try {
            browser.close();
         } catch (Exception var3) {
            LOG.error("unable to close queue browser", var3);
         }
      }

   }

   private void tryClose(MessageProducer producer) {
      if (producer != null) {
         try {
            producer.close();
         } catch (Exception var3) {
            LOG.error("unable to close producer", var3);
         }
      }

   }

   public long getTimeout() {
      return timeout;
   }

   public void setTimeout(long timeout) {
      MessageAccessImpl.timeout = timeout;
   }

   public List<String> getOverdueMessages(Session session, String queue) throws JMSException {
      long terminateAt = System.currentTimeMillis() + 700L;
      QueueBrowser browser = this.browseMessages(session, queue, "SAPAlertTime<" + System.currentTimeMillis());

      try {
         Enumeration<Message> messages = browser.getEnumeration();
         ArrayList messageIDs = new ArrayList();

         while(hasMoreElements(messages, terminateAt)) {
            messageIDs.add(((Message)messages.nextElement()).getJMSMessageID());
         }

         ArrayList var8 = messageIDs;
         return var8;
      } finally {
         browser.close();
      }
   }

   private void waitBeforeBrowse() {
      try {
         Thread.sleep(700L);
      } catch (InterruptedException var2) {
         LOG.error("interrupted exception", var2);
         Thread.currentThread().interrupt();
      }

   }

   private static boolean hasMoreElements(Enumeration<?> enumeration, long terminateAt) {
      boolean ret;
      for(ret = enumeration.hasMoreElements(); !ret && System.currentTimeMillis() < terminateAt; ret = enumeration.hasMoreElements()) {
         try {
            Thread.sleep(20L);
         } catch (InterruptedException var5) {
            LOG.error("interrupted exception", var5);
            Thread.currentThread().interrupt();
         }
      }

      return ret;
   }

   public boolean moveMessageCheck(Session session, String fromQueue, String toQueue, List<String> ids, PropertiesChange propChange) throws JMSException, InvalidInputException {
      int n = false;
      boolean messageFound = false;
      if (ids != null && !ids.isEmpty()) {
         int n = ids.size();

         String fromSelector;
         for(Iterator var8 = ids.iterator(); var8.hasNext(); messageFound = this.moveMessages(session, fromQueue, toQueue, fromSelector, propChange)) {
            String id = (String)var8.next();
            fromSelector = "JMSMessageID = '" + id + "'";
         }

         return n != 1 || messageFound;
      } else {
         throw new InvalidInputException("No JMS identifiers set");
      }
   }

   public boolean moveCompleteMessages(Session session, String fromQueue, String toQueue, String fromSelector, PropertiesChange propChange) throws JMSException, MessageInconsistentException {
      MessageConsumer consumer = null;
      MessageProducer producer = null;
      boolean messageFound = false;

      try {
         consumer = session.createConsumer(session.createQueue(fromQueue), fromSelector);
         producer = session.createProducer(session.createQueue(toQueue));
         BytesMessage message = null;

         do {
            message = (BytesMessage)consumer.receive(timeout);
            if (message != null) {
               messageFound = true;
               BytesMessage newMessage = session.createBytesMessage();
               String chunkId = message.getStringProperty("CamelJmsChunkCollectionId");
               int count = 0;
               String chunkQueueOld = null;
               if (chunkId != null) {
                  count = message.getIntProperty("CamelJmsCount");
                  chunkQueueOld = message.getStringProperty("CamelJmsChunkQueue");
               }

               Util.copyBytesBody(message, newMessage);
               Util.setMessageProperties(newMessage, Util.getMessageProperties(message));
               if (propChange != null) {
                  newMessage = (BytesMessage)propChange.change(newMessage);
               }

               if (chunkId != null) {
                  String newChunkQueue;
                  if (fromQueue.startsWith("E.")) {
                     newChunkQueue = "C." + toQueue.substring(2);
                  } else {
                     newChunkQueue = "C." + toQueue;
                  }

                  newMessage.setStringProperty("CamelJmsChunkQueue", newChunkQueue);
                  MessageConsumer chunkConsumer = null;
                  MessageProducer chunkProducer = null;

                  try {
                     chunkConsumer = session.createConsumer(session.createQueue(chunkQueueOld), "CamelJmsChunkCollectionId = '" + chunkId + "'");
                     chunkProducer = session.createProducer(session.createQueue(newChunkQueue));

                     for(int i = 0; i < count; ++i) {
                        BytesMessage chunkMessage = (BytesMessage)chunkConsumer.receive(timeout);
                        if (chunkMessage == null) {
                           throw new MessageInconsistentException("chunk part " + i + " of chunk id " + chunkId + " in queue " + chunkQueueOld + " missing");
                        }

                        BytesMessage newChunk = session.createBytesMessage();
                        Util.copyBytesBody(chunkMessage, newChunk);
                        Util.setMessageProperties(newChunk, Util.getMessageProperties(chunkMessage));
                        long ttl = chunkMessage.getJMSExpiration() - System.currentTimeMillis();
                        chunkProducer.send(newChunk, producer.getDeliveryMode(), producer.getPriority(), ttl > 0L ? ttl : 0L);
                     }
                  } finally {
                     this.tryClose(chunkConsumer);
                     this.tryClose(chunkProducer);
                  }
               }

               long ttl = message.getJMSExpiration() - System.currentTimeMillis();
               producer.send(newMessage, producer.getDeliveryMode(), producer.getPriority(), ttl > 0L ? ttl : 0L);
               session.commit();
            }
         } while(message != null);

         boolean var30 = messageFound;
         return var30;
      } finally {
         this.tryClose(producer);
         this.tryClose(consumer);
      }
   }

   public Map<String, String> consumeMessageRef(Session session, String queue, List<String> ids) throws JMSException, NoMessageReceivedException {
      Map<String, String> ref = new HashMap();
      Iterator var5 = ids.iterator();

      while(var5.hasNext()) {
         String id = (String)var5.next();
         MessageConsumer consumer = null;
         MessageConsumer subConsumer = null;

         try {
            consumer = session.createConsumer(session.createQueue(queue), "JMSMessageID = '" + id + "'");
            Message message = consumer.receive(timeout);
            if (message == null) {
               LOG.error("Consume head message to delete message: No Message was received within the given timeout " + timeout);
            } else {
               String chunkId = message.getStringProperty("CamelJmsChunkCollectionId");
               String chunkQueue;
               if (chunkId != null) {
                  chunkQueue = message.getStringProperty("CamelJmsChunkQueue");
                  if (chunkQueue != null) {
                     Queue subQueue = session.createQueue(chunkQueue);
                     subConsumer = session.createConsumer(subQueue, "CamelJmsChunkCollectionId = '" + chunkId + "'");

                     for(int i = 0; i < message.getIntProperty("CamelJmsCount"); ++i) {
                        if (subConsumer.receive(timeout) == null) {
                           LOG.error("Consume sub message to delete message: No Message was received within the given timeout " + timeout);
                        }
                     }
                  }
               }

               chunkQueue = this.getMplId(message);
               ref.put(id, chunkQueue);
               session.commit();
            }
         } catch (JMSException var17) {
            LOG.error("Error during consuming the message", var17);
            throw var17;
         } finally {
            this.tryClose(consumer);
            this.tryClose(subConsumer);
         }
      }

      return ref;
   }

   public Map<String, String> deleteQueueRef(Session session, String queueName) throws JMSException, NoMessageReceivedException {
      Object browser = null;

      Map var6;
      try {
         Map<String, String> refs = null;
         List<String> ids = this.getAllJmsIds(session, queueName);
         if (!ids.isEmpty()) {
            refs = this.consumeMessageRef(session, queueName, ids);
         }

         var6 = refs;
      } finally {
         if (browser != null) {
            ((QueueBrowser)browser).close();
         }

      }

      return var6;
   }

   private List<String> getAllJmsIds(Session session, String queueName) throws JMSException {
      QueueBrowser browser = this.browseMessages(session, queueName, "");
      long terminateAt = System.currentTimeMillis() + 700L;
      Enumeration<Message> messages = browser.getEnumeration();
      ArrayList ids = new ArrayList();

      while(hasMoreElements(messages, terminateAt)) {
         ids.add(((Message)messages.nextElement()).getJMSMessageID());
      }

      return ids;
   }

   public Map<String, String> getAllRefsForQueue(Session session, String queueName) throws JMSException {
      QueueBrowser browser = this.browseMessages(session, queueName, "");
      long terminateAt = System.currentTimeMillis() + 700L;
      Enumeration<Message> messages = browser.getEnumeration();
      HashMap refs = new HashMap();

      while(hasMoreElements(messages, terminateAt)) {
         Message message = (Message)messages.nextElement();
         String mplId = this.getPregenMplId(message);
         refs.put(message.getJMSMessageID(), mplId);
      }

      return refs;
   }

   private String getMplId(Message message) throws JMSException {
      Enumeration<String> propNames = message.getPropertyNames();

      String propName;
      for(propName = null; propNames.hasMoreElements(); propName = null) {
         propName = (String)propNames.nextElement();
         if (propName.toUpperCase(Locale.ENGLISH).equals("SAP_MessageProcessingLogID".toUpperCase(Locale.ENGLISH))) {
            break;
         }
      }

      String mplId = null;
      if (propName != null) {
         mplId = message.getStringProperty(propName);
      }

      if (mplId == null) {
         mplId = message.getStringProperty("SAPProperty_SAP_MessageProcessingLogID");
      }

      return mplId;
   }

   private String getPregenMplId(Message message) throws JMSException {
      Enumeration<String> propNames = message.getPropertyNames();

      String propName;
      for(propName = null; propNames.hasMoreElements(); propName = null) {
         propName = (String)propNames.nextElement();
         if ("SAP_PregeneratedMplId".toUpperCase(Locale.ENGLISH).equals(propName.toUpperCase(Locale.ENGLISH))) {
            break;
         }
      }

      String mplId = null;
      if (propName != null) {
         mplId = message.getStringProperty(propName);
      }

      return mplId;
   }

   private String getMplId(BytesXMLMessage message) throws SDTException {
      SDTMap propNames = message.getProperties();
      String propName = null;

      for(Iterator var4 = propNames.keySet().iterator(); var4.hasNext(); propName = null) {
         String prop = (String)var4.next();
         propName = prop;
         if ("SAP_PregeneratedMplId".toUpperCase(Locale.ENGLISH).equals(prop.toUpperCase(Locale.ENGLISH))) {
            break;
         }
      }

      String mplId = null;
      if (propName != null) {
         mplId = propNames.getString(propName);
      }

      return mplId;
   }

   public Map<String, String> jcsmpDeleteRef(JCSMPSession session, String queue, List<String> ids) throws JCSMPException {
      Map<String, String> idMap = new HashMap();
      Iterator var5 = ids.iterator();

      while(var5.hasNext()) {
         String id = (String)var5.next();
         BrowserProperties browserProps = new BrowserProperties();
         JCSMPFactory fac = JCSMPFactory.onlyInstance();
         com.solacesystems.jcsmp.Queue jcsmpQueue = fac.createQueue(queue);
         browserProps.setEndpoint(jcsmpQueue);
         String selector = "mi='" + id + "'";
         browserProps.setSelector(selector);
         Browser browser = session.createBrowser(browserProps);
         BytesXMLMessage message = browser.getNext();
         SDTMap msgProps = message.getProperties();
         String mplId = this.getMplId(message);
         String chunkId = msgProps.getString("CamelJmsChunkCollectionId");
         if (chunkId != null) {
            String chunkQueue = msgProps.getString("CamelJmsChunkQueue");
            BrowserProperties subBrowserProps = new BrowserProperties();
            com.solacesystems.jcsmp.Queue subQueue = fac.createQueue(chunkQueue);
            browserProps.setEndpoint(subQueue);
            String subSelector = "CamelJmsChunkCollectionId='" + chunkId + "'";
            subBrowserProps.setSelector(subSelector);
            Browser subBrowser = session.createBrowser(subBrowserProps);

            while(subBrowser.hasMore()) {
               BytesXMLMessage subMessage = browser.getNext();

               try {
                  subBrowser.remove(subMessage);
               } catch (JCSMPException var23) {
                  LOG.error("Unable to delete chunk message " + id + "from queue " + chunkQueue);
               }
            }
         }

         browser.remove(message);
         idMap.put(id, mplId);
      }

      return idMap;
   }

   public class MessageDownload implements Enumeration<byte[]>, ChunkMessageHelper {
      private int count;
      private QueueBrowser subBrowser;
      private Enumeration<BytesMessage> subMessages;
      private byte[] body;
      private int i;
      private int encryptionKeyId;
      private Map<String, Object> messageProperties = new HashMap();

      public MessageDownload(Session session, String queue, String id) throws JMSException, IOException {
         Queue jmsQueue = session.createQueue(queue);
         long terminateAt = System.currentTimeMillis() + 700L;
         QueueBrowser browser = session.createBrowser(jmsQueue, "JMSMessageID = '" + id + "'");
         Enumeration<BytesMessage> messages = browser.getEnumeration();
         BytesMessage message = null;
         if (MessageAccessImpl.hasMoreElements(messages, terminateAt)) {
            message = (BytesMessage)messages.nextElement();
            MessageAccessImpl.this.tryClose(browser);
            this.body = new byte[(int)message.getBodyLength()];
            message.readBytes(this.body);
            String chunkId = message.getStringProperty("CamelJmsChunkCollectionId");
            Enumeration propertyNames = message.getPropertyNames();

            while(propertyNames.hasMoreElements()) {
               String propertyName = (String)propertyNames.nextElement();
               String decodedPropertyName = Util.decodeKey(propertyName);
               if (!"SAPEncryptionKeyId".equalsIgnoreCase(decodedPropertyName)) {
                  this.messageProperties.put(decodedPropertyName, message.getObjectProperty(propertyName));
               }
            }

            this.encryptionKeyId = this.getIntProperty(message, "SAPEncryptionKeyId");
            if (chunkId != null) {
               Queue subQueue = session.createQueue(message.getStringProperty("CamelJmsChunkQueue"));
               this.subBrowser = session.createBrowser(subQueue, "CamelJmsChunkCollectionId = '" + chunkId + "'");
               this.subMessages = this.subBrowser.getEnumeration();
               MessageAccessImpl.this.waitBeforeBrowse();
               this.count = message.getIntProperty("CamelJmsCount");
            } else {
               this.count = 0;
            }

         } else {
            MessageAccessImpl.this.tryClose(browser);
            throw new DownloadException(new QueueBrowserException());
         }
      }

      private int getIntProperty(BytesMessage message, String name) throws JMSException {
         Object obj = message.getObjectProperty(name);
         if (obj == null) {
            return 0;
         } else {
            return obj instanceof Integer ? (Integer)obj : message.getIntProperty(name);
         }
      }

      public boolean hasMoreElements() {
         return this.body != null;
      }

      public byte[] nextElement() {
         byte[] result = this.body;
         if (this.i < this.count) {
            BytesMessage subMessage = null;

            try {
               if (!this.hasMoreElements()) {
                  throw new NoMessageReceivedException();
               }

               try {
                  subMessage = (BytesMessage)this.subMessages.nextElement();
               } catch (NoSuchElementException var4) {
                  throw new MessageCorruptedException();
               }

               this.body = new byte[(int)subMessage.getBodyLength()];
               subMessage.readBytes(this.body);
            } catch (NoMessageReceivedException | JMSException var5) {
               throw new DownloadException(var5);
            }

            ++this.i;
         } else {
            this.body = null;
            MessageAccessImpl.this.tryClose(this.subBrowser);
         }

         return result;
      }

      public int getEncryptionKeyId() {
         return this.encryptionKeyId;
      }

      public Map<String, Object> getMessageProperties() {
         return this.messageProperties;
      }

      protected void finalize() throws Throwable {
         MessageAccessImpl.this.tryClose(this.subBrowser);
         super.finalize();
      }
   }
}
