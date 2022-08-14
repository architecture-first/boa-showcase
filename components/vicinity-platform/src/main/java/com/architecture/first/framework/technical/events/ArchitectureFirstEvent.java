package com.architecture.first.framework.technical.events;

import com.architecture.first.framework.business.actors.Actor;
import com.architecture.first.framework.business.vicinity.events.ErrorEvent;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import com.architecture.first.framework.security.SecurityGuard;
import com.architecture.first.framework.technical.util.SimpleModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.arch.Processor;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The core event for communication in the Vicinity and in process
 */
@Slf4j
public class ArchitectureFirstEvent extends ApplicationEvent {
    public static final String REQUEST_ID = "requestId";
    public static final String DEFAULT_PROJECT = "default";
    public static final String ORIGINAL_EVENT_NAME = "originalEventName";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String CUSTOMER_INFO = "customerInfo";
    public static final String TOKEN = "token";
    public static final String JWT_TOKEN = "jwtToken";
    public static final String BOA_CONN = "boa-conn";
    public static final String BOA_PROJECT = "boa-project";
    public static String EVENT_ALL_PARTICIPANTS = "all";

    private String name = "ArchitectureFirstEvent";
    private String type = "AnonymousOkEvent";
    private SimpleModel header = new SimpleModel();
    private SimpleModel payload = new SimpleModel();
    private String message = "";
    private transient Optional<Actor> target = Optional.empty();
    private transient Gson gson;
    private transient Configuration gsonConfig;
    private boolean isPropagatedFromVicinity = false;
    private boolean isLocalEvent = false;
    private boolean isAnnouncement = false;
    private boolean wasHandled = false;
    private boolean awaitResponse = false;
    private long awaitTimeoutSeconds = 30;
    private boolean isPipelineEvent = false;
    private boolean hasErrors = false;
    private boolean isReply = false;
    private boolean requiresAcknowledgement = false;
    private boolean isToDoTask = false;
    private String toDoLink = "";
    private boolean processLaterIfNoActorFound = true;
    private String originalActorName = "";
    private String tasklist = "";
    private long index = 0;

    /**
     * Create an event
     * @param source
     * @param name
     * @param from
     * @param to
     * @param originalEvent
     */
    public ArchitectureFirstEvent(Object source, String name, String from, List<String> to, ArchitectureFirstEvent originalEvent) {
        super(source);
        this.name = name;
        header.put(FROM, from);
        header.put(TO, to);
        if (originalEvent != null) {
            setOriginalEvent(originalEvent);
        }
        else {
            setRequestId(SecurityGuard.getRequestId());
            setOriginalEventName(name());
        }
    }

    /**
     * Create an event
     * @param source
     * @param name
     * @param from
     * @param to
     */
    public ArchitectureFirstEvent(Object source, String name, String from, List<String> to) {
        this(source, name, from, to, null);
    }


    /**
     * Create an event
     * @param source
     * @param eventToReplyTo
     */
    public ArchitectureFirstEvent(Object source, ArchitectureFirstEvent eventToReplyTo) {
        this(source, eventToReplyTo.name(), eventToReplyTo.toFirst(), eventToReplyTo.from(), eventToReplyTo);
    }

    /**
     *Create an event
     * @param source
     * @param name
     * @param from
     * @param to
     */
    public ArchitectureFirstEvent(Object source, String name, String from, String to) {
        this(source, name, from, new ArrayList<String>(Collections.singletonList(to)), null);
    }

    /**
     * Create an event
     * @param source
     * @param name
     * @param from
     * @param to
     * @param originEvent
     */
    public ArchitectureFirstEvent(Object source, String name, String from, String to, ArchitectureFirstEvent originEvent) {
        this(source, name, from, new ArrayList<String>(Collections.singletonList(to)), originEvent);
    }

    /**
     * Set the Actor that the event targets once it arrives in the desired process
     * @param target
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setTargetActor(Actor target) {
        this.target = Optional.of(target);
        return this;
    }

    /**
     * Clear the target entry
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent resetTargetActor() {
        this.target = Optional.empty();
        return this;
    }

    /**
     * Returns the target actor
     * @return Optional
     */
    public Optional<Actor> getTarget() {return target;}

    /**
     * Returns whether there is a target actor
     * @return boolean - true if a target actor exists
     */
    public boolean hasTargetActor() {return target != null && target.isPresent();}

    /**
     * Returns the type of the event
     * @return
     */
    public String type() {return type;}

