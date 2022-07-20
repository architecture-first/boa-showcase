package com.architecture.first.framework.technical.cache;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Iterates through cache hash set entries and allows actions
 */
public class JedisHCursor {
    private final JedisPooled jedis;
    private final int DEFAULT_CURSOR_START = 0;
    private final int DEFAULT_CHUNK_SIZE = 1000;
    private final String DEFAULT_MATCH_PARAMS = "*";
    private String cursor;

    public JedisHCursor(JedisPooled jedis) {this.jedis = jedis;}

    /**
     * Iterates through entries
     * @param filter - Jedis compatible filter
     * @param start - starting entry
     * @param chunkSize - amount of entries returned per call
     * @param fnOnChunk - method to execute per entry
     */
    public void processAll(String hset, String filter, int start, int chunkSize,
                           Function<Map.Entry<String,String>, Boolean> fnOnChunk) {
        ScanParams scanParams = new ScanParams().count(chunkSize).match(filter);
        cursor = String.valueOf(start);
        var end = String.valueOf(DEFAULT_CURSOR_START);
        AtomicBoolean hasPassed = new AtomicBoolean(false);

        do {
            var scanResult = jedis.hscan(hset, cursor, scanParams);
            scanResult.getResult().forEach(e -> {
                if (fnOnChunk.apply(e)) {
                    hasPassed.set(true);
                }
            });

            if (hasPassed.get()) {
                break;
            }
            cursor = scanResult.getCursor();
        } while (!cursor.equals(end));
    }

    /**
     * Iterates through entries
     * @param filter - Jedis compatible filter
     * @param chunkSize - amount of entries returned per call
     * @param fnOnChunk - method to execute per entry
     */
    public void processAll(String hset, String filter, int chunkSize,
                           Function<Map.Entry<String,String>, Boolean> fnOnChunk) {
        processAll(hset, filter, DEFAULT_CURSOR_START, chunkSize, fnOnChunk);
    }

    /**
     * Iterates through entries
     * @param fnOnChunk - method to execute per entry
     */
    public void processAll(String hset, Function<Map.Entry<String,String>, Boolean> fnOnChunk) {
        processAll(hset, DEFAULT_MATCH_PARAMS, DEFAULT_CURSOR_START, DEFAULT_CHUNK_SIZE, fnOnChunk);
    }

}
