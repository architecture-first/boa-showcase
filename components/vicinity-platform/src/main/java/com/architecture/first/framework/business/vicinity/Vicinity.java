package com.architecture.first.framework.business.vicinity;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.vicinity.conversation.Conversation;
import com.architecture.first.framework.business.vicinity.events.ErrorEvent;
import com.architecture.first.framework.business.vicinity.events.VicinityConnectionBrokenEvent;
import com.architecture.first.framework.business.vicinity.exceptions.VicinityException;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import com.architecture.first.framework.business.vicinity.threading.VicinityConnections;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.events.ArchitectureFirstEvent;
import com.architecture.first.framework.technical.events.LocalEvent;
import com.architecture.first.framework.technical.threading.Connection;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.ScanParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Vicinity class is the main communication vehicle between Actors.
 * It can be thought of as a virtual location in which participating Actors exist.
 *
 *         Note:
 *             The Vicinity component hides the communication from the Actor.
 *             In this case, it is using Redis pub/sub to communicate
 *             This implementation could be replaced with REST calls or Message Queues or Event Grids, etc.
 */
@Component
@Slf4j
public class Vicinity implements ApplicationListener<ArchitectureFirstEvent> {
    public static final int ROSTER_LIST_COUNT = 100;
    public static final int JEDIS_TIMEOUT = 60000;
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private Conversation convo;

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    private final Map<String, LinkedList<String>> workQueueMap = new HashMap<>();
    private final Map<String, Integer> currentWorkforceSize = new HashMap<>();

    private static final Map<Future, String> activeTasks = new HashMap<>();
    private final Map<String, VicinityConnections> taskGroups = new HashMap<>();

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
                log.info("Receiving event: " + event);

