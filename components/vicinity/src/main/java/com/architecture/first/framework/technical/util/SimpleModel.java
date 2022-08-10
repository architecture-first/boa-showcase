package com.architecture.first.framework.technical.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a dynamic set of data
 */
// TODO - replace with version from Lush Framework (via Paul Parrone)
public class SimpleModel extends LinkedHashMap<String, Object> {

    /**
     * Creates a SimpleModel from a Map
     * @param map
     */
    public SimpleModel(Map<String, Object> map) {
        super();
        this.putAll(map);
    }

    /**
     * Creates an empty SimpleModel
     */
    public SimpleModel() {
        super();
    }

    /**
     * Creates a SimpleModel from a Map
     * @param map
     * @return SimpleModel
     */
    public static SimpleModel from(Map<String, Object> map) {
        SimpleModel simpleModel = new SimpleModel(map);
        return simpleModel;
    }

    /**
     * Creates a SimpleModel from another SimpleModel
     * @param source
     * @return SimpleModel
     */
    public static SimpleModel from(SimpleModel source) {
        SimpleModel target = new SimpleModel();
        source.forEach((k, v) -> {
            target.put(k, v);
        });
        return target;
    }
}
