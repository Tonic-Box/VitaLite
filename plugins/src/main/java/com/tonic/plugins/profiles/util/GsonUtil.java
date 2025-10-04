package com.tonic.plugins.profiles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class GsonUtil {
    public static Gson GSON = new GsonBuilder().create();

    public static <T> T loadJsonResource(Class<?> owner, String resource, Class<T> clazz) {
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, clazz);
            }
        } catch (IOException | JsonSyntaxException e) {
            throw new RuntimeException("Failed to load or parse JSON resource " + resource, e);
        }
    }
}
