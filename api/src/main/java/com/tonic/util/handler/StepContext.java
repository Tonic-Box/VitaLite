package com.tonic.util.handler;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StepContext
{
    private final Map<String, Object> contextMap = new HashMap<>();

    public List<Object> values() {
        return List.copyOf(contextMap.values());
    }

    public void put(String key, Object value) {
        contextMap.put(key, value);
    }

    public <T> T get(String key) {
        if(!contextMap.containsKey(key)) {
            return null;
        }
        return (T) contextMap.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        if(!contextMap.containsKey(key)) {
            return null;
        }
        return clazz.cast(contextMap.get(key));
    }

    public void remove(String key) {
        if(!contextMap.containsKey(key)) {
            return;
        }
        contextMap.remove(key);
    }
}
