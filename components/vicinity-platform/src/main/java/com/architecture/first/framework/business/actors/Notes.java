package com.architecture.first.framework.business.actors;

import com.architecture.first.framework.technical.util.SimpleModel;
import com.google.gson.Gson;

import java.util.Map;

/**
 * Notes represents information an Actor will record for longer term storage than memory
 */
public class Notes {
    private String author;
    private final SimpleModel entries = new SimpleModel();

    public Notes() {
    }

    public Notes(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Notes addNotes(Map<String, Object> entries) {
        this.entries.putAll(entries);
        return this;
    }

    public Notes addNotes(SimpleModel entries) {
        this.entries.putAll(entries);
        return this;
    }

    public SimpleModel getEntries() {
        return SimpleModel.from(this.entries);
    }

    public static Notes from(Map<String, Object> entries) {
        return new Notes().addNotes(entries);
    }

    public static Notes from(SimpleModel entries) {
        return new Notes().addNotes(entries);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
