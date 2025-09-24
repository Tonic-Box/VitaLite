package com.tonic.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.net.URL;
public class CatFacts {
    public static String get(int maxLength)
    {
        try
        {
            URL url;
            if(maxLength != -1)
                url = new URL("https://catfact.ninja/fact?max_length=" + maxLength);
            else
                url = new URL("https://catfact.ninja/fact");
            InputStreamReader reader = new InputStreamReader(url.openStream());
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson( reader, JsonObject.class);
            return jsonObject.get("fact").getAsString();
        }
        catch (Exception ex)
        {
            return "No cats to be found here...";
        }
    }
}