    /**
     * Returns whether the event is a certain name
     * @return
     */
    public boolean isNamed(String name) {return this.name.equals(name) || this.getClass().getSimpleName().equals(name);}


    public boolean isType(Type type) {
        return this.type.equals(type.getTypeName());
    }

    /**
     * Returns the name of the event
     * @return
     */
    public String name() {return StringUtils.isNotEmpty(name) ? name : getClass().getSimpleName();}

    /**
     * Returns the subject of the event
     * @return
     */
    public String subject() {return name().replace("Event","");}

    /**
     * Sets the optional project. Default is 'default'
     * @param project
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setProject(String project) {
        this.header().put(BOA_PROJECT, project);
        return this;
    }

    /**
     * Returns the project
     * @return
     */
    public String project() {return (String) this.header().get(BOA_PROJECT);}

    /**
     * Returns the source of the event
     * @return
     */
    public String from() {return (String) header.get(FROM);}

    /**
     * Returns the target names and/or groups of the event
     * @return
     */
    public List<String> to() {return (List<String>) header.get(TO);}

    /**
     * Returns the first target name or group
     * @return
     */
    public String toFirst() {return ((List<String>) header.get(TO)).get(0);}

    /**
     * Returns the first group
     * @return
     */
    public String toFirstGroup() {return ((List<String>) header.get(TO)).get(0).split("\\.")[0];}

    /**
     * Sets the target name or group
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setTo(String name) {
        header.put(TO, new ArrayList<String>());
        ((List<String>) header.get(TO)).add(name);
        return this;
    }

    /**
     * Sets the source name
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setFrom(String name) {
        header.put(FROM, name);
        return this;
    }

    /**
     * Sets the source.
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setSource(String name) {
        source = name;
        return this;
    }

    /**
     * Returns the source
     * @return
     */
    public Object source() {return source;}

    /**
     * Returns the header
     * @return
     */
    public SimpleModel header() {if (header == null) {header = new SimpleModel();} return header;}

    /**
     * Returns the payload
     * @return
     */
    public SimpleModel payload() {if (payload == null) {payload = new SimpleModel();} return payload;}

    /**
     * Set true if the event has arrived external to the process via the Vicinity
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setPropagatedFromVicinity(boolean status) {
        isPropagatedFromVicinity = status;
        return this;
    }

    /**
     * Returns if the event has arrived external to the process via the Vicinity
     * @return
     */
    public boolean isPropagatedFromVicinity() {return isPropagatedFromVicinity;}

    /**
     * Sets the event as one that will not be sent through the Vicinity and will stay in process
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsLocal(boolean status) {
        isLocalEvent = status;
        return this;
    }

    /**
     * Returns if the event will not be sent through the Vicinity and will stay in process
     * @return
     */
    public boolean isLocal() {return isLocalEvent;}

    /**
     * Sets the event as an announcement type that will be sent to a group of Actors
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsAnnouncement(boolean status) {
        isAnnouncement = status;
        return this;
    }

    /**
     * Returns if the event is an announcement type
     * @return
     */
    public boolean isAnnouncement() {return isAnnouncement;}

    /**
     * Sets the event as handled so it is no longer propagated
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsHandled(boolean status) {
        wasHandled = status;
        return this;
    }

    /**
     * Returns if the event was handled
     * @return
     */
    public boolean wasHandled() {return wasHandled;}

    /**
     * Sets the event as a pipeline event for dynamic processing
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsPipelineEvent(boolean status) {
        isPipelineEvent = status;
        return this;
    }

    /**
     * Returns if the event is a pipeline event
     * @return
     */
    public boolean isPipelineEvent() {return isPipelineEvent;}

    /**
     * Sets the task as a TO-DO task for processing later
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsToDoTask(boolean status) {
        isToDoTask = status;
        return this;
    }

    /**
     * Returns if the task is a TO-DO task
     * @return
     */
    public boolean isToDoTask() {return isToDoTask;}

    /**
     * Sets a link between the TO-DO task and the event
     * @param toDoLink
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setToDoLink(String toDoLink) {
        this.toDoLink = toDoLink;
        return this;
    }

    /**
     * Returns the TO-DO task link
     * @return TO-DO link string
     */
    public String getToDoLink() {return toDoLink;}

    /**
     * Set if should process later or allow event to be unhandled
     * @param status true if should process later
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsProcessLaterIfNoActorFound(boolean status) {
        processLaterIfNoActorFound = status;
        return this;
    }

    /**
     * Returns process later status
     * @return true if should process later
     */
    public boolean shouldProcessLaterIfNoActorFound() {return processLaterIfNoActorFound;}

