package com.architecture.first.framework.business.vicinity.todo;

import ch.qos.logback.core.html.NOPThrowableRenderer;
import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.acknowledgement.Acknowledgement;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import com.architecture.first.framework.technical.cache.JedisHCursor;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a list of items for actors in a group to process
 */
@Component
public class ToDoList {
    public enum Status {
        Pending ("Pending"),
        InProgress ("InProgress"),
        Completed ("Completed"),
        Failed ("Failed");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    public static long NOT_EXECUTED = -1l;

    @Autowired
    private JedisPooled jedis;

    @Autowired
    private Acknowledgement ack;

    @Autowired
    private Vicinity vicinity;

    @Autowired
    private VicinityInfo vicinityInfo;

    private final Gson gson = new Gson();

    public static String TO_DO_LIST = "ToDo";

    /**
     * Add a task that represents an unacknowledged event
     * @param group - group, such as Merchant
     * @param key - key to determine entry
     * @param position - position in the UnAck list
     * @return 1 if successful
     */
    public long addTask(String group, String key, long position) {
        if (isEnabled()) {
            var entry = new ToDoListEntry(group, key, position);
            return jedis.hset(generateSignature(group), entry.toString(), Status.Pending.status);
        }

        return NOT_EXECUTED;
    }

    /**
     * Adds an event to process later
     * @param event
     * @return 1 if successful
     */
    public long addTask(ArchitectureFirstEvent event) {
        if (isEnabled()) {
            event.setAsToDoTask(true);

            var entry = new ToDoListEntry(event.toFirstGroup(), event.getRequestId(), event.index());
            event.setToDoLink(entry.toString());

            return jedis.hset(generateSignature(event.toFirstGroup()), entry.toString(), Status.Pending.status);
        }

        return NOT_EXECUTED;
    }

    /**
     * Completes a TO-DO task.
     * @param event
     * @return 1 if successful
     */
    public long completeTask(ArchitectureFirstEvent event) {
        if (isEnabled()) {
            ack.recordAcknowledgement(event);
            jedis.hdel(generateSignature(event.toFirstGroup()), event.getToDoLink());  // delete if not owned
            var entry = new ToDoListEntry(event.getTarget().get().name(), event.getRequestId(), event.index());
            return jedis.hdel(generateSignature(event.toFirstGroup()), entry.toString());
        }

        return NOT_EXECUTED;
    }

    /**
     * Fails a TO-DO task.
     * @param event
     * @return 1 if successful
     */
    public long failTask(ArchitectureFirstEvent event) {
        if (isEnabled()) {
            return jedis.hset(generateSignature(event.toFirstGroup()), event.getToDoLink(), Status.Failed.status);
        }

        return NOT_EXECUTED;
    }

    /**
     * Rassigns a TO-DO task
     * @param group
     * @param key
     * @return 1 if successful
     */
    public long reassignTask(String group, String key) {
        if (isEnabled()) {
            return jedis.hset(generateSignature(group), key, Status.Pending.status);
        }

        return NOT_EXECUTED;
    }

    /**
     * Closes a TO-DO task
     * @param group
     * @param key
     * @return 1 if successful
     */
    public long closeTask(String group, String key) {
        if (isEnabled()) {
            return jedis.hdel(generateSignature(group), key);
        }

        return NOT_EXECUTED;
    }

    // Note: has side effects
    public Optional<ArchitectureFirstEvent> acquireAvailableTask(String group, String requestor) {
        if (isEnabled()) {
            AtomicReference<ToDoListEntry> ref = new AtomicReference<>();

            var cursor = new JedisHCursor(jedis);
            var signature = generateSignature(group);
            cursor.processAll(signature, e -> {
                var entry = ToDoListEntry.from(e.getKey());
                if (ref.get() == null && e.getValue().equals(Status.Pending.toString())) {
                    ref.set(entry);
                    return true;
                } else if (!entry.hasOwner() || (entry.hasOwner() && !vicinity.actorIsAvailable(entry.getOwner()))) {
                    var event = ack.getUnacknowledgedEvent(entry.getKey(), String.valueOf(entry.getIndex()));
                    if (event != null) {
                        reassignTask(entry.getGroup(), e.getKey());
                    } else {
                        closeTask(entry.getGroup(), e.getKey());
                    }
                }

                return false;
            });

            if (ref.get() != null) {
                var entry = ref.get();
                var event = ack.getUnacknowledgedEvent(entry.getKey(), String.valueOf(entry.getIndex()));
                if (event != null) {        // there may be no events to process
                    if (StringUtils.isNotEmpty(entry.getOwner())) {
                        event.setOriginalActorName(entry.getOwner());
                    }
                    event.shouldAwaitResponse(false);

                    jedis.hdel(signature, entry.toString());
                    entry.setOwner(requestor);
                    jedis.hset(signature, entry.toString(), Status.InProgress.status);

                    return Optional.of(event);
                } else {
                    closeTask(entry.getGroup(), entry.toString());
                }

                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Generates a TO-DO list signature by group
     * @param group
     * @return
     */
    private String generateSignature(String group) {
        return String.format("%s:%s",TO_DO_LIST, group);
    }

    private boolean isEnabled() {
        return vicinityInfo.getTodo().equals(VicinityInfo.VALUE_ENABLED);
    }
}
