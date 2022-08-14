package com.architecture.first.framework.business.vicinity.acknowledgement;

import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.events.AcknowledgementEvent;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import com.architecture.first.framework.technical.cache.JedisHCursor;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A repository for the acknowledgement of the acceptance of events
 */
@Slf4j
@Repository
public class Acknowledgement {

    public enum Status {
        Unacknowledged ("Unacknowledged"),
        Acknowledged ("Acknowledged");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    public static final String INDEX = "index";
    public static long NOT_EXECUTED = -1l;

    public class Entry {
        private final String classname;
        private final ArchitectureFirstEvent json;

        protected Entry(String classname, ArchitectureFirstEvent json) {
            this.classname = classname;
            this.json = json;
        }
        public String toString() {var json = gson.toJson(this); return json;}
    }

    private final int expirationSeconds = 3600; //43200; // 12 hours
    private static final Gson gson = new Gson();
    public static String ACK_TEMPLATE = "Ack";
    public static String UNACK_TEMPLATE = "UnAck";

    @Autowired
    private JedisPooled jedis;

    @Autowired
    private Vicinity vicinity;

    @Autowired
    private VicinityInfo vicinityInfo;

    private final String ackConnectionId = UUID.randomUUID().toString();

    /**
     * Records an event that needs acknowledgement and has not been acknowledged yet
     * @param event
     * @return the item number of the event
     */
    public long recordUnacknowledgedEvent(ArchitectureFirstEvent event) {
        if (isEnabled()) {
            if ((event.name().equals("SelfVicinityCheckupEvent"))) {
                return 0;
            }

            var ack = generateHandle(event.getRequestId(), Status.Unacknowledged);
            var index = jedis.hincrBy(ack, INDEX, 1);
            event.setIndex(index);
            event.setOriginalActorName(event.toFirst());
            var message = vicinity.generateMessage(event, event.toFirst());

            // note: these calls should be in a transaction

            jedis.hset(ack, String.valueOf(index), message.toString());
            jedis.expire(ack, expirationSeconds);

            return index;
        }

        return NOT_EXECUTED;
    }

    /**
     * Removes an event that has been acknowledged
     * @param requestId
     * @param index
     */
    public void removeUnacknowledgedEvent(String requestId, long index) {
        if (isEnabled()) {
            var ack = generateHandle(requestId, Status.Unacknowledged);
            jedis.hdel(ack, String.valueOf(index));
        }
    }

    /**
     *
     * @param event
     * @return
     */
    public long recordAcknowledgement(ArchitectureFirstEvent event) {
        if (isEnabled()) {
            if (event.name().equals("SelfVicinityCheckupEvent") || event.name().equals("AcknowledgementEvent")) {
                return 0;
            }

            var ack = generateHandle(event.getRequestId(), Status.Acknowledged);
            var actor = event.getTarget().get();
            var message = vicinity.generateMessage(event, event.toFirst());

            // note: these calls should be in a transaction
            var index = event.index();
            if (!jedis.hexists(ack, String.valueOf(index))) {
                jedis.hset(ack, String.valueOf(index), message.toString());
                jedis.expire(ack, expirationSeconds);

                removeUnacknowledgedEvent(event.getRequestId(), event.index());

                // To and From reversed for acknowledgement
                var ackEvent = new AcknowledgementEvent(this, event.toFirst(), event.from())
                        .setAcknowledgementEvent(event);
                var ackMessage = vicinity.generateMessage(ackEvent, event.from());
                vicinity.publishMessage(ackEvent.toFirst(), ackMessage.toString());
            }

            return index;
        }

        return NOT_EXECUTED;
    }

    /**
     * Get an unacknowledged event by request d and index
     * @param requestId
     * @param index
     * @return
     */
    public ArchitectureFirstEvent getUnacknowledgedEvent(String requestId, String index) {
        if (isEnabled()) {
            var ack = generateHandle(requestId, Status.Unacknowledged);
            var json = jedis.hget(ack, index);
            if (StringUtils.isNotEmpty(json)) {
                var message = VicinityMessage.from(json);

                return ArchitectureFirstEvent.from(this, message);
            }
        }

        return null;
    }

    /**
     * generate a handle to an event set for a given request
     * @param requestId
     * @param status
     * @return
     */
    private String generateHandle(String requestId, Status status) {
        return String.format("%s/%s", requestId,  (status == Status.Acknowledged) ? ACK_TEMPLATE : UNACK_TEMPLATE);
    }

    /**
     * Determines if a given event was acknowledged
     * @param requestId
     * @param eventName
     * @return
     */
    public boolean hasAcknowledged(String requestId, String eventName) {
        if (isEnabled()) {
            var ack = generateHandle(requestId, Status.Acknowledged);

            AtomicBoolean hasPassed = new AtomicBoolean(false);

            var cursor = new JedisHCursor(jedis);
            cursor.processAll(ack, e -> {
                var vicinityMessage = VicinityMessage.from(e.getValue());
                var event = (AcknowledgementEvent) ArchitectureFirstEvent.from(this, vicinityMessage);
                if (event.getAcknowledgedEventName().equals(eventName)) {
                    hasPassed.set(true);
                    return true;
                }

                return false;
            });

            return hasPassed.get();
        }

        return false;
    }

    private boolean isEnabled() {
        return vicinityInfo.getAcknowledgement().equals(VicinityInfo.VALUE_ENABLED);
    }
 }
