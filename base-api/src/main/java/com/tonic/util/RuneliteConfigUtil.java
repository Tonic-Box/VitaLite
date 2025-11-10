package com.tonic.util;

import com.google.gson.JsonParser;
import com.tonic.util.jagex.CacheClient;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static void verifyCacheAndVersion(int revision)
    {
        if(System.getProperty("forced.runelite.version") != null)
        {
            if(CacheClient.checkForUpdate(revision))
            {
                int result = JOptionPane.showConfirmDialog(
                        null,
                        "There has been a cache update and you are about to be loading an impossible version of runelite. Are you sure you want to proceed? (" + revision + ")",            // message
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION
                );

                if (result != JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        }
        CacheClient.updateCache();
    }

    public static String getLauncherVersion() {
        try
        {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://runelite.net").openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setInstanceFollowRedirects(true);

            StringBuilder html = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line);
                }
            }
            Matcher m = Pattern.compile("https?://[^\"'\\s]+\\.jar").matcher(html);
            String data = m.find() ? m.group() : null;
            if(data == null)
            {
                return "2.7.1";
            }
            return data.split("download/")[1].split("/")[0];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "2.7.1";
    }
}