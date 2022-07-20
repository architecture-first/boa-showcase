package com.architecture.first.framework.business.vicinity.queue;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.UUID;

@Slf4j
@Repository
public class Queue {

    public static final int ONE_DAY_DURATION_SECONDS = 86400;

    @Autowired
    private JedisPooled jedis;

    private final String queueConnectionId = UUID.randomUUID().toString();

    // default to expire the same as a task list
    @Value("${vicinity.task-list.items.default-expiration-seconds:3600}")
    private long expirationSeconds;
    private final int waitTimeount = 60;

    private static final String QUEUE_PREFIX = "queue/";
    private final Gson gson = new Gson();

    @PostConstruct
    public void init() {
    }

    /**
     * Creates a queue
     * @param signature - signature of the queue
     * @param expire - true if the queue should expire after a period of seconds
     * @param expirationSeconds - seconds to expire the queue if the expire parameter is true
     * @return true if successfully created
     */
    public boolean create(String signature, boolean expire, long expirationSeconds) {
        if (!jedis.exists(signature)) {
            jedis.rpush(signature, "init");
            jedis.lpop(signature);
            if (expire) {
                jedis.expire(signature, expirationSeconds);
            }
        }

        return jedis.exists(signature);
    }

    /**
     * Creates a queue
     * @param signature - signature of the queue
     * @param expire - true if the queue should expire after a period of seconds (default is 3600 seconds)
     * @return true if successfully created
     */
    public boolean create(String signature, boolean expire) {
        return create(signature, expire, expirationSeconds);
    }

    /**
     * Creates a queue
     * @return true if successfully created
     */
    public boolean create(String signature) {
        return create(signature, false, expirationSeconds);
    }

    /**
     * Push an entry to the back of the queue
     * @param signature
     * @param value
     */
    public void push(String signature, String value) {
        jedis.rpush(signature, value);
    }

    /**
     * Push an entry of a given type to the back of the queue
     * @param signature
     * @param value
     * @param classType
     */
    public void push(String signature, Object value, Type classType) {
        var json = gson.toJson(value, classType);
        push(signature, json);
    }

    /**
     * Pops an entry off the front of the queue
     * @param signature - the name of the queue
     * @param waitIfEmpty - true to block while empty
     * @param waitTimeout - the time to block if empty
     * @return a string of the entry
     */
    public String pop(String signature, boolean waitIfEmpty, int waitTimeout) {
        return (waitIfEmpty)
                ? jedis.blpop(waitTimeout,signature).get(0)
                : jedis.lpop(signature);
    }

    /**
     * Pops an entry off the front of the queue
     * @param signature - the name of the queue
     * @param waitIfEmpty - true to block while empty
     * @return a string of the entry
     */
    public String pop(String signature, boolean waitIfEmpty) {
        return pop(signature, waitIfEmpty, waitTimeount);
    }

    /**
     * Pops an entry off the front of the queue without waiting if empty
     * @param signature - the name of the queue
     * @return a string of the entry
     */
    public String pop(String signature) {
        return pop(signature, false);
    }

    /**
     * Pops an entry off the front of the queue
     * @param signature - the name of the queue
     * @param waitIfEmpty - true to block while empty
     * @param waitTimeout - the time to block if empty
     * @param classType - class type of entry
     * @return a string of the entry
     */
    public <T> T pop(String signature, boolean waitIfEmpty, int waitTimeout, Type classType) {
        var obj = gson.fromJson(
                pop(signature, waitIfEmpty, waitTimeout), classType
        );

        return (T) obj;
    }

    /**
     * Pops an entry off the front of the queue
     * @param signature - the name of the queue
     * @param waitIfEmpty - true to block while empty
     * @param classType - class type of entry
     * @return a string of the entry
     */
    public <T> T pop(String signature, boolean waitIfEmpty, Type classType) {
        return  pop(signature, waitIfEmpty, waitTimeount, classType);
    }

    /**
     * Pops an entry off the front of the queue
     * @param signature - the name of the queue
     * @param classType - class type of entry
     * @return a string of the entry
     */
    public <T> T pop(String signature, Type classType) {
        return  pop(signature, false, classType);
    }

    /**
     * Returns the size of the queue
     * @param signature
     * @return
     */
    public long size(String signature) {
        return jedis.llen(signature);
    }

}
