package com.architecture.first.framework.business.vicinity.threading;

import com.architecture.first.framework.technical.threading.Connection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages one or more connections to the Vicinity
 */
public class VicinityConnections {
    private final Map<String, Connection> connections = new HashMap<>();

    /**
     * Returns a connection by name
     * @param name
     * @return a Vicinity connection
     */
    public Connection getConnection(String name) {
        return connections.get(name);
    }

    /**
     * Sets a connection by name
     * @param name
     * @param connection
     */
    public void setConnection(String name, Connection connection) {
        connections.put(name, connection);
    }

    /**
     * Determines if there is an existing connection by name
     * @param name
     * @return
     */
    public boolean containsConnection(String name) {
        return connections.containsKey(name);
    }

    /**
     * Shuts down all contained connections
     */
    public void shutdown() {
        connections.values().stream()
                .filter(Predicate.not(Connection::isShutdown))
                .forEach(Connection::shutdownNow);
    }

    /**
     * Returns the number of connections
     * @return
     */
    public int numberOfConnections() {
        return connections.values().size();
    }

    /**
     * Determines if the status of all connections are ok
     * @param numRunningTasks
     * @return
     */
    public boolean isOk(int numRunningTasks) {
        int numCurrentlyRunningTasks = connections.values().stream()
                .filter(Predicate.not(conn -> conn.getFuture().isDone()))
                .collect(Collectors.toList()).size();

        return numCurrentlyRunningTasks == numRunningTasks && numCurrentlyRunningTasks == numberOfConnections();
    }
}
