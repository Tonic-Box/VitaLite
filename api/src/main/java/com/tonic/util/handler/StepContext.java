package com.tonic.util.handler;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StepContext
{
    @Getter
    private final Map<String,Integer> labels = new HashMap<>();
    private final Map<String, Object> contextMap = new HashMap<>();

    public List<Object> values() {
        return List.copyOf(contextMap.values());
    }

    public void put(String key, Object value) {
        contextMap.put(key, value);
    }

    public void putIfAbsent(String key, Object value) {
        contextMap.putIfAbsent(key, value);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        if(!contextMap.containsKey(key)) {
            put(key, defaultValue);
            return defaultValue;
        }
        return (T) contextMap.get(key);
    }

    public <T> T get(String key) {
        if(!contextMap.containsKey(key)) {
            return null;
        }
        return (T) contextMap.get(key);
    }

    public boolean contains(String key) {
        return contextMap.containsKey(key);
    }

    public void remove(String key) {
        if(!contextMap.containsKey(key)) {
            return;
        }
        contextMap.remove(key);
    }
}
