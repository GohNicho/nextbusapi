/*******************************************************************************
 * Copyright (C) 2013 by James R. Doyle
 *
 * This file is part of the NextBus® Livefeed Java Adapter (nblf4j). See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership and licensing.
 *
 * nblf4j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * nblf4j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with UJMP; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Usage of the NextBus Web Service and its data is subject to separate
 * Terms and Conditions of Use (License) available at:
 * 
 *      http://www.nextbus.com/xmlFeedDocs/NextBusXMLFeed.pdf
 * 
 * 
 * NextBus® is a registered trademark of Webtech Wireless Inc.
 *
 ******************************************************************************/
package net.sf.nextbus.html5demo.service;
import java.util.Date;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import org.codehaus.jackson.map.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.security.Principal;
/**
 *
 * @author jrd
 */
public class ClientEventStream implements Serializable {

    private static Logger log = LoggerFactory.getLogger(ClientEventStream.class);
   
    /** JSON serializer */
    private static final ObjectMapper jsonSerializer = new ObjectMapper();
    
    /**
     * Whether this event stream sends (id,data) pairs or (message, data) pairs.
     * See the HTML 5 Server Sent Events spec for more details.
     */
    public static enum StreamType {
        EventSequence, MessageType
    };
    
    /**
     * A queued event to stream over the SSE socket.
     */
    public class EventEntry implements Serializable {

        /**
         * Ctor for Sequenced messages.
         * @param evId a monotonically increasing msg sequence id.
         * @param payload 
         */
        public EventEntry(int evId, Object payload) {
            eventId=evId;
            opaquePayload=payload;
            enqueued = new Date();
        }
        /**
         * Ctor for Typed messages.
         * @param msgTypeId a string identified the message type.
         * @param payload
         */
        public EventEntry(String msgTypeId, Object payload) {
            messageTypeId=msgTypeId;
            opaquePayload=payload;
            enqueued = new Date();
        }
        
        private final Object opaquePayload;
        private int eventId;
        private String messageTypeId;
        private final Date enqueued;

        public Object getOpaquePayload() {
            return opaquePayload;
        }

        public int getEventId() {
            return eventId;
        }

        public Date getEnqueued() {
            return enqueued;
        }

        
        @Override
        public String toString() {
            return "EventEntry{" + "opaque=" + opaquePayload + ", eventId=" + eventId + ", enqueued=" + enqueued + '}';
        }
    }
    
    // ** Stream configuration **
    private String id;
    private String owner;
    private StreamType streamType;
    private Long waitTimeout = new Long(0);
    private LinkedList<EventEntry> ll;
    // ** Stream management and metrics **
    static private final int PASSIVATING_EVENT_ID = -1;
    private int nextEventId;
    private int lastEventIdSent;
    private final Date streamCreated;
    private Date lastEventSentTime;
    private Date lastEventRecvdTime;
    private int messagesRecvd;
    private int clientConnects;
    private int storageLimit;

    /**
     * Ctor - create a event stream on behalf of a Servlet instance dedicated to HTML 5 Server Side Events.
     * @param type stream type, message sequence v. message time
     * @param pollTimeout the sleep time on the consumer (servlet) side of the queue - in milliseconds.
     */
    public ClientEventStream(StreamType type, long pollTimeout) {
        if (type==null) {
            throw new IllegalArgumentException("StreamType argument is required.");
        }
        if (pollTimeout<0) {
             throw new IllegalArgumentException("poll timeout cant be negative.");
        }
        ll = new LinkedList<EventEntry>();
        streamType = type;
        this.waitTimeout = pollTimeout;
        streamCreated = new Date();
        
        
    }

    /**
     * Set the unique identifier for this EventStream (example, a UUID or an HTTP Session ID).
     * @param arg unique id.
     */
    public void setId(String arg) {
        id = arg;
    }
    public String getId() {
        return id;
    }

    /**
     * Set the owner (for debug trace printing purposes) of this stream (example, the Principal name)
     * @param arg 
     */
    public void setOwner(String arg) {
        this.owner = arg;
    }
    public void setOwner(Principal arg) {
        if (owner == null) return;
        owner = arg.getName();
    }
    public String getOwner() {
        return owner;
    }
    /**
     * <i>Producer</i>-side.
     * @param arg 
     */
    public synchronized void addEvent(Object arg) {
        // never queue nulls, ever.
        if (arg == null) {
            return;
        }
        // 
        if (nextEventId == PASSIVATING_EVENT_ID) {
            log.trace("Stream is marked for passivation, take no new events.");
            return;
        }
        int eventId = nextEventId++;
        EventEntry e = new EventEntry(nextEventId++, arg);
        ll.addLast(e);
        lastEventSentTime = new Date();
        notifyAll();
        log.debug("notified stream of new event available.");
    }

