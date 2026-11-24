package com.tonic.services.pathfinder.transports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.tonic.services.pathfinder.requirements.Requirements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The one Gson every transport file is read and written with: an entry without a requirements block loads with none, and writing drops a requirements block that holds nothing, every empty list inside one and the code-only requirements, so a transport carries only the kinds it uses.
 */
public final class TransportJson
{
    /**
     * The Gson.
     */
    public static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(new CompactRequirements()).setPrettyPrinting().create();

    private TransportJson()
    {
    }

    /**
     * Reads a transports file, working each tile's packed key out from its coordinates.
     *
     * @param json the file's text
     * @return the transports
     */
    public static List<Transport> read(String json)
    {
        Transport[] read = GSON.fromJson(json, Transport[].class);
        List<Transport> transports = new ArrayList<>(read == null ? Arrays.asList() : Arrays.asList(read));
        for (Transport transport : transports)
        {
            transport.repack();
        }
        return transports;
    }

    /**
     * @param transports the transports
     * @return them as a transports file
     */
    public static String write(List<Transport> transports)
    {
        return GSON.toJson(transports.toArray(new Transport[0]), Transport[].class);
    }

    private static final class CompactRequirements implements TypeAdapterFactory
    {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            Class<? super T> raw = type.getRawType();
            boolean transport = Transport.class.isAssignableFrom(raw);
            boolean requirements = Requirements.class.isAssignableFrom(raw);
            if (!transport && !requirements)
            {
                return null;
            }
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonElement tree = delegate.toJsonTree(value);
                    if (tree.isJsonObject()) {
                        JsonObject object = tree.getAsJsonObject();
                        if (requirements) {
                            compact(object);
                        } else {
                            JsonElement block = object.get("requirements");
                            if (block != null && block.isJsonObject()) {
                                compact(block.getAsJsonObject());
                                if (block.getAsJsonObject().size() == 0) {
                                    object.remove("requirements");
                                }
                            } else if (block != null && block.isJsonNull()) {
                                object.remove("requirements");
                            }
                        }
                    }
                    elements.write(out, tree);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    return delegate.fromJsonTree(elements.read(in));
                }
            };
        }

        private static void compact(JsonObject block)
        {
            block.remove("otherRequirements");
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(block.entrySet()))
            {
                JsonElement value = entry.getValue();
                if (value.isJsonNull() || (value.isJsonArray() && ((JsonArray) value).size() == 0))
                {
                    block.remove(entry.getKey());
                }
            }
        }
    }
}