    /**
     * Set the original Actor name for tracking
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setOriginalActorName(String name) {
        originalActorName = name;
        return this;
    }

    /**
     * Returns the original actor name for tracking
     * @return
     */
    public String originalActorName() {return originalActorName;}

    /**
     * Set the task list name
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setTasklist(String name) {
        tasklist = name;
        return this;
    }

    /**
     * Returns the associated task list name
     * @return
     */
    public String tasklist() {return tasklist;}

    /**
     * Sets the mode as reply
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setIsReply(boolean status) {
        isReply = status;
        return this;
    }

    /**
     * Returns if the mode is reply
     * @return true if is a reply
     */
    public boolean isReply() {return isReply;}

    /**
     * Sets the event to require acknowledgement
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAsRequiresAcknowledgement(boolean status) {
        requiresAcknowledgement = status;
        return this;
    }

    /**
     * Returns status for requiring acknowledgment
     * @return true if requires acknowledgement
     */
    public boolean requiresAcknowledgement() {return requiresAcknowledgement;}

    /**
     * Sets whether the event or related processing has errors
     * @param status
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setHasErrors(boolean status) {
        hasErrors = status;
        return this;
    }

    /**
     * Returns whether the event or related processing has errors
     * @return true if the event or related processing has errors
     */
    public boolean hasErrors() {return hasErrors;}

    /**
     * Returns whether the event or related processing has errors
     * @return true if the event or related processing has errors
     */
    public boolean isErrorEvent() {return "ErrorEvent".equals(type) || this instanceof ErrorEvent;}

    /**
     * Returns the index that the event is by order in the UnAck (unacknowledged) or Ack (acknowledged) event list
     * @return index
     */
    public long index() {
        return index;
    }

    /**
     * Sets the index that the event is by order in the UnAck (unacknowledged) or Ack (acknowledged) event list
     */
    public void setIndex(long index) {
        this.index = index;
    }

    /**
     * Returns whether the caller will await response of this event for callback purposes
     * @return true if should await response
     */
    public boolean awaitResponse() {return awaitResponse;}

    /**
     * Sets whether the caller will await response of this event for callback purposes
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent shouldAwaitResponse(boolean status) {
        this.awaitResponse = status;
        return this;
    }

    /**
     * Returns the duration of time in seconds the caller will await response of this event
     * @return the length of time in seconds to wait
     */
    public long awaitTimeoutSeconds() {return this.awaitTimeoutSeconds;}

    /**
     * Sets the duration of time in seconds the caller will await response of this event
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAwaitTimeoutSeconds(long seconds) {
        this.awaitTimeoutSeconds = seconds;
        return this;
    }

    /**
     * Sets the access token
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setAccessToken(String jwtToken) {header.put(JWT_TOKEN, jwtToken); return this;}

    /**
     * Returns the access token
     * @return access token
     */
    public String getAccessToken() {return (String) header.get(JWT_TOKEN);}

    /**
     * Returns whether the event contains a token
     * @return access token
     */
    public Boolean hasAccessToken() {return header.containsKey(JWT_TOKEN) && header.get(JWT_TOKEN) != null;}

    /**
     * Sets the processed access token, which is post validation
     */
    public void setProcessedJwtToken(String jwtToken) {((Map<String,Object>)payload.get(TOKEN)).put(TOKEN, jwtToken);}

    /**
     * Returns the processed access token, which is post validation
     * @return processed access token
     */
    public String getProcessedJwtToken() {return (String) ((Map<String,Object>)payload.get(TOKEN)).get(TOKEN);}

    /**
     * Adds payload for the event
     * @param payload
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent addPayload(SimpleModel payload) {this.payload = payload; return this;}

    /**
     * Adds a header entry
     * @param key
     * @param value
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent addHeader(String key, String value) {
        this.header().put(key, value);
        return this;
    }


    /**
     * Returns the contained message
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets a message
     * @param message
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Returns a reply form of the event
     * @param from
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent reply(String from) {
        this.setTo(from());
        header.put(FROM, from);
        isPropagatedFromVicinity = false;
        isReply = true;
        isLocalEvent = false;
        wasHandled = false;
        return this;
    }

    /**
     * Returns whether a request id exists
     * @return
     */
    public boolean hasRequestId() {return StringUtils.isNotEmpty((String) this.header().get(REQUEST_ID));}

