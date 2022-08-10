package com.architecture.first.framework.business.actors;

import com.architecture.first.framework.business.actors.exceptions.ActorException;
import com.architecture.first.framework.business.actors.exceptions.ActorNotFoundException;
import com.architecture.first.framework.business.actors.external.RestCall;
import com.architecture.first.framework.business.actors.external.behavior.Behavior;
import com.architecture.first.framework.business.actors.external.behavior.Logic;
import com.architecture.first.framework.business.actors.external.behavior.script.model.PipelineEntry;
import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.acknowledgement.Acknowledgement;
import com.architecture.first.framework.security.events.SecurityHolderEvent;
import com.architecture.first.framework.technical.bulletinboard.BulletinBoard;
import com.architecture.first.framework.technical.bulletinboard.BulletinBoardStatus;
import com.architecture.first.framework.business.vicinity.conversation.Conversation;
import com.architecture.first.framework.business.vicinity.events.*;
import com.architecture.first.framework.business.vicinity.locking.Lock;
import com.architecture.first.framework.business.vicinity.queue.Queue;
import com.architecture.first.framework.business.vicinity.tasklist.Tasklist;
import com.architecture.first.framework.business.vicinity.todo.ToDoList;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.security.events.SecurityIncidentEvent;
import com.architecture.first.framework.security.events.UserTokenReplyEvent;
import com.architecture.first.framework.security.events.UserTokenRequestEvent;
import com.architecture.first.framework.security.model.UserToken;
import com.architecture.first.framework.technical.events.*;
import com.architecture.first.framework.technical.util.RuntimeUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
/**
 * The main component of the platform.
 * The Actor is business focused, proactive, intelligent, robust and handles adversity.
 * The combination of Actors make a system or application.
 * One Actor can support multiple applications and projects.
 */
public class Actor {
    public static final int NUM_CONNECTIONS = 1;
    public static final String PROJECT = "PROJECT";

    @Autowired
    private BulletinBoard bulletinBoard;

    @Autowired
    private Tasklist tasklist;

    @Autowired
    private Conversation convo;

    @Autowired
    private Acknowledgement ack;

    @Autowired
    private ToDoList todo;

    @Autowired
    private Lock lock;

    @Autowired
    private Vicinity vicinity;

    @Autowired
    private Memory memory;

    @Autowired
    private Behavior behavior;

    @Autowired
    private Queue queue;

    @Autowired
    private Logic logic;

    private final Map<String, Function<ArchitectureFirstEvent, Actor>> functions = new HashMap<>();
    private final Map<String, Function<ArchitectureFirstEvent, Boolean>> callbacksByRequest = new HashMap<>();
    private final Map<String, Function<ActorException, Boolean>> errorHandlersByRequest = new HashMap<>();
    private final Map<String, Semaphore> locksByRequest = new HashMap<>();
    private String actorId;
    private String generation = "1.0.0";

    private String project = ArchitectureFirstEvent.DEFAULT_PROJECT;
    private String terminationReason;
    private String JOIN_TOKEN;
    private String OVERRIDE_TOKEN;
    private String OVERRIDE_ACCESS_TOKEN;
    private boolean isAway = false;
    private final boolean isOverriding = false;
    private boolean isTerminating = false;
    private boolean didOnce = false;

    private static final String VICINITY_MONITOR = "VicinityMonitor";
    private static final String ACTOR_NOTES = "ActorNotes:";
    private static final String VICINITY_STATUS = "VicinityStatus/";
    private static final String VICINITY_STATUS_ACTIVE = "active@";


    private String MY_VICINITY_STATUS;
    private String MY_ACTOR_NOTES;

    private BulletinBoardStatus bulletinBoardStatus;
    private final AtomicInteger minutesIn = new AtomicInteger(0);
    private final AtomicInteger hoursIn = new AtomicInteger(0);

    private ZonedDateTime vicinityHeathCheckStartTime = ZonedDateTime.now(ZoneId.of("GMT"));
    private final int expirationSecondsOnVicintyHealthCheck = 10;
    private boolean isVicinityHealthOk = true;
    private boolean isBusy = false;

    /**
     * Setup main Actor items
     */
    @PostConstruct
    protected void init() {
        actorId = UUID.randomUUID().toString();

        MY_VICINITY_STATUS = VICINITY_STATUS + group();
        MY_ACTOR_NOTES = ACTOR_NOTES + group();

        // support blue/green deployment and feature management
        setupProjectIfExists();
        log.info("actorId: " + name());

        // register to hear events
        registerBehavior("VicinityConnectionBroken", Actor.noticeVicinityConnectionBroken);
        registerBehavior("UnhandledException", Actor.noticeUnhandledException);
        registerBehavior("BeginTermination", Actor.noticeBeginTermination);
        registerBehavior("UserTokenReply", Actor.noticeUserTokenReply);
        registerBehavior("ActorDidNotUnderstand", Actor.noticeActorDidNotUnderstand);
        registerBehavior("SecurityIncident", Actor.noticeSecurityIncident);
        registerBehavior("SelfVicinityCheckup", Actor.noticeActorVicinityHealthReport);
        registerBehavior("Acknowledgement", Actor.noticeAcknowledgement);
        registerBehavior("ActorProcessingError", Actor.noticeActorProcessingError);
        registerBehavior("ActorEntered", Actor.noticeActorEntered);
        registerBehavior("ActorResume", Actor.noticeActorResumeRequest);

        // subscribe to events
        vicinity.subscribe(this, name(), Actor::onApplicationEvent);   // subscribe to my event
        vicinity.subscribe(this, group(), Actor::onApplicationEvent);  // subscribe to a group event

        // notify all that the actor is getting ready to receive events.
        bulletinBoardStatus = new BulletinBoardStatus(
                BulletinBoardStatus.Status.Active,
                "status",
                "started");

        bulletinBoard.postStatusTopic(MY_VICINITY_STATUS, name(), "Active", bulletinBoardStatus.toString());

        learn();
    }

