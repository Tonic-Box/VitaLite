package com.tonic.services.pathfinder.ui.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;

public class EmptyCollectionTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                JsonElement tree = delegate.toJsonTree(value);

                // Handle arrays of objects (like TransportDto[])
                if (tree.isJsonArray()) {
                    JsonArray array = tree.getAsJsonArray();
                    JsonArray cleanedArray = new JsonArray();

                    for (JsonElement element : array) {
                        if (element.isJsonObject()) {
                            cleanedArray.add(cleanObject(element.getAsJsonObject()));
                        } else {
                            cleanedArray.add(element);
                        }
                    }
                    elementAdapter.write(out, cleanedArray);
                }
                // Handle single objects
                else if (tree.isJsonObject()) {
                    JsonObject cleaned = cleanObject(tree.getAsJsonObject());
                    elementAdapter.write(out, cleaned);
                }
                // Handle primitives and other types
                else {
                    elementAdapter.write(out, tree);
                }
            }

            private JsonObject cleanObject(JsonObject jsonObject) {
                JsonObject result = new JsonObject();

                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    JsonElement value = entry.getValue();

                    // Skip empty arrays - CORRECTED HERE
                    if (value.isJsonArray() && value.getAsJsonArray().size() == 0) {
                        continue;
                    }

                    // Recursively clean nested objects
                    if (value.isJsonObject()) {
                        JsonObject cleaned = cleanObject(value.getAsJsonObject());
                        // Only add non-empty objects (optional - remove this check if you want to keep empty objects)
                        if (cleaned.size() > 0) {
                            result.add(entry.getKey(), cleaned);
                        }
                    } else {
                        result.add(entry.getKey(), value);
                    }
                }

                return result;
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegate.read(in);
            }
        };
    }
}