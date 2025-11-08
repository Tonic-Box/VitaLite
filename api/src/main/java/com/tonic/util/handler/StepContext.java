package com.tonic.util.handler;

import java.util.HashMap;
import java.util.Map;

public class StepContext
{
    private final Map<String, Object> contextMap = new HashMap<>();

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
        contextMap.remove(key);
    }
}