    /**
     * Determine if current Actor is a Security Guard
     * @return true if Security Guard
     */
    public boolean isSecurityGuard() {
        return this.name().contains("SecurityGuard");
    }

    /**
     * Cleanup resources
     */
    @PreDestroy
    protected void cleanup() {
        if (isOverriding) { // get other actors back to work
            announce( new ActorResumeEvent(this, name(), group())
                    .setAccessToken(SecurityGuard.getAccessToken())
                    .setJoinToken(JOIN_TOKEN)
                    .setOverrideToken(OVERRIDE_TOKEN)
            );
        }

        // notify Actors that this Actor is no longer available
        giveStatus(BulletinBoardStatus.Status.Gone, StringUtils.isNotEmpty(terminationReason) ? terminationReason : "ended normally");

        // stop receiving events
        vicinity.unsubscribe(name());
        vicinity.unsubscribe(group());
    }

    /**
     * Current Actor ID
     * @return unique ID
     */
    public String id() {
        return actorId;
    }

    /**
     * The Actor's group (such as Merchant)
     * @return the Actor's group
     */
    public String group() {
        return this.getClass().getSimpleName();
    }

    /**
     * The name as a combination
     * @return
     */
    public String name() {
        return new StringBuffer().append(group()).append(".")
                .append(project()).append(".")

                .append(generation()).append(".")
                .append(id()).toString();
    }

    /**
     * The project that the Actor is part of.  The default is 'default'.
     * @return
     */
    public String project() {
        return project;
    }

    /**
     * Assigns the project.
     * @param project
     * @return this Actor
     */
    protected Actor setProject(String project) {
        this.project = project;
        return this;
    }

    /**
     * Returns the generation. The default is 1.0.0.
     * @return
     */
    public String generation() {
        return generation;
    }

    /**
     * Sets the generation.
     * @param generation
     * @return this Actor
     */
    protected Actor setGeneration(String generation) {
        this.generation = generation;
        return this;
    }

    /**
     * Returns the Vicinity the Actor exists in.
     * @return
     */
    protected Vicinity vicinity() {return vicinity;}

    /**
     * Returns the shared lock resource.
     * @return the shared lock resource
     */
    protected Lock lock() {
        return lock;
    }

    /**
     * Returns the bulletin board
     * @return the bulletin board
     */
    protected BulletinBoard bulletinBoard() {
        return bulletinBoard;
    }

    /**
     * Returns the task list
     * @return the task list
     */
    protected Tasklist tasklist() {
        return tasklist;
    }

    /**
     * Returns the queue
     * @return the queue
     */
    protected Queue queue() { return queue; }

    /**
     * Returns the dynamic behavior
     * @return a behavior object
     */
    protected Behavior behavior() {
        return behavior;
    }

    /**
     * Returns the dynamic logic
     * @return a logic object
     */
    protected Logic logic() {return logic;}

    /**
     * Sets the health status of the Actor
     * @param vicinityHealthOk - true if the actor is healthy
     */
    protected void setVicinityHealthStatus(boolean vicinityHealthOk) {
        isVicinityHealthOk = vicinityHealthOk;
    }

    /**
     * Returns if the Actor is healthy
     * @return the actor status
     */
    protected boolean isVicinityHealthStatusOk() {
        return isVicinityHealthOk;
    }

    /**
     * Returns if the Actor is terminating
     * @return the actor status
     */
    public boolean isTerminating() {
        return isTerminating;
    }

    /**
     * Set the Actor state to terminating
     */
    protected void setAsTerminating() {
        this.isTerminating = true;
        this.setAsBusy(true);
    }

    /**
     * Determines if the Actor is busy
     * @return true if actor is busy
     */
    public boolean isBusy() {
        return isBusy;
    }

    /**
     * Set the busy status
     * @param status is true if Actor is busy
     */
    protected void setAsBusy(boolean status) {
        this.isBusy = status;
    }

    /**
     * Perform internal tasks such as, analysis, inspection and cleanup
     */
    @Scheduled(cron = "0 * * * * *")
    protected void think() {
        try {
            int duration = minutesIn.incrementAndGet();
            switch (duration) {
                case 30:
                    on30min();
                    break;
                case 60:
                    on30min();
                    on60min();
                    minutesIn.set(0);
                    int durationHours = hoursIn.incrementAndGet();
                    switch (durationHours) {
                        case 12:
                            on12hours();
                            break;
                        case 24:
                            on12hours();
                            on24hours();
                            hoursIn.set(0);
                    }
                    break;
                case 1:
                    if (!didOnce) { // perform one time tasks
                        doOnce();
                        didOnce = true;
                    }
                    on24hours();
                    break;
            }

            checkHealth();
            onThink();  // do proactive processing
        }
        catch (Exception e) {
            this.onException(new ActorException(this, e));
        }
    }

    /**
     * Learn new behaviors
     */
    protected void learn() {
        readNotes();
        learn(behavior.getBehaviors(this));
    }

    /**
     * Learn new bahaviors
     * @param optBehaviors - the behaviors to learn
     */
    protected void learn(Optional<Map<String, RestCall>> optBehaviors) {
        if (optBehaviors.isPresent()) {
            optBehaviors.get().entrySet().forEach(behavior -> {
                registerBehavior(behavior.getKey(), Behavior.onExternalBehavior);
            });
        }

        // override behavior handling if a script has been defined
        if (logic.load(this).isPresent()) {
            logic.getListeners().forEach(l -> {
                registerBehavior(l, Logic.onExternalLogic);
            });
        }
    }

    /**
     * Perform proactive tasks
     */
    protected void onThink() {
        // Perform periodic processing
        if (isMyTurn()) {
            doMyWork();
        }

        handleUnacknowledgedEvents();
        lookForWork();
    }

    /**
     * Do proactive work based on my role
     */
    protected void doMyWork() {
        // perform work based on a shared task list, etc.
    }

    /**
     * See if there are unacknowledged tasks to work on
     */
    protected void lookForWork() {
        var optEvent = todo.acquireAvailableTask(group(), name());
        if (optEvent.isPresent()) {
            whisper(optEvent.get().setAsToDoTask(true));
        }
    }

