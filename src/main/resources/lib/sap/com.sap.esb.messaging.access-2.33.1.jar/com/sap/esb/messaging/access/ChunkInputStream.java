package com.sap.esb.messaging.access;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class ChunkInputStream extends InputStream {
   private Enumeration<byte[]> chunks;
   private byte[] chunk;
   private int pos;
   private long size;
   private Session session;
   private Connection connection;

   protected ChunkInputStream() {
   }

   public ChunkInputStream(ConnectionFactory connectionFactory, MessageAccess access, String queueName, String id) throws JMSException, IOException {
      this.connection = connectionFactory.createConnection();
      this.connection.start();
      this.session = this.connection.createSession(false, 1);
      this.chunks = access.downloadMessage(this.session, queueName, id);
      this.updateChunk();
   }

   public int getEncryptionKeyId() {
      return this.chunks instanceof ChunkMessageHelper ? ((ChunkMessageHelper)this.chunks).getEncryptionKeyId() : 0;
   }

   public Map<String, Object> getJMSMessageProperties() {
      return this.chunks instanceof ChunkMessageHelper ? ((ChunkMessageHelper)this.chunks).getMessageProperties() : Collections.emptyMap();
   }

   protected boolean updateChunk() {
      boolean more = this.chunks.hasMoreElements();
      if (more) {
         this.chunk = (byte[])this.chunks.nextElement();
         this.pos = 0;
         this.size = (long)this.chunk.length;
      }

      return more;
   }

   public int read() throws IOException {
      int i;
      if ((long)this.pos < this.size) {
         i = this.chunk[this.pos] & 255;
      } else if (this.updateChunk()) {
         i = this.chunk[this.pos] & 255;
      } else {
         i = -1;
      }

      ++this.pos;
      return i;
   }

   public void close() throws IOException {
      try {
         super.close();
         this.session.close();
         this.connection.close();
      } catch (JMSException var2) {
         throw new IOException(var2);
      }
   }
}