    /**
     * Sets the request id
     * @param requestId
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setRequestId(String requestId) {
        if (StringUtils.isNotEmpty(requestId)) {
            this.header().put(REQUEST_ID, requestId);
        }
        return this;
    }

    /**
     * Returns whether the original event name exists
     * @return
     */
    public boolean hasOriginalEventName() {return StringUtils.isNotEmpty((String) this.header().get(ORIGINAL_EVENT_NAME));}

    /**
     * Sets the original event name
     * @param name
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setOriginalEventName(String name) {
        if (StringUtils.isNotEmpty(name)) {
            this.header().put(ORIGINAL_EVENT_NAME, name);
        }
        return this;
    }

    /**
     * Returns the original event name exists
     * @return
     */
    public String originalEventName() {return (String) this.header().get(ORIGINAL_EVENT_NAME);}

    /**
     * Sets the original event for tracking
     * @param originalEvent
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent setOriginalEvent(ArchitectureFirstEvent originalEvent) {
        setRequestId(originalEvent.getRequestId());
        setOriginalEventName(StringUtils.isNotEmpty(originalEvent.originalEventName()) ? originalEvent.originalEventName(): originalEvent.name());
        return initArchitectureFirstEvent(originalEvent);
    }

    private ArchitectureFirstEvent initArchitectureFirstEvent(ArchitectureFirstEvent originalEvent) {
        setRequestId(originalEvent.getRequestId());
        setAccessToken(originalEvent.getAccessToken());
        if (originalEvent.header().containsKey(BOA_CONN)) {
            addHeader(BOA_CONN, (String) originalEvent.header().get(BOA_CONN));
        }
        if (originalEvent.header().containsKey(BOA_PROJECT)) {
            addHeader(BOA_PROJECT, (String) originalEvent.header().get(BOA_PROJECT));
        }
        return this;
    }

    /**
     * Initialize the event based on the default event
     * @param defaultLocalEvent
     * @return ArchitectureFirstEvent
     */
    public ArchitectureFirstEvent initFromDefaultEvent(ArchitectureFirstEvent defaultLocalEvent) {
        setRequestId(defaultLocalEvent.getRequestId());
        return initArchitectureFirstEvent(defaultLocalEvent);
    }

    /**
     * Returns the request id
     * @return
     */
    public String getRequestId() {return (String) this.header().get(REQUEST_ID);}

    /**
     * Convert this object to JSON form
     * @return
     */
    private String getCurrentJson() {
        if (gson == null) {
            initGson();
        }
        return gson.toJson(this);
    }

    private void initGson() {
        gson = new Gson();
        gsonConfig = Configuration
                .builder()
                .mappingProvider(new JacksonMappingProvider())
                .jsonProvider(new JacksonJsonProvider())
                .build();
    }

    /**
     * Returns an Object from a JSON string
     * @param jsonPath - JSON Path
     * @param classType
     * @return
     */
    public Object getValueAs(String jsonPath, Type classType) {
        if (gson == null) {
            initGson();
        }

        var currentJson = getCurrentJson();    // convert current object
        var results = JsonPath.using(gsonConfig).parse(currentJson).read(jsonPath, (Class<? extends Object>) classType);

        return results;
    }

    /**
     * Returns an Object from a JSON string
     * @param jsonPath - JSON Path
     * @return
     */
    public Object getValue(String jsonPath) {
        return getValueAs(jsonPath, Object.class);
    }

    /**
     * Add a value to the header
     * @param name
     * @param value
     * @return
     */
    public ArchitectureFirstEvent addHeaderValue(String name, Object value) {
        header().put(name, value);
        return this;
    }

    /**
     * Returns a header entry
     * @param root
     * @param classType
     * @return
     */
    public Object getHeaderValueAs(String root, Type classType) {
        return getValueAs("$.header." + root, classType);
    }

    /**
     * Returns a header value
     * @param root
     * @return
     */
    public Object getHeaderValue(String root) {
        return getHeaderValueAs(root, Object.class);
    }

    /**
     * Adds a payload value
     * @param name
     * @param value
     * @return
     */
    public ArchitectureFirstEvent addPayloadValue(String name, Object value) {
        payload().put(name, value);
        return this;
    }

    /**
     * Returns a payload entry
     * @param root
     * @param classType
     * @return
     */
    public Object getPayloadValueAs(String root, Type classType) {
        return getValueAs("$.payload." + root, classType);
    }