    /**
     * Process any unacknowledged tasks
     */
    protected void handleUnacknowledgedEvents() {
        recall(String.class, e -> {
            var val = e.getValue().toString();
            if (val.startsWith("Waiting:")) {
                log.warn("Still waiting for acknowledgement on event: %s", e.getKey());
                var index = e.getValue().toString().split(":")[1];
                var cleanKey = e.getKey().replace("requiresAck:", "").split("/");
                var requestId = cleanKey[0];
                var eventName = cleanKey[1];
                var from = cleanKey[2];
                var to = cleanKey[3];
                var event = ack.getUnacknowledgedEvent(requestId, index);

                if (event != null) {
                    onUnacknowledgedEvent(event); // potentially resend
                } else {
                    handleMissedAcknowledgedEvent(requestId, eventName, from, to);
                }

                return true;
            }
            return false;
        });
    }

    /**
     * Called when an unacknowledged event is found
     * @param event
     */
    protected void onUnacknowledgedEvent(ArchitectureFirstEvent event) {
        log.info("Unacknowleged Event: " + event.name());

        // see if missed acknowledgement
        if (convo.hasAcknowledged(event.getRequestId(), event.name(), event.from(), event.toFirst())) {
            String requestKey = generateAckRequestKey(event, event.name());
            var mem = recall(requestKey, String.class);
            if (mem.isPresent()) {
                memory.forget(requestKey, String.class);
            }
            return;
        }

        // Note: override this code to resend (say) event
    }

    /**
     * Handle unacknowledged events without the actual event
     * @param requestId
     * @param eventName
     * @param from
     * @param to
     * @return
     */
    protected boolean handleMissedAcknowledgedEvent(String requestId, String eventName, String from, String to) {
        // see if missed acknowledgement
        if (convo.hasAcknowledged(requestId, eventName, from, to)) {
            String requestKey = generateAckRequestKey(requestId, eventName, from, to);
            var mem = recall(requestKey, String.class);
            if (mem.isPresent()) {
                memory.forget(requestKey, String.class);
            }
            return true;
        }

        return false;
    }