                if (SecurityGuard.isOkToProceed(event)) {
                    try (Jedis jedisDedicated = new Jedis(host, port)) {
                        event.to().forEach(t -> {
                            if (StringUtils.isNotEmpty(t)) {
                                if (event.getTarget().isEmpty() || (event.getTarget().isPresent() && !t.equals(event.getTarget().get().name()))) {
                                    VicinityMessage message = generateMessage(event, t);

                                    String channel = channelFor(t);
                                    var jsonEvent = new Gson().toJson(event);
                                    log.info("Published Event to Vicinity: " + channel + " message: " + jsonEvent);
                                    convo.record(event, Conversation.Status.SendingViaVicinity);

                                    jedisDedicated.publish(channel, message.toString());

                                    if (event instanceof ErrorEvent) {      // send error events to vicinity monitor as well as the caller
                                        if (!event.toFirst().equals(SecurityGuard.VICINITY_MONITOR)) {
                                            log.info("Published Event to Vicinity Monitor: " + channel + " " + event.getRequestId());
                                            jedisDedicated.publish(channelFor(SecurityGuard.VICINITY_MONITOR), message.toString());
                                        }
                                    }
                                }
                            } else {
                                String msg = "to: is empty on message: " + event.getMessage();
                                log.info(msg);
                                throw new VicinityException(msg);
                            }
                        });
                    } catch (Exception e) {
                        // TODO - handle threading errors.
                        log.error("Message error:", e);
                    }
                } else {
                    processInvalidToken(event);
                }
            }
        }
    }

    /**
     * Generates a Vicinity message from and event
     * @param event
     * @param to
     * @return
     */
    public VicinityMessage generateMessage(ArchitectureFirstEvent event, String to) {
        VicinityMessage message = new VicinityMessage(event.from(), to);
        message.setPayload(event, event.getClass());
        return message;
    }

    /**
     * Publish a message to the Vicinity
     * @param to
     * @param contents
     */
    public void publishMessage(String to, String contents) {
        try (Jedis jedisDedicated = new Jedis(host, port)) {
            jedisDedicated.publish(Vicinity.channelFor(to), contents);
        }
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
     * Receive events from the environment, such as Redis, and propagate to the intended targets
     * @param owner
     * @param target
     */
    public void subscribe(Actor owner, String target) {
        Runnable submitTask =  () -> {
            try (Jedis jedisDedicated = new Jedis(host, port, JEDIS_TIMEOUT)) {
                jedisDedicated.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        super.onMessage(channel, message);

                        VicinityMessage vicinityMessage = VicinityMessage.from(message);
                        if (vicinityMessage != null) {

                            AtomicReference<String> threadId = new AtomicReference<>();
                            var future = executor.submit(() -> {
                                try {
                                    threadId.set(Thread.currentThread().getName());
                                    ArchitectureFirstEvent event = ArchitectureFirstEvent.from(this, vicinityMessage);
                                    if (event != null) {
                                        event.setPropagatedFromVicinity(true);
                                        event.shouldAwaitResponse(false);  // this flag is for the caller not recipients
                                        event.onVicinityInit();
                                        log.info("Received and Locally Published Event: " + new Gson().toJson(event));
                                        convo.record(event, Conversation.Status.ReceivedInVicinity);

                                        if (SecurityGuard.isOkToProceed(event)) {
                                            event.setAsLocal(false).setAsHandled(false);
                                            publisher.publishEvent(event);
                                        }
                                        else {
                                            processInvalidToken(event);
                                        }
                                    } else {
                                        owner.onError("Vicinity Message is not readable as an ArchitectureFirstEvent: " + vicinityMessage);
                                    }
                                }
                                catch (Exception e) {
                                    owner.onException(new ActorException(owner, e), "Error processing event: ");
                                }
                                finally {
                                    activeTasks.remove(this);
                                }
                            });
                            activeTasks.put(future, "running");

                        }
                        else {
                            owner.onError("Original message is not readable as a VicinityMessage: " + message);
                        }
                    }
                }, channelFor(target));
            }
            catch(Exception e) {
                var evt =  new VicinityConnectionBrokenEvent(this, "vicinity", owner.name())
                        .setOwner(owner.name())
                        .setTargetOwner(target)
                        .setVicinity(this)
                        .setTargetActor(owner);
                owner.onException(evt, new ActorException(owner, e), "Vicinity Error:");
                publisher.publishEvent(evt);
            }
        };

        log.info("Subscription to: " + channelFor(target));

        setupTask(target, submitTask);
    }

    /**
     * Unsubscribe from the event subscription
     * @param target
     */
    public void unsubscribe(String target) {
        if (taskGroups.containsKey(target)) {
            taskGroups.get(target).shutdown();
        }
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
        var workQueueKey = String.format("%s.%s", type,
                StringUtils.isNotEmpty(project) ? project : ArchitectureFirstEvent.DEFAULT_PROJECT);
        if (!workQueueMap.containsKey(workQueueKey) || currentWorkforceSize.get(workQueueKey) == 0 ||
                ( workQueueMap.get(workQueueKey).size() < currentWorkforceSize.get(workQueueKey))) {

            try (Jedis jedisDedicated = new Jedis(host, port)) {
                String roster = getRoster(type);

                String cursor = "0";
                ScanParams scanParams = new ScanParams()
                                            .match("*").count(ROSTER_LIST_COUNT);
                var scanResult = jedisDedicated.hscan(roster, cursor, scanParams);

                LinkedList<String> workQueue = (workQueueMap.containsKey(workQueueKey))
                        ? workQueueMap.get(workQueueKey) : new LinkedList<>();
                workQueueMap.put(workQueueKey, workQueue);

                final List<String> cleanupList = new ArrayList<>();

                final AtomicInteger workforceSize = new AtomicInteger(0);
                scanResult.getResult().forEach(x -> {
                    if (x.getValue().contains("\"message\":\"running\"")) {
                        if (x.getKey().contains(project)) {
                            workQueue.push(x.getKey());
                            workforceSize.incrementAndGet();
                        }
                    }
                    else {
                        cleanupList.add(x.getKey());
                    }
                });

                currentWorkforceSize.put(workQueueKey, workforceSize.get());

                cleanupList.forEach(e -> { jedisDedicated.hdel(roster, e);});
            }
        }

        return (workQueueMap.get(workQueueKey) != null && workQueueMap.get(workQueueKey).size() > 0) ? workQueueMap.get(workQueueKey).pop() : "";
    }

    /**
     * Returns an actor name for a group and a project
     * @param type
     * @param project
     * @return
     */
    public String findActor(String type, String project) {
        var prj = (StringUtils.isNotEmpty(project)) ? project : ArchitectureFirstEvent.DEFAULT_PROJECT;
        // search for actor in default project if that is what was sent
        if (ArchitectureFirstEvent.DEFAULT_PROJECT.equals(prj)) {
            return findActiveActor(type, prj);
        }

        // otherwise, search for actor in actual project first
        var actorName = findActiveActor(type, prj);

        // if not found then find actor in default project
        if (StringUtils.isEmpty(actorName)) {
            return findActiveActor(type, ArchitectureFirstEvent.DEFAULT_PROJECT);
        }

        return actorName;
    }

    /**
     * Returns an actor name for a group and default project
     * @param type
     * @return
     */
    public String findActor(String type) {
        return findActor(type, ArchitectureFirstEvent.DEFAULT_PROJECT);
    }

    /**
     * Returns the current roster of active Actors
     * @param type
     * @return
     */
    private String getRoster(String type) {
        String template = "BulletinBoard:topic/VicinityStatus/%s:%s/Active";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd");
        LocalDate localDate = LocalDate.now(ZoneId.of("GMT"));
        String roster = String.format(template, type, dtf.format(localDate));
        return roster;
    }

    /**
     * Determines if a specific Actor is available
     * @param name
     * @return
     */
    public boolean actorIsAvailable(String name) {
        try (Jedis jedisDedicated = new Jedis(host, port)) {
            String type = name.substring(0, name.indexOf("."));
            String roster = getRoster(type);

            if (jedisDedicated.hexists(roster, name)) {
                return true;
            }
        }

        return false;
    }

    public static String channelFor(String name) {
        return "channel: " + name;
    }

    private void setupTask(String target, Runnable submitTask) {
        var tasks = (taskGroups.containsKey(target))
                ? taskGroups.get(target)
                : addConnection(target);

        if (tasks.containsConnection(target)) {
            var connection = tasks.getConnection(target);
            connection.getFuture().cancel(true);
            connection.getExecutorService().shutdownNow();
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
        var task = executorService.submit(submitTask);
        Connection conn = new Connection(executorService, task);

        tasks.setConnection(target, conn);
    }

    /**
     * Adds a connection to a subscription
     * @param ownername
     * @return
     */
    private VicinityConnections addConnection(String ownername) {
        taskGroups.put(ownername, new VicinityConnections());
        return taskGroups.get(ownername);
    }

}
