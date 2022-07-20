package com.architecture.first.framework.business.actors;

import com.architecture.first.framework.business.actors.exceptions.MemoryException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The local short term memory for an Actor. It serves as an in process cache.
 */
@Slf4j
@Repository
public class Memory {

    @Value("${actor.memory.maximumNumberOfElementsInMemory:200}")
    private int maximumNumberOfElementsInMemory;

    @Value("${actor.memory.numberOfMinutesToExpireEntry:120}")
    private int numberOfMinutesToExpireEntry;

    private final Map<String, LoadingCache<String, Object>> cacheByType = new HashMap<String, LoadingCache<String, Object>>();
    private final Gson gson = new Gson();

    /**
     * Initialize the cache
     */
    @PostConstruct
    public void init() {
        createCache(Object.class.getTypeName());
    }

    /**
     * Store an object in memory by type
     * @param name
     * @param value
     * @param classType
     */
    public void store(String name, Object value, Type classType) {

        LoadingCache<String, Object> cache = (cacheByType.containsKey(classType.getTypeName()))
                ? cacheByType.get(classType.getTypeName())
                : createCache(classType.getTypeName());

        cache.put(name, value);
    }

    /**
     * Store an object in memory
     * @param name
     * @param value
     */
    public void store(String name, Object value) {
        store(name, value, Object.class);
    }

    /**
     * Store an occurrence of an object in memory by type
     * @param name
     * @param value
     * @param classType
     */
    public void storeOccurrence(String name, Object value, Type classType) {
        try {
            List<Object> items = null;

            try {
                items = (List<Object>) getCache(classType.getTypeName()).get(name);
            }
            catch (CacheLoader.InvalidCacheLoadException e) {
                // This is not an error in this use of the cache.
                // It means that there are no existing notes
                items = new ArrayList<>();
            }

            items.add(value);

            store(name, items, classType);
        }
        catch (ExecutionException e) {
            throw new MemoryException(e);
        }
    }

    /**
     * Store an occurrence of an object in memory
     * @param name
     * @param value
     */
    public void storeOccurrence(String name, Object value) {
        storeOccurrence(name, value, Object.class);
    }

    /**
     * Looks at a specific cache and returns value if it is found
     * @param name
     * @return Optional
     */
    public <T> Optional<T> retrieve(String name, Class<T> classType) {
        try {
            return (Optional<T>) Optional.of(getCache(classType.getTypeName()).get(name));
        }
        catch (ExecutionException e) {
            throw new MemoryException(e);
        }
        catch (CacheLoader.InvalidCacheLoadException e) {
            // This exception is ok, because it is Ok to forget things.
            // It is the job of the caller to reload a memory if it has been forgotten
            return Optional.empty();
        }
    }

    /**
     * Looks at all the caches regardless of type and returns value if it is found
     * @param name
     * @return Optional
     */
    public Optional<Object> retrieve(String name) {
        var lists = cacheByType.values().stream().map(lc -> {
            try {
                return Optional.of(lc.getUnchecked(name));
            }
            catch (Exception e) {return Optional.empty();} // Note: it is not a problem if empty
        }).filter(Predicate.not(Optional::isEmpty)).collect(Collectors.toList());

        return lists.get(0);
    }

    /**
     * Retrieve a fact based on conditions
     * @param classType
     * @param fnFilter
     * @return
     */
    public List<Map.Entry<String, Object>> retrieve(Type classType, Predicate<Map.Entry<String, Object>> fnFilter) {
        var list = getCache(classType.getTypeName()).asMap()
                .entrySet().stream().filter(fnFilter)
                .collect(Collectors.toList());

        return list;
    }

    /**
     * Remove a fact from memory
     * @param name
     * @param classType
     * @param <T>
     */
    public <T> void forget(String name, Class<T> classType) {
        store(name, "FORGOTTEN", classType);
    }

    /**
     * Potentially refresh memory from an external source, such as notes
     * @param name
     * @return
     * @param <T>
     */
    public <T> T refreshFromNotes(String name) {
        // TODO - determine if notes should be tied into memory
        return null;
    }

    /**
     * Return the cache to store or retrieve facts
     * @param cachename
     * @return
     * @param <T>
     */
    private <T> LoadingCache<String, T> getCache(String cachename) {
        if (cacheByType.containsKey(cachename)) {
            return (LoadingCache<String, T>) cacheByType.get(cachename);
        }

        return createCache(cachename);
    }

    /**
     * Create a cache to store or retrieve facts
     * @param cachename
     * @return
     * @param <T>
     */
    private <T> LoadingCache<String, T> createCache(String cachename) {
        LoadingCache<String, T> cache =  CacheBuilder.newBuilder()
                .maximumSize(maximumNumberOfElementsInMemory)
                .expireAfterAccess(numberOfMinutesToExpireEntry, TimeUnit.MINUTES)
                .build(new CacheLoader<String, T>() {
                    @Override
                    public T load(String s) throws Exception {
                        return refreshFromNotes(s);
                    }
                });
        cacheByType.put(cachename, (LoadingCache<String, Object>) cache);

        return cache;
    }

    /**
     * Provides a Brain dump from memory
     * @return lists of objects
     */
    public Map<String, Object> dump() {
        return retrieveAll();
    }

    /**
     * Return all facts
     * @return a map of facts
     */
    private Map<String, Object> retrieveAll() {
        String json = gson.toJson(cacheByType);
        Map<String,Object> map = gson.fromJson(json,Map.class);

        return map;
    }
}
