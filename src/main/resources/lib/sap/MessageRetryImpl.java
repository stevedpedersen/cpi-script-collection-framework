package com.sap.esb.messaging.jms.retry.impl;

import com.sap.esb.lock.management.LockException;
import com.sap.esb.lock.management.LockInfo;
import com.sap.esb.lock.management.LockKey;
import com.sap.esb.lock.management.LockManagement;
import com.sap.esb.messaging.access.InvalidInputException;
import com.sap.esb.messaging.access.MessageAccess;
import com.sap.esb.messaging.access.MessageRetry;
import com.sap.esb.messaging.access.PropertiesChange;
import com.sap.esb.messaging.jms.retry.queues.RetryQueues;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MessageRetryImpl implements MessageRetry, LockManagement {
   private static final String RETRY_THREAD_PROPERTY = "com.sap.it.messaging.jms.retry.thread";
   private static final String THREAD_COUNT_PROPERTY = "com.sap.it.messaging.jms.retry.thread.count";
   private static final int DEFAULT_THREAD_COUNT = 3;
   private static final String JMS_TIMESTAMP = "JMSTimestamp";
   private static final String JMS_TYPE_DL = "JMSType = 'DL'";
   private static final String JMS_DEAD_LETTER = "JmsDeadLetter";
   private static final Logger LOG = LoggerFactory.getLogger(MessageRetryImpl.class);
   private volatile ConnectionFactory connectionFactory;
   private RetryQueues queuesToRetry;
   private MessageAccess access;
   private boolean enableThreads;
   private ThreadPoolExecutor executor;
   private int threadCount;
   private boolean runJob;

   public MessageRetryImpl() {
      String index = System.getenv("CF_INSTANCE_INDEX");
      if (index != null && !index.equals("0") && !index.equals("1") && !index.equals("2")) {
         this.runJob = false;
      } else {
         this.runJob = true;
         String threadProperty = System.getProperty("com.sap.it.messaging.jms.retry.thread");
         this.enableThreads = Boolean.valueOf(threadProperty);
         String threadCountProperty = System.getProperty("com.sap.it.messaging.jms.retry.thread.count");
         if (threadCountProperty != null) {
            try {
               this.threadCount = Integer.decode(threadCountProperty);
            } catch (NumberFormatException var5) {
               this.threadCount = 3;
            }
         } else {
            this.threadCount = 3;
         }

         LOG.info("thread pool size: " + this.threadCount);
         this.executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(this.threadCount, new MessageRetryImpl.RetryThreadFactory());
      }

      LOG.info("job will be executed: " + this.runJob);
   }

   @Reference(
      cardinality = ReferenceCardinality.OPTIONAL,
      policy = ReferencePolicy.DYNAMIC,
      target = "(factoryType=default)"
   )
   public void setConnectionFactory(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
   }

   public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
      this.connectionFactory = null;
   }

   public void retry() throws JMSException {
      if (this.runJob) {
         LOG.debug("retry()");
         if (this.connectionFactory != null) {
            Connection connection = null;
            Session session = null;

            try {
               LOG.info("Starting move of messages from error queues back to processing queues");
               if (this.enableThreads) {
                  StartThreads startThreads = new StartThreads(this.queuesToRetry.getErrorQueues(), this);
                  startThreads.submitThreads();
               } else {
                  connection = this.connectionFactory.createConnection();
                  connection.start();
                  session = connection.createSession(true, 1);
                  Iterator var9 = this.queuesToRetry.getErrorQueues().iterator();

                  while(var9.hasNext()) {
                     String errorQueue = (String)var9.next();
                     LOG.info(MessageFormat.format("Processing error queue: {0}", errorQueue));
                     String fromSelector = "SAPJMSRetryAt < " + System.currentTimeMillis();
                     LOG.info("Using criterion " + fromSelector + " to select the messages");
                     this.access.moveMessage(session, errorQueue, getQueue(errorQueue), fromSelector, (PropertiesChange)null);
                  }
               }
            } finally {
               this.tryClose(session);
               if (connection != null) {
                  connection.close();
               }

            }
         } else {
            LOG.debug("no connection factory bound, do nothing");
         }
      }

   }

   public void retryIds(String errorQueue, List<String> ids) throws JMSException, InvalidInputException {
      this.retryIdsCheck(errorQueue, ids);
   }

   public void retryQueue(String errorQueue) throws JMSException, InvalidInputException {
      if (this.connectionFactory != null) {
         Connection connection = null;
         Session session = null;

         try {
            connection = this.connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true, 1);
            LOG.info("Starting move of messages from error queue back to processing queue");
            LOG.info(MessageFormat.format("Processing error queue: {0}", errorQueue));
            String fromSelector = "JMSTimestamp < " + System.currentTimeMillis();
            this.access.moveMessage(session, errorQueue, getQueue(errorQueue), fromSelector, (PropertiesChange)null);
         } finally {
            this.tryClose(session);
            if (connection != null) {
               connection.close();
            }

         }
      } else {
         LOG.debug("no connection factory bound, do nothing");
      }

   }

   private void tryClose(Session session) {
      if (session != null) {
         try {
            session.close();
         } catch (Exception var3) {
            LOG.error("cannot close session", var3);
         }
      }

   }

   private void tryClose(QueueBrowser browser) {
      if (browser != null) {
         try {
            browser.close();
         } catch (Exception var3) {
            LOG.error("cannot close browser", var3);
         }
      }

   }

   private static String getQueue(String errorQueue) {
      return errorQueue.substring("E.".length());
   }

   private static String getErrorQueue(String queue) {
      return "E." + queue;
   }

   public MessageAccess getAccess() {
      return this.access;
   }

   @Reference
   public void setAccess(MessageAccess access) {
      this.access = access;
   }

   public void delete(List<LockKey> lockKeys) throws LockException {
      if (this.connectionFactory != null) {
         Connection connection = null;
         Session session = null;

         try {
            connection = this.connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true, 1);
            Iterator var4 = lockKeys.iterator();

            while(var4.hasNext()) {
               LockKey key = (LockKey)var4.next();
               List<String> ids = new ArrayList();
               ids.add(key.entry);
               this.access.moveMessage(session, getErrorQueue(key.source), key.source, ids, new PropertiesChange() {
                  public Message change(Message message) throws JMSException {
                     message.setJMSType((String)null);
                     return message;
                  }
               });
               session.commit();
            }
         } catch (JMSException var15) {
            throw new LockException(var15);
         } catch (InvalidInputException var16) {
            throw new LockException(var16);
         } finally {
            this.tryClose(session);
            if (connection != null) {
               try {
                  connection.close();
               } catch (JMSException var14) {
                  throw new LockException(var14);
               }
            }

         }
      } else {
         LOG.debug("no connection factory bound, do nothing");
      }

   }

   public List<LockInfo> get() throws LockException {
      List<LockInfo> ret = new ArrayList();
      if (this.connectionFactory != null) {
         Connection connection = null;
         Session session = null;
         QueueBrowser browser = null;

         try {
            connection = this.connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, 1);
            List<String> errorQueues = this.queuesToRetry.getErrorQueues();
            Iterator var6 = errorQueues.iterator();

            while(var6.hasNext()) {
               String queue = (String)var6.next();

               try {
                  browser = this.access.browseMessages(session, queue, "JMSType = 'DL'");
                  Enumeration messages = browser.getEnumeration();

                  while(messages.hasMoreElements()) {
                     Message message = (Message)messages.nextElement();
                     ret.add(new LockInfo("JmsDeadLetter", getQueue(queue), message.getJMSMessageID(), message.getJMSTimestamp(), message.getJMSExpiration()));
                  }
               } finally {
                  this.tryClose(browser);
               }
            }
         } catch (JMSException var24) {
            throw new LockException(var24);
         } finally {
            this.tryClose(session);
            if (connection != null) {
               try {
                  connection.close();
               } catch (JMSException var22) {
                  throw new LockException(var22);
               }
            }

         }
      } else {
         LOG.debug("no connection factory bound, do nothing");
      }

      return ret;
   }

   public boolean retryIdsCheck(String errorQueue, List<String> ids) throws JMSException, InvalidInputException {
      if (this.connectionFactory != null) {
         Connection connection = null;
         Session session = null;

         try {
            connection = this.connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true, 1);
            LOG.info("Starting move of messages from error queue back to processing queue");
            LOG.info(MessageFormat.format("Processing error queue: {0}", errorQueue));
            if (!this.access.moveMessageCheck(session, errorQueue, getQueue(errorQueue), ids, (PropertiesChange)null)) {
               throw new InvalidInputException("The selected message could not be found");
            }
         } finally {
            this.tryClose(session);
            if (connection != null) {
               connection.close();
            }

         }
      } else {
         LOG.debug("no connection factory bound, do nothing");
      }

      return false;
   }

   public RetryQueues getRetryQueues() {
      return this.queuesToRetry;
   }

   @Reference
   public void setRetryQueues(RetryQueues retryQueues) {
      this.queuesToRetry = retryQueues;
   }

   public void retryQueues(List<String> errorQueues) throws JMSException {
      LOG.debug("retryQueues()");
      if (this.connectionFactory != null) {
         Connection connection = null;
         Session session = null;

         try {
            connection = this.connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true, 1);
            LOG.info("Starting move of messages from error queue back to processing queue");
            this.retryQueuesInt(session, errorQueues);
         } finally {
            this.tryClose(session);
            if (connection != null) {
               connection.close();
            }

         }
      } else {
         LOG.debug("no connection factory bound, do nothing");
      }

   }

   public void retryQueuesInt(Session session, List<String> errorQueues) throws JMSException {
      Iterator var3 = errorQueues.iterator();

      while(var3.hasNext()) {
         String errorQueue = (String)var3.next();
         LOG.info(MessageFormat.format("Processing error queue: {0}", errorQueue));
         String fromSelector = "SAPJMSRetryAt < " + System.currentTimeMillis();
         LOG.info("Using criterion " + fromSelector + " to select the messages");
         this.access.moveMessage(session, errorQueue, getQueue(errorQueue), fromSelector, (PropertiesChange)null);
      }

   }

   public ThreadPoolExecutor getExecutor() {
      return this.executor;
   }

   public int getThreadCount() {
      return this.threadCount;
   }

   public void shutDown() {
      this.executor.shutdown();
   }

   void setThreadCount(int threadCount) {
      this.threadCount = threadCount;
   }

   private static class RetryThreadFactory implements ThreadFactory {
      private final AtomicInteger poolNumber = new AtomicInteger(1);
      private final ThreadGroup group;
      private final AtomicInteger threadNumber = new AtomicInteger(1);
      private final String namePrefix;

      RetryThreadFactory() {
         SecurityManager s = System.getSecurityManager();
         this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
         this.namePrefix = "jms-retry-" + this.poolNumber.getAndIncrement() + "-thread-";
      }

      public Thread newThread(Runnable r) {
         Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
         if (t.isDaemon()) {
            t.setDaemon(false);
         }

         if (t.getPriority() != 5) {
            t.setPriority(5);
         }

         return t;
      }
   }
}
