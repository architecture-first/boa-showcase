package com.architecture.first.framework.business.vicinity.todo;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents an entry in a TO-DO list
 */
public class ToDoListEntry {
    private String owner;
    private String group;
    private final String key;
    private final Long index;

    public ToDoListEntry(String group, String key, Long index) {
        this(group, key, index, null);
     }

    public ToDoListEntry(String group, String key, Long index, String owner) {
        this.group = group;
        this.key = key;
        this.index = index;
        this.owner = owner;
    }

    public long getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    private String formatEntry() {
        return new Gson().toJson(this, ToDoListEntry.class);
    }

    public String toString() {
        return formatEntry();
    }

    public boolean hasOwner() {return StringUtils.isNotEmpty(owner);}

    public static ToDoListEntry from(String json) {
        return new Gson().fromJson(json, ToDoListEntry.class);
    }
}