    /**
     * Send a message to one Actor
     *
     * @param event (contains a "to" that points to a group, such as Merchant)
     * @return
     */
    public ArchitectureFirstEvent say(ArchitectureFirstEvent event,
                                      Function<ArchitectureFirstEvent, Boolean> fnReplyHandler,
                                      Function<ActorException, Boolean> fnErrorHandler) {

        // Add RequestID if not the security guard
        setRequestInfo(event);
        if (SecurityGuard.needsAnAccessToken(event) && !event.hasAccessToken()) {
            throw new ActorException(this, "jwtToken is null");
        }

        var callbackKey = getCallbackKey(event);
        if (fnReplyHandler != null) {
            callbacksByRequest.put(callbackKey, fnReplyHandler);
        }
        if (fnErrorHandler != null) {
            errorHandlersByRequest.put(callbackKey, fnErrorHandler);
        }

        AtomicBoolean anEventWasSent = new AtomicBoolean(false);

        var toList = event.to().stream().toList();
        toList.forEach(to -> {
            if (event.isLocal()) {
                event.setTo(name());
            }
                // Find an actor by name to send to (a.k.a. a connection)
            else if (!to.contains(".") && !to.equalsIgnoreCase(ArchitectureFirstEvent.EVENT_ALL_PARTICIPANTS)) {  // Don't do this if there is already an actor name there
                String actorName = vicinity.findActor(to, event.project());
                if (StringUtils.isEmpty(actorName)) {
                    log.error("Actor not found with for group: " + to);
                    announce(new ActorNotFoundEvent(this, name(), VICINITY_MONITOR, event).setParticipant(to));

                    if (event.shouldProcessLaterIfNoActorFound()) {
                        recordEventToDo(event);
                    }
                    if (errorHandlersByRequest.containsKey(callbackKey)) {
                        Function<? super ActorException, Boolean> fnCustom = errorHandlersByRequest.get(callbackKey);
                        var results = fnCustom.apply(new ActorNotFoundException("ACTOR_NOT_FOUND: " + to).setEvent(event));
                        errorHandlersByRequest.remove(callbackKey);
                    }
                    return;
                }
                event.setTo(actorName);

            } else {                                  // Verify that desired actor is available or choose another
                if (!to.equalsIgnoreCase(ArchitectureFirstEvent.EVENT_ALL_PARTICIPANTS) &&
                        !vicinity.actorIsAvailable(to)) {
                    String actorName = vicinity.findActor(to.substring(0, to.indexOf(".")), event.project());
                    if (StringUtils.isEmpty(actorName)) {
                        log.error("Actor not found with name: " + to);
                        announce(new ActorNotFoundEvent(this, name(), VICINITY_MONITOR, event).setParticipant(actorName));
                        return;
                    }
                }
            }

            var convoEntry = convo.record(event,
                    !event.isReply() ? Conversation.Status.Starting : Conversation.Status.Replying);

            if (!convoEntry.startsWith("ERROR") && !convoEntry.startsWith("WARNING")) {
                if (event.requiresAcknowledgement()) {
                    String requestKey = generateAckRequestKey(event, event.name());
                    remember(requestKey, String.format("Waiting:%s", event.index()), String.class);
                }
            }

            if (!event.isLocal() || (event.isLocal() && !event.wasHandled())) {
                publishEvent(event);
                anEventWasSent.set(true);
            }
        });

        if (anEventWasSent.get()) {
            if (!event.isReply()) { // don't block on reply or to do events
                if (event.awaitResponse() && !event.isPropagatedFromVicinity()) {
                    var lock = new Semaphore(0);
                    locksByRequest.put(callbackKey, lock);
                    try {
                        log.info("await for: " + callbackKey);
                        if (!lock.tryAcquire(event.awaitTimeoutSeconds(), TimeUnit.SECONDS)) {

                            convo.record(event, Conversation.Status.ErrorAfterReceivedByActor);
                            if (errorHandlersByRequest.containsKey(callbackKey)) {
                                Function<ActorException, Boolean> fnCustom = errorHandlersByRequest.get(callbackKey);
                                var results = fnCustom.apply(new ActorException(this, "AWAIT_TIMED_OUT"));
                                errorHandlersByRequest.remove(callbackKey);
                            }
                            if (event.shouldProcessLaterIfNoActorFound() && !event.isToDoTask()) {
                                recordEventToDo(event);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Interrupted thread: ", e);
                        throw new ActorException(this, e);
                    }
                }
            }
        }

        return event;
    }

    /**
     * Record an event to be handled later if the desired Actor cannot process it
     * @param event
     */
    private void recordEventToDo(ArchitectureFirstEvent event) {
        var unackIndex = ack.recordUnacknowledgedEvent(event);
        todo.addTask(event);
    }

    /**
     * Send an event to another Actor and handle the response
     * @param event
     * @param fnReplyHandler - the response handler
     * @return
     */
    public ArchitectureFirstEvent say(ArchitectureFirstEvent event, Function<ArchitectureFirstEvent, Boolean> fnReplyHandler) {
        return say(event, fnReplyHandler, null);
    }

    /**
     * Send an event to another Actor
     *      * @param event
     * @param event
     * @return
     */
    public ArchitectureFirstEvent say(ArchitectureFirstEvent event) {
        return say(event, null, null);
    }


    /**
     * Send an event only inside this process and not to the Vicinity.
     * @param event
     * @param fnReplyHandler - handles the response
     * @param fnErrorHandler - handles the error
     * @return
     */
    public ArchitectureFirstEvent whisper(ArchitectureFirstEvent event,
                                          Function<ArchitectureFirstEvent, Boolean> fnReplyHandler,
                                          Function<ActorException, Boolean> fnErrorHandler) {
        return say(event.setAsLocal(true).setAsProcessLaterIfNoActorFound(false), fnReplyHandler, fnErrorHandler);
    }

    /**
     * Send an event only inside this process and not to the Vicinity.
     * @param event
     * @param fnReplyHandler - handles the response
     * @return
     */
    public ArchitectureFirstEvent whisper(ArchitectureFirstEvent event, Function<ArchitectureFirstEvent, Boolean> fnReplyHandler) {
        return whisper(event, fnReplyHandler, null);
    }

    /**
     * Send an event only inside this process and not to the Vicinity.
     * @param event
     * @return
     */
    public ArchitectureFirstEvent whisper(ArchitectureFirstEvent event) {
        return whisper(event, null, null);
    }

    /**
     * Send a message to one or more groups, such as Merchant, Cashier
     * @param event (contains a "to" that points to a group, such as Merchant)
     * @return
     */
    public ArchitectureFirstEvent announce(ArchitectureFirstEvent event, Function<ArchitectureFirstEvent, Boolean> fnReplyHandler) {
        setRequestInfo(event);
        event.setAsAnnouncement(true);

        if (fnReplyHandler != null) {
            callbacksByRequest.put(getCallbackKey(event), fnReplyHandler);
        }
        publishEvent(event);

        return event;
    }

    /**
     * Send a message to one or more groups, such as Merchant, Cashier
     * @param event
     * @return
     */
    public ArchitectureFirstEvent announce(ArchitectureFirstEvent event) {
        return announce(event, null);
    }

    /**
     * Sets core request information to support event chaining
     * @param event
     */
    private void setRequestInfo(ArchitectureFirstEvent event) {
        // Add RequestID if not the security guard
        if (!isSecurityGuard()) {
            if (!event.hasRequestId()) {
                event.setRequestId(SecurityGuard.getRequestId());
                event.setOriginalEventName(event.name());
            }
            else if (event.hasRequestId() && "DefaultLocalEvent".equals(event.originalEventName())) {
                event.setOriginalEventName(event.name());
            }

            if (SecurityGuard.needsAnAccessToken(event)) {
                event.setAccessToken(SecurityGuard.getAccessToken());
            }
        }
    }


    /**
     * Creates an event to reply to the original event
     * @param event
     * @param fnReplyHandler - handles the response
     * @return
     */
    public ArchitectureFirstEvent reply(ArchitectureFirstEvent event,
                                        Function<ArchitectureFirstEvent, Boolean> fnReplyHandler) {
        var replyEvent = ArchitectureFirstEvent.fromForReply(this, name(), event);

        if (fnReplyHandler != null) {
            callbacksByRequest.put(getCallbackKey(event), fnReplyHandler);
        }

        return say(replyEvent);
    }

    /**
     * Creates an event to reply to the original event
     * @param event
     * @return
     */
    public ArchitectureFirstEvent reply(ArchitectureFirstEvent event) {
        return reply(event, null);
    }

    /**
     * Notice an event
     * @param event
     * @return
     */
    public Actor notice(ArchitectureFirstEvent event) {
        return hear(event);
    }

    /**
     * Recive an event an attempt to process it
     * @param event
     * @return this Actor
     */
    protected Actor hear(ArchitectureFirstEvent event) {
        try {
            event.setTargetActor(this);
            convo.record(event, Conversation.Status.ReceivedByActor);
            event.onVicinityInit(); // perform marshalling that Vicinity would

            String behaviorKey = event.name().replace("Event", "");
            // Perform internal processing first
            if (functions.containsKey(behaviorKey) || event.isPipelineEvent()) {
                event.setAsHandled(true);
                if (event.isToDoTask()) {
                    todo.completeTask(event);
                } else {
                    ack.recordAcknowledgement(event);
                }
                convo.record(event, Conversation.Status.AcknowledgedByActor);
                if (functions.containsKey(behaviorKey)) {
                    functions.get(behaviorKey).apply(event);
                }
            } else {
                var reply =
                        new ActorDidNotUnderstandEvent(this, name(), event.from())
                                .setUnansweredEvent(event)
                                .setMessage("Actor " + event.toFirst() + " did not respond to event " + event.name())
                                .setOriginalEvent(event);
                say(reply);                                     // notify original Actor
                announce(reply.setTo(VICINITY_MONITOR));        // notify monitor
            }

            if (!(event instanceof AcknowledgementEvent)) {
                // Perform one time processing
                String callbackKey = getCallbackKey(event);

                boolean responseIsComplete = false;
                if (!(event instanceof ErrorEvent)) {
                    if (callbacksByRequest.containsKey(callbackKey)) {
                        Function<ArchitectureFirstEvent, Boolean> fnCustom = callbacksByRequest.get(callbackKey);
                        responseIsComplete = fnCustom.apply(event);
                        if (responseIsComplete) {
                            callbacksByRequest.remove(callbackKey);
                        }
                    }
                } else {
                    if (errorHandlersByRequest.containsKey(callbackKey)) {
                        Function<ActorException, Boolean> fnCustom = errorHandlersByRequest.get(callbackKey);
                        responseIsComplete = fnCustom.apply(new ActorException(this, event));
                        if (responseIsComplete) {
                            errorHandlersByRequest.remove(callbackKey);
                        }
                    }
                    if (event.isToDoTask()) {
                        todo.failTask(event);
                    }
                }

                if (responseIsComplete) {
                    if (locksByRequest.containsKey(callbackKey)) {
                        locksByRequest.get(callbackKey).release();
                        locksByRequest.remove(callbackKey);
                    }
                }
            }

            return this;
        } catch (Exception e) {
            remember("exception occurred", e);
            log.error("Error: ", e);
            onException(event, new ActorException(this, e));
        }

        return this;
    }

    /**
     * The default behavior for dynamic processing
     */
    // Default response. Replace with a specific one in derived class
    Function<ArchitectureFirstEvent, ArchitectureFirstEvent> fnDefaultReplyBehavior = (event -> {
        event.setMessage("replying to original event");
        return event;
    });

    /**
     * Register dynamic behavior
     * @param name
     * @param behavior
     */
    protected void registerBehavior(String name, Function<ArchitectureFirstEvent, Actor> behavior) {
        functions.put(name, behavior);
    }

    /**
     * Request new token from the identity provider
     * @param userToken
     */
    protected void requestNewToken(UserToken userToken) {
        String actorName = vicinity.findActor("IdentityProvider");
        say(new UserTokenRequestEvent(this, name(), actorName).setUserToken(userToken));
    }

    /**
     * Remember a fact for period of time
     * @param name
     * @param value
     * @return
     * @param <T>
     */
    // ex.   remember("a", 24);
    protected <T> Actor remember(String name, T value) {
        memory.store(name, value, value.getClass());
        return this;
    }

    /**
     * Remember a fact for a period of time by type
     * @param name
     * @param value
     * @param classType
     * @return
     * @param <T>
     */
    protected <T> Actor remember(String name, T value, Type classType) {
        memory.store(name, value, classType);
        return this;
    }

    /**
     * Remember an occurrence of a fact
     * @param name
     * @param value
     * @return
     * @param <T>
     */
    protected <T> Actor rememberOccurrence(String name, T value) {
        memory.storeOccurrence(name, value, value.getClass());
        return this;
    }

    /**
     * Remember an occurrence of a fact by type
     * @param name
     * @param value
     * @param classType
     * @return
     * @param <T>
     */
    protected <T> Actor rememberOccurrence(String name, T value, Type classType) {
        memory.storeOccurrence(name, value, classType);
        return this;
    }

    /**
     * Recall a fact memorized earlier by type
     * @param name
     * @param classType
     * @return
     * @param <T>
     */
    // ex. recall("a", Integer.class);
    protected <T> Optional<T> recall(String name, Class<T> classType) {
        return memory.retrieve(name, classType);
    }

    /**
     * Recall a fact memorized earlier
     * @param name
     * @return
     */
    // recall("int");
    protected Optional<Object> recall(String name) {
        return memory.retrieve(name);
    }

    /**
     * Recall facts that pass conditions.
     * @param classType
     * @param fnFilter
     * @return
     */
    // ex. recall("a", Integer.class);
    protected List<Map.Entry<String, Object>> recall(Type classType, Predicate<Map.Entry<String, Object>> fnFilter) {
        return memory.retrieve(classType, fnFilter);
    }

    /**
     * Post an entry to the bulletin board
     * @param name
     * @param value
     * @return
     */
    protected Actor post(String name, String value) {
        bulletinBoard.post(name, value);
        return this;
    }

    /**
     * Post a fact to a topic on a bulletin board
     * @param topic
     * @param name
     * @param value
     * @return
     */
    protected Actor postTopic(String topic, String name, String value) {
        bulletinBoard.postStatusTopic(topic, name, "Active", value);
        return this;
    }

    /**
     * Read an item from the bulletin board
     * @param name
     * @return
     */
    protected String read(String name) {
        return bulletinBoard.read(name);
    }

    /**
     * Read an item from a topic on the bulletin board
     * @param topic
     * @param name
     * @return
     */
    protected String readTopic(String topic, String name) {
        return bulletinBoard.readTopicEntry(topic, name);
    }

    /**
     * Read a number of entries from a topic on the bulletin board
     * @param topic
     * @param name
     * @return
     */
    protected Map<String, String> readTopicEntries(String topic, String name) {
        return bulletinBoard.readTopicEntries(topic, name);
    }

    /**
     * Publish an event to the Vicinity or process
     * @param event
     */
    protected void publishEvent(ArchitectureFirstEvent event) {
        log.info("Publishing event: " + event);
        event.setAsHandled(false);
        vicinity.onApplicationEvent(event);
    }

    /**
     * Helper method to see if a list contains any entries
     * @param list
     * @return
     */
    protected boolean exists(List<? extends Object> list) {
        return list.size() > 0;
    }

    /**
     * Check the health of the Agent and the Vicinity.
     * Terminate if unhealthy
     */
    protected void checkHealth() {
        if (!isHealthOk()) {
            onTerminate("self health not ok for: " + name());
        }

        if (!isVicinityHealthOk()) {
            onTerminate("vicinity connections are stale for: " + name());
        }

        if (!isEnvironmentOk()) {
            onTerminate("environment is invalid for: " + name());
        }

        giveStatus((!isAway)
                ? BulletinBoardStatus.Status.Active
                : BulletinBoardStatus.Status.Away, "running");
    }

    /**
     * Provide status to the bulletin board in order to accept events
     * @param status
     * @param message
     */
    private void giveStatus(BulletinBoardStatus.Status status, String message) {
        bulletinBoardStatus.setStatus(status);
        bulletinBoardStatus.setMessage(message);
        bulletinBoardStatus.touch();
        bulletinBoard.postStatusTopic(MY_VICINITY_STATUS, name(), status.name(), bulletinBoardStatus.toString());
    }

    /**
     * Determine if it is this Actor's turn to do a task
     * @return true if it is this actors turn
     */
    protected boolean isMyTurn() {
        return name().equals(bulletinBoard.whosTurnIsIt(MY_VICINITY_STATUS));
    }

    /**
     * Overridable method to check Actor's health
     * @return true if the Actor is healthy
     */
    protected boolean isHealthOk() {
        return true;
    }

    /**
     * Overridable method to check Actor's health regarding the Vicinity
     * @return true if the Vicinity is healthy
     */
    protected boolean isVicinityHealthOk() {

        if (isVicinityHealthStatusOk()) {
            if (RuntimeUtils.isInDebugger()) {  // don't let breakpoints make Actor look unhealthy
                return true;
            }

            vicinityHeathCheckStartTime = ZonedDateTime.now(ZoneId.of("GMT"));
            say(new SelfVicinityCheckupEvent(this));
        }

        // self and Check if pub/sub connection is ok
        return isVicinityHealthStatusOk() &&
                vicinity.areConnectionsOk(name(), NUM_CONNECTIONS) &&
                vicinity.areConnectionsOk(group(), NUM_CONNECTIONS);
    }

    /**
     * Overridable method to check the environment's status
     * @return true if the environment is healthy
     */
    protected boolean isEnvironmentOk() {
        // note: this should be done with database also in derived class (shelf.isOk)
        return bulletinBoard.isOk();
    }

    /**
     * Performs cleanup on graceful termination
     * @param reason the reason for terminating
     */
    protected void onTerminate(String reason) {
        setAsTerminating();
        if (StringUtils.isNotEmpty(reason)) {
            log.info(reason);
            terminationReason = reason;
        }

        if (!isSecurityGuard()) {  // let security guard and others clean up first
            whisper(new BeginTerminationEvent(this, name(), ArchitectureFirstEvent.EVENT_ALL_PARTICIPANTS));
        }
        cleanup();

        if (isSecurityGuard()) {  // let other actors clean up next
            whisper(new BeginTerminationEvent(this, name(), ArchitectureFirstEvent.EVENT_ALL_PARTICIPANTS));
        }
    }

    /**
     * Perform processing one time when the Actor becomes available the first time
     */
    protected void doOnce() {
        if (!this.isSecurityGuard()) {
            handleOverriding();
        }
        onDoOnce();
    }

    /**
     * Perform the one time action
     */
    protected void onDoOnce() {
        //... override
    }

    /**
     * Support overriding other Actors in the same group and receive all events
     */
    protected void handleOverriding() {
        JOIN_TOKEN = System.getenv("JOIN_TOKEN");
        OVERRIDE_TOKEN = System.getenv("OVERRIDE_TOKEN");
        OVERRIDE_ACCESS_TOKEN = System.getenv("OVERRIDE_ACCESS_TOKEN");

        if (StringUtils.isNotEmpty(OVERRIDE_TOKEN)) {
            log.warn(String.format("Actor %s is attempting to override actors in group %s", name(), group()));
        }

        announce(new ActorEnteredEvent(this, name(), group())
                .setAccessToken(OVERRIDE_ACCESS_TOKEN)
                .setJoinToken(JOIN_TOKEN)
                .setOverrideToken(OVERRIDE_TOKEN)
        );
    }

    /**
     * Setup a project to handle directed events
     */
    protected void setupProjectIfExists() {
        var assignedProject = System.getenv(PROJECT);
        if (StringUtils.isNotEmpty(assignedProject)) {
            project = assignedProject;
        }
    }

    /**
     * Perform processing every 30 minutes
     */
    protected void on30min() {
        // override for specific behavior
        if (logic.existsFor30min()) {
            applyLogic("30min");
        }
    }

    /**
     * Perform processing every hour
     */
    protected void on60min() {
        recordNotes();
        if (logic.existsFor60min()) {
            applyLogic("60min");
        }
    }

    /**
     * Perform processing every 12 hours
     */
    protected void on12hours() {
        // override for specific behavior
        if (logic.existsFor12hours()) {
            applyLogic("12hours");
        }
    }

    /**
     * Perform processing every 24 hours
     */
    protected void on24hours() {
        // override for specific behavior
        if (logic.existsFor24hours()) {
            applyLogic("24hours");
        }
    }

    /**
     * Apply desired dynamic lagic
     * @param s - name of desired logic
     */
    private void applyLogic(String s) {
        logic.apply(s, new DynamicActorEvent(this, name(), name())
                .setTargetActor(this)
                .setAccessToken(SecurityGuard.getAccessToken()));
    }

    /**
     * Perform processing on acknowledgement of an event
     */
    protected void onAcknowledgement(AcknowledgementEvent event) {
        // override for specific behavior
        log.info(String.format("Acknowledged event: %s/%s from: %s to: %s", event.getAcknowledgedEventName(), event.getRequestId(), event.from(), event.toFirst()));
        if (event.requiresAcknowledgement()) {
            String requestKey = generateAckRequestKey(event.getAcknowledgedEvent(), event.getAcknowledgedEventName());
            var mem = recall(requestKey, String.class);
            if (mem.isPresent()) {
                memory.forget(requestKey, String.class);
            }
        }
    }

    /**
     * Perform processing on actor processing error
     */
    protected void onActorProcessingError(ActorProcessingErrorEvent event) {
        // override for specific behavior
        var message = String.format("Received error for event: %s/%s from: %s to: %s", event.originalEventName(), event.getRequestId(), event.from(), event.toFirst());
        log.info(message);
        remember("latestError", message);
    }

    /**
     * Generate a request key for an acknowledgement
     * @param acknowledgedEvent - event to acknowledge
     * @param acknowledgedEventName - name of event
     * @return request key
     */
    private String generateAckRequestKey(ArchitectureFirstEvent acknowledgedEvent, String acknowledgedEventName) {
        return generateAckRequestKey(acknowledgedEvent.getRequestId(), acknowledgedEventName,
                acknowledgedEvent.from(), acknowledgedEvent.toFirst());
    }

    /**
     * enerate a request key for an acknowledgement
     * @param requestId - request id of the event
     * @param eventName - name of the event
     * @param from - event creator
     * @param to - event target consumer
     * @return requst key
     */
    private String generateAckRequestKey(String requestId, String eventName, String from, String to) {
        return String.format("requiresAck:%s/%s/%s/%s", requestId, eventName, from, to);
    }

    /**
     * Record notes from the Actor's memory.
     * Note: Override to modify behavior
     */
    protected void recordNotes() {
        var memories = this.provideBrainDump();
        Notes notes = new Notes(this.name()).addNotes(memories);
        bulletinBoard.postTopic(MY_ACTOR_NOTES, name(), notes.toJson());
    }

    /**
     * Read existing notes into the Actor's memory
     */
    protected void readNotes() {
        var contents = bulletinBoard.readRandomTopicEntry(MY_ACTOR_NOTES);
        if (hasNotes(contents)) {
            Notes notes = new Gson().fromJson(contents, Notes.class);
            notes.getEntries().entrySet().forEach(n -> {
                try {
                    var clazz = Class.forName(n.getKey());
                    Map<String, Object> entries = new HashMap<>();
                    var ents = (Map<String, Object>) n.getValue();
                    ents.forEach((k, v) -> {
                        if (n.getValue() != null && !((Map<?, ?>) n.getValue()).containsValue("FORGOTTEN")) {
                            Map<String, Object> cache = (Map<String, Object>) ents.get(k);
                            cache.entrySet().forEach(a -> {
                                remember(a.getKey(), a.getValue(), clazz);
                            });
                        }
                    });
                } catch (ClassNotFoundException e) {  // Loading previous notes is a "nice to have" and should not stop the Actor
                    log.error("Cannot remember data due to error: ", e);
                }
            });
        }
    }

    /**
     * Determines if the content has notes
     * @param contents JSON string of notes
     * @return true if there are notes
     */
    protected boolean hasNotes(String contents) {
        return StringUtils.isNotEmpty(contents) && !contents.equals("{}");
    }

    /**
     * Gather all facts in the Agent's memory
     * @return a map of facts
     */
    protected Map<String, Object> provideBrainDump() {
        return memory.dump();
    }

    /**
     * Returns the key that matches the callback of an event
     * @param event - the event to provide a callback for
     * @return the callback key
     */
    private String getCallbackKey(ArchitectureFirstEvent event) {
        return event.getRequestId() + "/" + ((event.hasOriginalEventName()) ? event.originalEventName() : event.name());
    }

    /**
     * Perform an action when an actor of the same group enters the Vicinity
     * @param event
     */
    public void onActorEntered(ActorEnteredEvent event) {
        // ... override for different behavior
        if (StringUtils.isNotEmpty(JOIN_TOKEN) && JOIN_TOKEN.equals(event.getJoinToken())) {
            if (StringUtils.isNotEmpty(OVERRIDE_TOKEN) && OVERRIDE_TOKEN.equals(event.getOverrideToken())) { // allow another Actor to take the work
                log.warn(String.format("Actor %s is standing down due to override request from %s", name(), event.from()));
                giveStatus(BulletinBoardStatus.Status.Away, "standing down");
                isAway = true;
            }
        }
    }

    /**
     * Resume activities if standing down
     * @param event
     */
    public void onActorResumeRequest(ActorResumeEvent event) {
        // ... override for different behavior
        if (StringUtils.isNotEmpty(JOIN_TOKEN) && JOIN_TOKEN.equals(event.getJoinToken())) {
            if (StringUtils.isNotEmpty(OVERRIDE_TOKEN) && OVERRIDE_TOKEN.equals(event.getOverrideToken())) { // allow another Actor to take the work
                log.warn(String.format("Actor %s is resuming down due to request from %s", name(), event.from()));
                giveStatus(BulletinBoardStatus.Status.Active, "running");
                isAway = false;
            }
        }
    }

    /**
     * Perform an action if an event is not handled
     * @param event
     */
    protected void onUnhandledEvent(ArchitectureFirstEvent event) {
        // .. override to handle
    }

    /**
     * Potentially perform an action if an event is directed to this Actor
     * @param event
     */
    public static Void onApplicationEvent(Actor actor, ArchitectureFirstEvent event) {
        if (event.isLocal() || actor != event.getSource()) {    // do not receive an event from self unless a whisper
            event.to().forEach(t -> {
                if (StringUtils.isNotEmpty(t)) {
                    if (t.equalsIgnoreCase(actor.name())    // message is targeted to this actor
                            || t.equalsIgnoreCase(actor.getClass().getSimpleName()) // message is targeted to any actor in group
                            || t.equalsIgnoreCase(ArchitectureFirstEvent.EVENT_ALL_PARTICIPANTS)) { // event is targeted to any actor
                        if (!event.isAnnouncement() || (event.isAnnouncement() && !event.from().equals(actor.name()))) {
                            log.info("Receiving event: " + new Gson().toJson(event));
                            actor.hear(event);
                        }
                    }
                } else {
                    log.error("ERROR", new ActorException(event.getTarget().get(), "to: is empty on the message"));
                    // TODO - handle errors globally
                    //throw new ActorException("to: is empty on the message");
                }
            });
        }

        return null;
    }

    /**
     * Perform an external action based on an event
     * @param event
     * @return
     */
    public ArchitectureFirstEvent onExternalBehavior(ArchitectureFirstEvent event) {
        if (behavior.contains(event) || event.isPipelineEvent()) {
            var results =  behavior().perform(event);
            if (results.isPresent()) {
                var entry = new PipelineEntry(results.get().getClass().getName(), results.get());
                event.payload().put("results", entry);

                return event;
            }
        }
        return null;
    }

    /**
     * Apply external logic based on an event
     * @param event
     * @return
     */
    public ArchitectureFirstEvent onExternalLogic(ArchitectureFirstEvent event) {
        logic.apply(event);
        return event;
    }

    /**
     * Perform an action based on an exception
     * @param event - event that was being processed
     * @param exception - exception that was caught
     */
    public void onException(ArchitectureFirstEvent event, ActorException exception) {
        onException(event, exception, null);
    }

    /**
     * Perform an action based on an exception
     * @param exception - exception that was caught
     */
    public void onException(ActorException exception) {
        onException(null, exception);
    }

    /**
     * Perform an action based on an exception
     * @param event - event that was being processed
     * @param exception - exception that was caught
     * @param message - custom message related to the exception
     */
    public void onException(ArchitectureFirstEvent event, ActorException exception, String message) {
        if (event != null) {
            if (event.toFirst().equals(name())) {
                var errorEvent = ActorProcessingErrorEvent.from(this,  event, exception);
                errorEvent.setMessage(message);
                log.error(errorEvent.message());

                convo.record(event, Conversation.Status.ErrorAfterReceivedByActor);
                say(errorEvent);
                return;
            }
        }

        var reportEvent = new ActorProcessingErrorEvent(this, name(), VICINITY_MONITOR)
                .setException(exception);
        log.error(message, exception);
        announce(reportEvent);
    }

    public void onException(ActorException exception, String message) {
        onException(null, exception, message);
    }

    public void onError(String message) {
        log.error(message, this);
    }

    /**
     * Handle reply of user token
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeUserTokenReply = (event -> {
        UserTokenReplyEvent evt = (UserTokenReplyEvent) event;
        var actor = event.getTarget().get();

        UserToken userToken = evt.getCustomerToken();
        if (userToken.isRejected()) {
            log.info("Customer Token is rejected for: ", userToken.getUserId());
            return actor;
        }

        actor.rememberOccurrence("customerToken", userToken);
        return actor;
    });

    /**
     * Handle event when an Actor of the same group has entered the Vicinity
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeActorEntered = (event -> {
        var evt = (ActorEnteredEvent) event;

        Actor actor = event.getTarget().get();
        actor.onActorEntered(evt);

        return event.getTarget().get();
    });

    /**
     * Handle event when an actor that is idle can begin to accept events
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeActorResumeRequest = (event -> {
        var evt = (ActorResumeEvent) event;

        Actor actor = event.getTarget().get();
        actor.onActorResumeRequest(evt);

        return event.getTarget().get();
    });

    /**
     * Handle the situation when the connection to the Vicinity is broken
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeVicinityConnectionBroken = (event -> {
        VicinityConnectionBrokenEvent evt = (VicinityConnectionBrokenEvent) event;

        // Resubscribe
        evt.getVicinity().subscribe(evt.getTarget().get(), evt.getTargetOwner(), Actor::onApplicationEvent);

        return event.getTarget().get();
    });

    /**
     * Handle the situation when an exception was not gracefully handled by another Actor during a conversation
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeUnhandledException = (event -> {
        UnhandledExceptionEvent evt = (UnhandledExceptionEvent) event;

        evt.getTarget().get().remember("exception occurred", evt.getException());

        return event.getTarget().get();
    });

    /**
     * Handle the situation when a target Actor did not understand the event sent by this actor during a conversation
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeActorDidNotUnderstand = (event -> {
        ActorDidNotUnderstandEvent evt = (ActorDidNotUnderstandEvent) event;

        log.warn("Actor did not understand event", evt.getMessage());
        Actor actor = event.getTarget().get();
        actor.onUnhandledEvent(event);

        return actor;
    });

    /**
     * Handle the results of the Actor's Vicintiy health check
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeActorVicinityHealthReport = (event -> {
        Actor actor = event.getTarget().get();
        actor.setVicinityHealthStatus(Duration.between(actor.vicinityHeathCheckStartTime, ZonedDateTime.now(ZoneId.of("GMT"))).getSeconds() < actor.expirationSecondsOnVicintyHealthCheck);

        return actor;
    });

    /**
     * Receive the acknowledgment of a previous event sent to a target Actor during a conversation
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeAcknowledgement = (event -> {
        Actor actor = event.getTarget().get();
        actor.onAcknowledgement((AcknowledgementEvent) event);

        return actor;
    });

    /**
     * Handle the situation when there is an error processing an event by the target Actor during a conversation
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeActorProcessingError = (event -> {
        Actor actor = event.getTarget().get();
        actor.onActorProcessingError((ActorProcessingErrorEvent) event);

        return actor;
    });

    /**
     * Handle a security incident, such as an event rejected due to an expired token
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeSecurityIncident = (event -> {
        var evt = (SecurityIncidentEvent) event;

        log.warn("Actor had a security incident", evt.getMessage());

        Actor actor = event.getTarget().get();
        return actor;
    });

    /**
     * Handle the request to begin termination of the Actor
     */
    protected static Function<ArchitectureFirstEvent, Actor> noticeBeginTermination = (event -> {
        BeginTerminationEvent evt = (BeginTerminationEvent) event;

        var actor = evt.getTarget().get();
        if (!actor.isTerminating()) {
            actor.onTerminate("complied with termination request");
        }

        return actor;
    });

    /**
     * Utility method to convert an object to a map
     * @param m - the object to convert
     * @return a map of all entries
     */
    protected Map<String, ? extends Object> convertToMap(Object m) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(m);
        return gson.fromJson(jsonString, new HashMap<String, Object>().getClass());
    }
}
