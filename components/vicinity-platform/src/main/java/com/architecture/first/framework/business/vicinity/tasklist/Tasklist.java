package com.architecture.first.framework.business.vicinity.tasklist;

import com.architecture.first.framework.business.vicinity.locking.Lock;
import com.architecture.first.framework.technical.util.DateUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a task list for a common workflow across Actors
 */
@Slf4j
@Repository
public class Tasklist {

    public static final String TASKLIST_DEFINITIONS = "TasklistDefinitions";
    public static final String STATUS = "status";
    public static final String CURRENT_TASK = "currentTask";
    public static final String LAST_COMPLETED_TASK = "lastCompletedTask";
    public static final String LAST_FAILED_TASK = "lastFailedTask";
    public static final int ONE_DAY_DURATION_SECONDS = 86400;
    public static final String NUM_TASKS_COMPLETED = "numTasksCompleted";
    public static final String NUM_TASKS_FAILED = "numTasksFailed";

    @Autowired
    private JedisPooled jedis;

    @Autowired
    private Lock lock;

    private final String taskListConnectionId = UUID.randomUUID().toString();

    @Value("${vicinity.task-list.items.default-expiration-seconds:3600}")
    private long expirationSeconds;
    @Value("${vicinity.task-list.root-path:none}")
    private String rootPath;

    private static final String TASK_LIST_PREFIX = "tasklist:topic/";

    @PostConstruct
    public void init() {
        log.info("tasklistConnectionId: " + taskListConnectionId);
        initTasklists();
    }

    /**
     * Post an entry to a task list
     * @param usecase
     * @param name
     * @param value
     */
    public void postEntry(String requestId, String usecase, String name, String value) {
        String tasklist = generateSignature(requestId, usecase);

        // Setup active monitoring
        String tasklistDefinition = jedis.hget(TASKLIST_DEFINITIONS, usecase);
        if (StringUtils.isNotEmpty(tasklistDefinition)) {
            TasklistMonitoringDefinition monitoring = new Gson().fromJson(tasklistDefinition, TasklistMonitoringDefinition.class);
            tasklist = generateSignature(requestId, monitoring.getName());
        }
        jedis.hset(tasklist, STATUS, "InProgress");
        jedis.expire(tasklist, expirationSeconds);

        // Do this every time to eliminate edge cases. Robustness is higher than efficiency in priority
        // Add to taskboard
        String taskboard =  DateUtils.appendDaily("Tasklists") + "/Active";
        jedis.hset(taskboard, tasklist, "running");
        jedis.expire(taskboard, expirationSeconds);
        jedis.hset(tasklist, CURRENT_TASK, name);

        jedis.hset(tasklist, name, value);
        jedis.expire(tasklist, expirationSeconds);
    }

    /**
     * Read entries from a task list
     * @param usecase
     * @param name
     * @return a map of all entries
     */
    public Map<String,String> readEntries(String usecase, String name) {
        return jedis.hgetAll(usecase);
    }

    /**
     * Records a failure of processing in the task list for bookkeeping.
     * @param requestId
     * @param usecase
     * @param task
     * @param message
     */
    public void recordFailure(String requestId, String usecase, String task, String message) {
        String tasklist = generateSignature(requestId, usecase);
        jedis.hset(tasklist, STATUS, "Failed");
        jedis.hincrBy(tasklist, NUM_TASKS_FAILED, 1);
        jedis.hdel(tasklist, CURRENT_TASK);
        jedis.hset(tasklist, LAST_COMPLETED_TASK, task);
        jedis.hset(tasklist, "message", message);
    }

    /**
     * Records a failure of processing in the task list for bookkeeping
     * @param requestId
     * @param task
     * @param message
     */
    public void recordFailure(String requestId, String task, String message) {
        recordFailure(requestId, task, task, message);
    }

    /**
     * Records the completion of processing in the task list for bookkeeping
     * @param requestId
     * @param usecase
     * @param task
     */
    public void recordCompletion(String requestId, String usecase, String task) {
        String tasklist = generateSignature(requestId, usecase);
        jedis.hincrBy(tasklist, NUM_TASKS_COMPLETED, 1);
        jedis.hdel(tasklist, CURRENT_TASK);
        jedis.hset(tasklist, LAST_COMPLETED_TASK, task);
    }

    /**
     * Records the completion of processing in the task list for bookkeeping
     * @param requestId
     * @param task
     */
    public void recordCompletion(String requestId, String task) {
        recordCompletion(requestId, task, task);
    }

