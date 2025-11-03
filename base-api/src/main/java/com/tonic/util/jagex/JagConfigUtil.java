package com.tonic.util.jagex;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Getter
public class JagConfigUtil
{
    protected final Map<String, String> config;
    protected final HashMap<String, String> gameParameters = new HashMap<>();
    protected final HashMap<String, String> appletParameters = new HashMap<>();

    public JagConfigUtil(int world) {
        config = readJavConfig(world);
        filterParameters(world);
    }

    public int getCurrentRevision()
    {
        try
        {
            return Integer.parseInt(config.get("param25"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        return -1;
    }

    private Map<String, String> readJavConfig(int world)
    {
        Map<String, String> configMap = new HashMap<>();
        try (Scanner scanner = new Scanner(new URL("http://oldschool" + world + ".runescape.com/jav_config.ws").openStream())) {
            while (scanner.hasNextLine()) {
                String[] parts = scanner.nextLine().split("=", 3);
                configMap.put(parts[0] + (parts.length == 3 ? parts[1] : ""), parts[parts.length - 1]);
            }
        } catch (IOException ignored) {
        }
        return configMap;
    }

    private void filterParameters(int world) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("http://oldschool" + world + ".runescape.com/jav_config.ws").openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf("=");
                if (index == -1) continue;

                String segment = line.substring(0, index);
                if (segment.equals("msg")) continue;

                int index1 = line.indexOf("=", index + 2);
                if (segment.equals("param")) {
                    gameParameters.put(line.substring(index + 1, index1), line.substring(index1 + 1));
                } else {
                    appletParameters.put(segment, line.substring(index + 1));
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public String getValue(String key)
    {
        return config.get(key);
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            out.append(entry.getKey()).append(" => ").append(entry.getValue()).append("\n");
        }
        return out.toString();
    }
}