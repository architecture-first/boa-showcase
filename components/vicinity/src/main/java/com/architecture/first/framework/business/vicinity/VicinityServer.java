package com.architecture.first.framework.business.vicinity;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.conversation.Conversation;
import com.architecture.first.framework.business.vicinity.exceptions.VicinityException;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import com.architecture.first.framework.business.vicinity.threading.VicinityConnections;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.LocalEvent;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/**
 * The Vicinity class is the main communication vehicle between Actors.
 * It can be thought of as a virtual location in which participating Actors exist.
 *
 *         Note:
 *             The Vicinity component hides the communication from the Actor.
 *             In this case, it is using Redis pub/sub to communicate
 *             This implementation could be replaced with REST calls or Message Queues or Event Grids, etc.
 */
@Service()
@Slf4j
public class VicinityServer implements Vicinity {
    public static final int ROSTER_LIST_COUNT = 100;
    public static final int JEDIS_TIMEOUT = 60000;
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private VicinityProxy vicinityProxy;

    @Autowired
    private VicinityInfo vicinityInfo;

    @Autowired
    private Conversation convo;

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    private final Map<String, VicinityConnections> taskGroups = new HashMap<>();

    /**
     * Initializes the Vicinity
     */
    @PostConstruct
    public void onVicinityEnvironmentInit() {

    }