    /**
     * Update the bookkeeping status of completed tasks
     * @param requestId
     * @param usecase
     * @param task
     * @return
     */
    public boolean handleFinishedTasks(String requestId, String usecase, String task) {
        String tasklist = generateSignature(requestId, usecase);
        String status = jedis.hget(tasklist, STATUS);
        if ("Failed".equalsIgnoreCase(status)) {
            // Move to failed tasks
            // remove from task board
            String taskboard =  DateUtils.appendDaily("Tasklists") + "/Active";
            jedis.hdel(taskboard, tasklist);
            jedis.expire(taskboard, expirationSeconds);

            // add to task board
            taskboard =  DateUtils.appendDaily("Tasklists") + "/Failed";
            jedis.hset(taskboard, tasklist, "Failed");
            jedis.expire(taskboard, expirationSeconds);
            return true;
        }
        else if ("Completed".equalsIgnoreCase(status)) {
            // Move to failed tasks
            // remove from task board
            String taskboard =  DateUtils.appendDaily("Tasklists") + "/Active";
            jedis.hdel(taskboard, tasklist);
            jedis.expire(taskboard, expirationSeconds);

            // add to task board
            taskboard =  DateUtils.appendDaily("Tasklists") + "/Completed";
            jedis.hset(taskboard, tasklist, "Completed");
            jedis.expire(taskboard, expirationSeconds);
            return true;
        }

        // Load Task List Definitions
        String tasklistDefinition = jedis.hget(TASKLIST_DEFINITIONS, usecase);

        // Setup active monitoring
        TasklistMonitoringDefinition monitoring = new Gson().fromJson(tasklistDefinition, TasklistMonitoringDefinition.class);

        if (monitoring != null) { // if an optional task definition is defined
            Integer numTaskCompleted = Integer.parseInt(jedis.hget(tasklist, NUM_TASKS_COMPLETED));
            if (numTaskCompleted >= monitoring.getTasks().size()) {  // A demo level determination of completion
                jedis.hset(tasklist, STATUS, "Completed");

                // Move to completed tasks
                // remove from task board
                String taskboard = DateUtils.appendDaily("Tasklists") + "/Active";
                jedis.hdel(taskboard, tasklist);
                jedis.expire(taskboard, expirationSeconds);

                // add to task board
                taskboard = DateUtils.appendDaily("Tasklists") + "/Completed";
                jedis.hset(taskboard, tasklist, "Completed");
                jedis.expire(taskboard, expirationSeconds);
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if an Actor has acknowledged a given task
     * @param requestId
     * @param usecase
     * @param task
     * @return true if an actor has acknowledged the task
     */
    public boolean hasAcknowledgedTask(String requestId, String usecase, String task) {
        String tasklist = generateSignature(requestId, usecase);

        return jedis.hexists(task, task);
    }

    /**
     * Determines if an Actor has completed a given task
     * @param requestId
     * @param usecase
     * @param task
     * @return true if an actor has completed the task
     */
    public boolean hasCompletedTask(String requestId, String usecase, String task) {
        String tasklist = generateSignature(requestId, usecase);
        String json = jedis.hget(tasklist, task);

        return json.contains("\"status\":\"Complete\"");
    }

    /**
     * Determines if the Tasklist component is ok
     * @return true if Tasklist component is ok
     */
    public boolean isOk() {
        try {
            String taskListPath = "environment/health/tasklist";
            jedis.hset(taskListPath, "TL" + taskListConnectionId, ZonedDateTime.now(ZoneId.of("GMT")).toString());
            jedis.expire(taskListPath, expirationSeconds);
        }
        catch(Exception e) {
            log.error("Health Check Error: " + e);
            return false;
        }

        return true;
    }

    /**
     * Generates a signature for the task list based on request id
     * @param requestId
     * @param usecase
     * @return the signature
     */
    private String generateSignature(String requestId, String usecase) {
        String template = "%s/Tasklist:%s";
        return String.format(template, requestId, usecase);
    }

    /**
     * Attempts to load the task list based on external definitions
     */
    protected void initTasklists() {

        if (StringUtils.isNotEmpty(rootPath) && rootPath.equals("none")) {
            return; // do nothing
        }

        if (jedis.ttl(TASKLIST_DEFINITIONS) < 100) {
            if (!lock.isLocked(TASKLIST_DEFINITIONS)
                    && !lock.attemptLock(TASKLIST_DEFINITIONS, "Tasklist." + taskListConnectionId)
                    .equals(Lock.FAILED_LOCK_ATTEMPT)) {
                try {
                    // Load Task List Definitions
                    var files = findTaskDefinitions(Path.of(rootPath), "json");
                    files.forEach(def -> {
                        String tasklist = null;
                        try {
                            tasklist = new String(Files.readAllBytes(Paths.get(def.toString())));
                            String usecase = def.toUri().getPath()
                                    .replace(rootPath, "")
                                    .replace("//", "")
                                    .replace(".json", "");
                            jedis.hset(TASKLIST_DEFINITIONS, usecase, tasklist);
                            jedis.expire(TASKLIST_DEFINITIONS, ONE_DAY_DURATION_SECONDS);
                        } catch (IOException e) {
                            log.error("Cannot read Task Definition", e);

                            // NOTE: This error will affect monitoring, but should not affect the operation of ths system
                        }
                    });
                } catch (IOException e) {
                    log.error("Cannot read Use Case information", e);

                    // NOTE: This error will affect monitoring, but should not affect the operation of ths system
                    // The philosophy is to have resilient components that gracefully degrade but do not fail.
                    // Therefore, the exception does not need to propagate.
                    return;
                } finally {
                    lock.unlock(TASKLIST_DEFINITIONS, "Tasklist." + taskListConnectionId);
                }
            }
        }
    }

    /**
     * Returns the desired task definitions based on a path
     * @param path
     * @param fileExtension
     * @return a list of file paths
     * @throws IOException
     */
    private List<Path> findTaskDefinitions(Path path, String fileExtension) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(fileExtension))
                    .collect(Collectors.toList());
        }

        return result;
    }
}
