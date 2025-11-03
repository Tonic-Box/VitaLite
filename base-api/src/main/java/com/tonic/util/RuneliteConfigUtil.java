package com.tonic.util;

import com.google.gson.JsonParser;
import com.sun.tools.javac.Main;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

public class RuneliteConfigUtil
{
    public static String fetchUrl()
    {
        String injectedVersion   = getRuneLiteVersion();
        String injectedFilename  = "injected-client-" + injectedVersion + ".jar";
        return "https://repo.runelite.net/net/runelite/injected-client/" + injectedVersion + "/" + injectedFilename;
    }
    public static JarFile fetchGamePack() throws Exception
    {
        String injectedUrl = fetchUrl();
        URL jarUrl = new URL("jar:" + injectedUrl + "!/");
        return ((JarURLConnection) jarUrl.openConnection()).getJarFile();
    }

    public static String getRuneLiteVersion() {
        String forcedVersion = System.getProperty("forced.runelite.version");
        if(forcedVersion != null && !forcedVersion.isEmpty()) {
            return forcedVersion;
        }
        try {
            URL url = new URL("https://static.runelite.net/bootstrap.json");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                return JsonParser.parseString(json.toString())
                        .getAsJsonObject()
                        .get("version")
                        .getAsString();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }
}