    /**
     * Consumer-side (servlet) method to poll & wait on the event stream
     * @return
     * @throws InterruptedException 
     */
    public synchronized EventEntry getNextEventToSend() throws InterruptedException {
        log.debug("polling and waiting on event stream for new event to send.");
        while (ll.isEmpty()) {
            // waiting
            log.debug("about to wait {} msec for next event to arrive.", new Object[]{waitTimeout});
            wait(waitTimeout);
            if (ll.isEmpty()) {
                log.trace("done waiting, no event available, looping.");
            } else {
                 log.trace("done waiting, event available.");
            }
            // done waiting
        }
        EventEntry e = ll.getFirst();
        lastEventIdSent = e.eventId;
        lastEventRecvdTime = new Date();
        messagesRecvd++;
        return e;
    }

    /**
     * Event consumer side utility to convert a given event entry to an HTML5 Server Side event
     * fragment to be sent over an existing HTTP connection. The object payload of the even is
     * flattened to JSON during this operation as well. Depending on the modality of this streamer
     * (id or message based), the proper wire format is constructed for you. See the HTML5 Server Sent
     * events spec for more details.
     * 
     * @param e the event to serialize
     * @return HTML Event Stream (HTTP) message fragment to transmit.
     * @throws IOException 
     */
    public String toJSON(EventEntry e) throws IOException {
        String json = jsonSerializer.writeValueAsString(e.getOpaquePayload());
       
        StringBuilder b = new StringBuilder();
        switch (streamType) {
            case EventSequence:
                b.append(String.format("id: %d\ndata: %s", e.eventId, json));
                break;
            case MessageType:
                b.append(String.format("message: %d\ndata: %s", e.messageTypeId, json));
                break;
            default:
                throw new RuntimeException("No strategy for constructing the stream fragment.");
        }
        return b.toString();
    }
    
    /**
     * <B>Consumer</B>-side method to dequeue a message once it has been accepted to wire (i.e. sent to output stream and flushed).
     * @param arg the message to remove, located in queue by its reference identity.
     */
    public synchronized void acknowledgeSent(EventEntry arg) {
        if (ll.isEmpty()) {
            return;
        }
        
        EventEntry e = ll.getFirst();
        // only remove the event if it already exists at the head of queue.
        if (e.equals(arg) == false) {
            return;
        }
        lastEventIdSent = e.eventId;
        lastEventSentTime = new Date();
        ll.remove(0);
    }

    public synchronized void purgeAll() {
        nextEventId = PASSIVATING_EVENT_ID;
        ll.clear();
    }
    
    public void reconnect() {
        clientConnects++;
    }
    
    @Override
    public String toString() {
        return "ClientEventStream{" + "id=" + id + ", owner=" + owner + ", queue size=" + ll.size() + ", nextEventId=" + nextEventId + ", lastEventIdSent=" + lastEventIdSent + ", lastEventSentTime=" + lastEventSentTime + ", lastEventRecvdTime=" + lastEventRecvdTime + '}';
    }
    
    /**
     * For testing only.  Generates an internal event feed using a new thread to drive
     * the producer side of this stream object.
     */
   // private static final Random r = new Random();
    public void test() {
        Thread t = new Thread() {
 
            @Override
            public void run() {
                long delay = 1040;
                while (true) {
                    if (nextEventId == PASSIVATING_EVENT_ID) {
                        break;
                    }
                    Map value = new HashMap();
                    value.put("time",System.currentTimeMillis());
                    value.put("type","testcase");
                    value.put("val",delay);
                    addEvent(value);
                    log.info("added test event.");
                    try {
                        Thread.currentThread().sleep(delay);
                    } catch (InterruptedException ie) {
                        log.trace("thread interrupted while asleep. continuing...");
                    }
                }
                log.info("test driver thread exiting.");
            }
        };
        // schedule this test driver to run.
        t.start();
        log.debug("test traffic generator started.");
    }

    
}