    /**
     * Returns a payload entry
     * @param root
     * @param classType - should be a list type, such as new TypeToken<List<Product>>(){}.getType()
     * @return
     */
    public Object getPayloadListValueAs(String root, Type classType) {
        var rawList = getPayloadValue(root);
        var json = new Gson().toJson(rawList);
        return gson.fromJson(json, classType);
    }

    /**
     * Returns a payload entry
     * @param root
     * @return
     */
    public Object getPayloadValue(String root) {
        return getPayloadValueAs(root, Object.class);
    }

    @Override
    public String toString() {
        return getCurrentJson();
    }

    // Dynamic methods (start)
    private transient HashMap<String, BiFunction<ArchitectureFirstEvent, Object, ArchitectureFirstEvent>> fnSetters = new HashMap();
    public ArchitectureFirstEvent addSetterFunction(String name, BiFunction<ArchitectureFirstEvent, Object, ArchitectureFirstEvent> fnSetter) {
        fnSetters.put(name, fnSetter);
        return this;
    }

    public ArchitectureFirstEvent apply(BiFunction<ArchitectureFirstEvent, Object, ArchitectureFirstEvent> fnSetter, Object param) {
        fnSetter.apply(this, param);
        return this;
    }
    public ArchitectureFirstEvent apply(String name, String param) {
        fnSetters.get(name).apply(this, param);
        return this;
    }

    private transient HashMap<String, Function<ArchitectureFirstEvent, Object>> fnGetters = new HashMap();
    public ArchitectureFirstEvent addGetterFunction(String name, Function<ArchitectureFirstEvent, Object> fnGetter) {
        fnGetters.put(name, fnGetter);
        return this;
    }

    public Object apply(Function<ArchitectureFirstEvent, Object> fnGetter) {
        return fnGetter.apply(this);
    }
    public Object apply(String name) {
        return fnGetters.get(name).apply(this);
    }
    // Dynamic methods (end)

    // Lifecycle events (start)

    /**
     * Called when instantiated from the Vicinity
     */
    public void onVicinityInit() {
        //... override for custom behavior
    }
    // Lifecycle events (end)

    /**
     * Convert a Vicinity message to an ArchitectureFirstEvent
     * @param source
     * @param message
     * @return return ArchitectureFirstEvent object or null if error
     */
    public static ArchitectureFirstEvent from(Object source, VicinityMessage message) {
        try {
            var eventType = message.getHeader().getEventType();
            if (eventType != null) {
                if (Character.isUpperCase(eventType.charAt(0))) {
                    return from(message);
                }
                else {
                    var cls = Class.forName(message.getHeader().getEventType());
                    ArchitectureFirstEvent event = new Gson().fromJson(message.getJsonPayload(), (Type) cls);
                    return event;
                }
            }

        } catch (Exception e) {
            log.warn("Invalid class definition: ", e);
        }

        return from(message);
    }

    /**
     * Convert a Vicinity message to a generic ArchitectureFirstEvent
     * @param message
     * @return return ArchitectureFirstEvent object or null if error
     */
    public static ArchitectureFirstEvent from(VicinityMessage message) {
        try {
            ArchitectureFirstEvent event = new Gson().fromJson(message.getJsonPayload(), ArchitectureFirstEvent.class);

            return event;
        } catch (Exception e) {
            log.error("Invalid class definition: ", e);
        }

        return null;
    }

    /**
     * Returns an event based on an original event without the payload
     * @param from
     * @param source
     * @param originalEvent
     * @return return ArchitectureFirstEvent object or null if error
     */
    public static ArchitectureFirstEvent fromForReplyWithoutPayload(Object source, String from, ArchitectureFirstEvent originalEvent) {
        ArchitectureFirstEvent replyEvent = new ArchitectureFirstEvent(source, originalEvent.type(), from, originalEvent.from());
        replyEvent.setOriginalEvent(originalEvent);

        return replyEvent;
    }

    /**
     * Returns an event based on an original event
     * @param from
     * @param source
     * @param originalEvent
     * @return return ArchitectureFirstEvent object or null if error
     */
    public static ArchitectureFirstEvent fromForReply(Object source, String from, ArchitectureFirstEvent originalEvent) {
        ArchitectureFirstEvent replyEvent = fromForReplyWithoutPayload(source, from, originalEvent);
        replyEvent.addPayload(originalEvent.payload());
        replyEvent.setAccessToken(originalEvent.getAccessToken());

        return replyEvent;
    }
}
