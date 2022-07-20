package com.architecture.first.framework.business.vicinity.tickets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.util.function.Function;

/**
 * Represents a shared sequence number in the Vicinity
 */
@Component
public class TicketNumber {

    @Autowired
    private JedisPooled jedis;

    public static String TICKET_NUMBERS = "TicketNumbers";

    /**
     * Sets a value for a ticket by name
     * @param name
     * @param value
     * @return
     */
    public long set(String name, long value) {
        jedis.hset(TICKET_NUMBERS, name, String.valueOf(value));
        return value;
    }

    /**
     * Determines the next value for a ticket
     * @param name - name of the ticket
     * @param fnSetup - custom method to determine ticket numbers
     * @return - the available ticket number
     */
    public long next(String name, Function<String, Long> fnSetup) {
        if (!jedis.hexists(TICKET_NUMBERS, name)) {
            var startNum = fnSetup.apply(name) + 1;
            jedis.hset(TICKET_NUMBERS, name, String.valueOf(startNum));
            return startNum;
        }

        return jedis.hincrBy(TICKET_NUMBERS, name, 1);
    }
}