    /**
     * Creates a new thread to manage Vicinity tasks
     */
    private final ThreadFactory threadFactory = new ThreadFactory() {
        private final AtomicLong threadIndex = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("a1-vicinity-" + threadIndex.getAndIncrement());
            return thread;
        }
    };

    /**
     * Executes threads in a dedicated pool
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 10, 30, TimeUnit.SECONDS,
                                    new LinkedBlockingDeque<>(10),
                                    threadFactory,
                                    new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * Peforms Vicinity initialization
     */
    @PostConstruct
    protected void init() {

    }

    /**
     * Gracefully shuts down resources
     */
    @PreDestroy
    private void shutdown() {
        taskGroups.values().stream().forEach(VicinityConnections::shutdown);
    }

    /**
     * Event listener for events.
     * The events are sent to the target actor in the Vicinity if they are not local or meant to be received in the current process.
     * @param event
     */
    @Override
    public void onApplicationEvent(ArchitectureFirstEvent event) {

        if (!event.isLocal() && !(event instanceof LocalEvent)) { // local events don't leave this process
            if (!event.isPropagatedFromVicinity()) { // don't echo back out events
                if (onVicinityReceiveEvent(event)) {

                    if (onVicinityEventSecurityCheck(event)) {
                        try (Jedis jedisDedicated = new Jedis(host, port)) {
                            event.to().forEach(t -> {
                                if (StringUtils.isNotEmpty(t)) {
                                    if (!event.hasTargetActor() || (event.hasTargetActor() && !t.equals(event.getTarget().get().name()))) {
                                        var message = onVicinityBeforePublishMessage(event, t);

                                        onVicinityRecordConvo(event);
                                        String channel = onVicinityPublishMessage(jedisDedicated, message);

                                        onVicinityAfterPublishMessage(channel, message);
                                        if (event.isErrorEvent()) {      // send error events to vicinity monitor as well as the caller
                                            onVicinityError(jedisDedicated, event, channel, message);
                                        }
                                    }
                                } else {
                                    onVicinityEmptyTarget(event);
                                }
                            });
                        } catch (Exception e) {
                            onVicinityProcessingException(e);
                        }
                    } else {
                        onVicinitySecurityGuardRejectedEvent(event);
                    }
                }
                else {
                    onVicinityReceivedEventBlocked(event);
                }
            }
            else {
                onVicinityAlreadyPropagatedEvent(event);
            }
        }
        else {
            onVicinityLocalEventIgnored(event);
        }
    }


    /**
     * Generates a Vicinity message from and event
     * @param event
     * @param to
     * @return
     */
    public VicinityMessage generateMessage(ArchitectureFirstEvent event, String to) {
        return vicinityProxy.generateMessage(event, to);
    }

    /**
     * Publish a message to the Vicinity
     * @param to
     * @param contents
     */
    public void publishMessage(String to, String contents) {
        vicinityProxy.publishMessage(to, contents);
    }

    @Override
    public void subscribe(Actor owner, String target, BiFunction<Actor, ArchitectureFirstEvent, Void> fnCallback) {
        vicinityProxy.subscribe(owner, target, fnCallback);
    }


    /**
     * Process a VicinityMessage
     */
    public void publishVicinityMessage(VicinityMessage message) {
        onVicinityReceiveRawMessage(message);
        var event = onVicinityCreateArchitectureFirstEvent(this, message);

        onVicinityAfterCreateArchitectureFirstEvent(event);
        onApplicationEvent(event);
        onVicinityAfterProcessArchitectureFirstEvent(event);
    }

    /**
     * Handle an invalid token
     * @param event
     */
    private void processInvalidToken(ArchitectureFirstEvent event) {
        String msg = "Received Invalid Token: " + new Gson().toJson(event);
        log.error(msg);
        SecurityGuard.reportError(event, msg);
        SecurityGuard.replyToSender(event.setMessage(msg));
    }

    /**
     * Unsubscribe from the event subscription
     * @param target
     */
    public void unsubscribe(String target) {
        vicinityProxy.unsubscribe(target);
    }

    /**
     * Determines if the subscription connections are ok
     * @param target
     * @param numberOfConnections
     * @return
     */
    public boolean areConnectionsOk(String target, int numberOfConnections) {
        return taskGroups.containsKey(target) && taskGroups.get(target).isOk(numberOfConnections);
    }

    /**
     * Returns an active Actor name from a specific group to communicate with
     * @param type
     * @param project
     * @return
     */

    protected String findActiveActor(String type, String project) {
        return vicinityProxy.findActiveActor(type, project);
    }

    /**
     * Returns an actor name for a group and a project
     * @param type
     * @param project
     * @return
     */
    public String findActor(String type, String project) {
        return vicinityProxy.findActor(type, project);
    }

    /**
     * Returns an actor name for a group and default project
     * @param type
     * @return
     */
    public String findActor(String type) {
        return vicinityProxy.findActor(type);
    }

    /**
     * Determines if a specific Actor is available
     * @param name
     * @return
     */
    public boolean actorIsAvailable(String name) {
        return vicinityProxy.actorIsAvailable(name);
    }

    public static String channelFor(String name) {
        return "channel: " + name;
    }

    // Lifecycle events (start)
    private void onVicinityAfterProcessArchitectureFirstEvent(ArchitectureFirstEvent event) {
    }

    private ArchitectureFirstEvent onVicinityCreateArchitectureFirstEvent(VicinityServer vicinityServer, VicinityMessage message) {
        return ArchitectureFirstEvent.from(this, message);
    }

    private void onVicinityAfterCreateArchitectureFirstEvent(ArchitectureFirstEvent event) {
    }

    private void onVicinityReceiveRawMessage(VicinityMessage message) {
    }

    private void onVicinityReceivedEventBlocked(ArchitectureFirstEvent event) {
        log.info("Message blocked: " + event.toString());
    }

    private void onVicinityProcessingException(Exception e) {
        // TODO - handle threading errors.
        log.error("Message error:", e);
    }

    private void onVicinityEmptyTarget(ArchitectureFirstEvent event) {
        String msg = "to: is empty on message: " + event.getMessage();
        log.info(msg);
        throw new VicinityException(msg);
    }

    private void onVicinityError(Jedis jedis, ArchitectureFirstEvent event, String channel, VicinityMessage message) {
        if (!event.toFirst().equals(SecurityGuard.VICINITY_MONITOR)) {
            log.info("Published Event to Vicinity Monitor: " + channel + " " + event.getRequestId());
            jedis.publish(channelFor(SecurityGuard.VICINITY_MONITOR), message.toString());
        }
    }

    private void onVicinityAfterPublishMessage(String channel, VicinityMessage message) {
        log.info("Published Event to Vicinity: " + channel + " message: " + message);
    }

    private String onVicinityPublishMessage(Jedis jedis, VicinityMessage message) {
        String channel = channelFor(message.to());
        jedis.publish(channel, message.toString());

        return channel;
    }

    private VicinityMessage onVicinityBeforePublishMessage(ArchitectureFirstEvent event, String t) {
        return generateMessage(event, t);
    }

    private void onVicinityRecordConvo(ArchitectureFirstEvent event) {
        convo.record(event, Conversation.Status.SendingViaVicinity);
    }

    private boolean onVicinityReceiveEvent(ArchitectureFirstEvent event) {
        log.info("Vicinity Server Receiving event: " + event);

        if (event.name().equals("ActorEnteredEvent")) {
            if (vicinityInfo.getActorEnteredEvent().equals(VicinityInfo.VALUE_DISABLED)) {
                return false;
            }
        }

        return true;
    }

    private boolean onVicinityEventSecurityCheck(ArchitectureFirstEvent event) {
        return SecurityGuard.isOkToProceed(event);
    }

    private void onVicinitySecurityGuardRejectedEvent(ArchitectureFirstEvent event) {
        processInvalidToken(event);
    }

    private void onVicinityAlreadyPropagatedEvent(ArchitectureFirstEvent event) {
        log.warn("Local event already propagated: " + event);
    }

    private void onVicinityLocalEventIgnored(ArchitectureFirstEvent event) {
        log.warn("Local event ignored: " + event);
    }

    // Lifecycle events (end)
}
