package com.architecture.first.framework.business.vicinity.vault;

import com.architecture.first.framework.business.vicinity.locking.Lock;
import com.architecture.first.framework.technical.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents a common location for secure information.
 * TODO:   support encryption
 */
@Slf4j
@Repository
public class Vault {

    public static final String VAULT = "Vault";
    @Autowired
    private JedisPooled jedis;

    @Autowired
    private Lock lock;

    private final String vaultConnectionId = UUID.randomUUID().toString();
    private final int expirationSeconds = 86400;

    @PostConstruct
    public void init() {
        log.info("vaultConnectionId: " + vaultConnectionId);
    }

    /**
     * Add an item to the Vault
     * @param name
     * @param value
     */
    public void addItem(String name, String value) {
        String vault = DateUtils.appendDaily(VAULT);

        jedis.hset(vault, name, value);
        jedis.expire(vault, expirationSeconds);
    }

    /**
     * Return and item from the Vault
     * @param name
     * @return
     */
    public String getItem(String name) {
        String vault = DateUtils.appendDaily(VAULT);

        return jedis.hget(vault, name);
    }

    /**
     * Remove an item from the Vault
     * @param name
     * @return
     */
    public long removeItem(String name) {
        String vault = DateUtils.appendDaily(VAULT);

        return jedis.hdel(vault, name);
    }

    /**
     * Determines if the status of the Vault is ok
     * @return true if healthy
     */
    public boolean isOk() {
        try {
            String taskListPath = "environment/health/vault";
            jedis.hset(taskListPath, "TL" + vaultConnectionId, ZonedDateTime.now(ZoneId.of("GMT")).toString());
            jedis.expire(taskListPath, 120);
        }
        catch(Exception e) {
            log.error("Health Check Error: " + e);
            return false;
        }

        return true;
    }
}
