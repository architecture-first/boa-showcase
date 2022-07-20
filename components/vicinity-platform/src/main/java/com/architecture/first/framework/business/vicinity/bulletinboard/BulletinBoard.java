package com.architecture.first.framework.business.vicinity.bulletinboard;

import com.architecture.first.framework.technical.cache.JedisHCursor;
import com.architecture.first.framework.technical.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The shared object for inter-actor communication.
 */
@Slf4j
@Repository
public class BulletinBoard {

    @Autowired
    private JedisPooled jedis;
    private final String bulletinBoardConnectionId = UUID.randomUUID().toString();

    @Value("${vicinity.bulletin-board.items.default-expiration-seconds:3600}")
    private long expirationSeconds;
    private static final String BULLETIN_BOARD_PREFIX = "BulletinBoard:topic/";

    @PostConstruct
    public void init() {
        log.info("bulletinBoardConnectionId: " + bulletinBoardConnectionId);
    }

    /**
     * Post an entry to the main bulletin board
     * @param name
     * @param value
     */
    public void post(String name, String value) {
        jedis.set(name, value);
        jedis.expire(name, expirationSeconds);
    }

    /**
     * Post an entry to a topic-based bulletin board
     * @param topic
     * @param name
     * @param value
     */
    // default to date based topics
    public void postTopic(String topic, String name, String value) {
        topic = DateUtils.appendDaily(topic);

        jedis.hset(topic, name, value);
        jedis.expire(topic, expirationSeconds);
    }

    /**
     * Post a status entry to an availability bulletin board
     * @param topic
     * @param name
     * @param value
     * @param statusString
     */
    // default to date based topics
    public void postStatusTopic(String topic, String name, String value, String statusString) {
        topic = DateUtils.appendDaily(topic);

        String activeTopic = BULLETIN_BOARD_PREFIX + topic + "/Active";
        String awayTopic = BULLETIN_BOARD_PREFIX + topic + "/Away";
        String goneTopic = BULLETIN_BOARD_PREFIX + topic + "/Gone";

        switch (value) {
            case "Gone":
                jedis.hdel(activeTopic, name);
                jedis.hdel(awayTopic, name);
                jedis.hset(goneTopic, name, statusString);
                jedis.expire(goneTopic, expirationSeconds);
                return;
            case "Away":
                jedis.hdel(activeTopic, name);
                jedis.hset(awayTopic, name, statusString);
                jedis.expire(awayTopic, expirationSeconds);
                return;
        }
        jedis.hset(activeTopic, name, statusString);
        jedis.expire(activeTopic, expirationSeconds);

        clearIdleTopicEntries(activeTopic, awayTopic);
    }

    /**
     * Remove idle entries that have not been updated recently
     * @param activeTopic
     * @param awayTopic
     */
    public void clearIdleTopicEntries(String activeTopic, String awayTopic) {
        var idleEntries = new HashMap<String,String>();

        var cursor = new JedisHCursor(jedis);
        cursor.processAll(activeTopic, e -> {
            var entry = BulletinBoardStatus.from(e.getValue());
            if (entry.getTimestamp().isBefore(ZonedDateTime.now(ZoneId.of("GMT")).minus(2, ChronoUnit.MINUTES))) {
                idleEntries.put(e.getKey(), e.getValue());
            }

            return false;
        });

        idleEntries.entrySet().forEach(e -> {
            jedis.hdel(activeTopic, e.getKey());
            jedis.hset(awayTopic, e.getKey(), e.getValue());
            jedis.expire(awayTopic, expirationSeconds);
        });

    }

    /**
     * Determine which Actor should do the next task based on bulletin board status
     * @param topic
     * @return
     */
    public String whosTurnIsIt(String topic) {
        topic = DateUtils.appendDaily(topic);

        String activeTopic = BULLETIN_BOARD_PREFIX + topic + "/Active";
        if (jedis.exists(activeTopic)) {
            return jedis.hrandfield(activeTopic);
        }

        return "";
    }

    /**
     * Read an entry from the main bulletin board
     * @param name
     * @return
     */
    public String read(String name) {
        return jedis.get(name);
    }

    /**
     * Read an entry from a daily topic-related bulletin board
     * @param topic
     * @param name
     * @return
     */
    public String readTopicEntry(String topic, String name) {
        topic = DateUtils.appendDaily(topic);
        return jedis.hget(topic, name);
    }

    /**
     * Read a random entry from a daily topic-related bulletin board
     * @param topic
     * @return
     */
    public String readRandomTopicEntry(String topic) {
        topic = DateUtils.appendDaily(topic);
        if (jedis.exists(topic)) {
            String key = jedis.hrandfield(topic);
            return jedis.hget(topic, key);
        }

        return "";
    }

    /**
     * Read all entries from a daily topic-related bulletin board
     * @param topic
     * @param name
     * @return
     */
    public Map<String,String> readTopicEntries(String topic, String name) {
        topic = DateUtils.appendDaily(topic);
        return jedis.hgetAll(topic);
    }

    /**
     * Determines if the bulletin board is healthy
     * @return true if the bulletin board can make a simple update
     */
    public boolean isOk() {
        try {
            String bulletinboardPath = "environment/health/bulletinboard";
            jedis.hset(bulletinboardPath, "BB" + bulletinBoardConnectionId, ZonedDateTime.now(ZoneId.of("GMT")).toString());
            jedis.expire(bulletinboardPath, expirationSeconds);
        }
        catch(Exception e) {
            log.error("Health Check Error: " + e);
            return false;
        }

        return true;
    }
}
