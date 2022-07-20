package com.architecture.first.framework.business.vicinity.locking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import java.util.UUID;

/**
 * Represents a shared Lock across Actors
 */
@Slf4j
@Repository
public class Lock {
    public static final String FAILED_LOCK_ATTEMPT = "FAILED_LOCK_ATTEMPT";
    public static final String NOT_OWNER_OF_THE_LOCK = "NOT_OWNER_OF_THE_LOCK";
    public static final String LOCK_DOES_NOT_EXIST = "LOCK_DOES_NOT_EXIST";
    public static int DEFAULT_LOCK_DURATION = 300; // 5 minutes
    public static String LOCK_TEMPLATE = "Lock:%s";

    @Autowired
    private JedisPooled jedis;
    private final String lockConnectionId = UUID.randomUUID().toString();

    /**
     * Attempts to acquire a lock to perform an action
     * @param resource - resource the lock is used for
     * @param requester - requester of the lock
     * @param lockDurationSeconds - length of time the lock will be held for
     * @return the lock name
     */
    public String attemptLock(String resource, String requester, long lockDurationSeconds) {
        String lockname = String.format(LOCK_TEMPLATE, resource);
        if (!jedis.exists(lockname)) {
            jedis.set(lockname, requester);
            jedis.expire(lockname, lockDurationSeconds);
            return lockname;
        }

        return FAILED_LOCK_ATTEMPT;
    }

    /**
     * Attempts to acquire a lock to perform an action
     * @param resource - resource the lock is used for
     * @param requester - requester of the lock
     * @return the lock name
     */
    public String attemptLock(String resource, String requester) {
        return attemptLock(resource, requester, DEFAULT_LOCK_DURATION);
    }

    /**
     * Unlocks a resource that was locked
     * @param resource - resource the lock is used for
     * @param requester - requester of the lock
     * @return lock name if successful or "NOT_OWNER_OF_THE_LOCK" or "LOCK_DOES_NOT_EXIST"
     */
    public String unlock(String resource, String requester) {
        String lockname = String.format(LOCK_TEMPLATE, resource);
        if (jedis.exists(lockname)) {
            if (requester.equalsIgnoreCase(jedis.get(lockname))) {
                jedis.del(lockname);
                return lockname;
            }
            return NOT_OWNER_OF_THE_LOCK;
        }
        return LOCK_DOES_NOT_EXIST;
    }

    /**
     * Returns the Lock status
     * @param resource - resource being locked
     * @return true if the resource is locked
     */
    public boolean isLocked(String resource) {
        String lockname = String.format(LOCK_TEMPLATE, resource);
        return jedis.exists(lockname);
    }
}
