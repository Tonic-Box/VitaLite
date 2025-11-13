package com.tonic.services;

import lombok.Getter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AutoLogin
{
    @Getter
    private static String credentials = null;

    public static void setCredentials(String creds) {
        try
        {
            if(creds == null) {
                credentials = null;
                return;
            }
            String[] parts = creds.split(":");
            if(parts.length == 2 || parts.length == 3) {
                credentials = creds;
                return;
            }

            Properties props = readCredentials(creds);
            credentials = props.getProperty("JX_SESSION_ID") + ":" +
                    props.getProperty("JX_CHARACTER_ID") + ":" +
                    props.getProperty("JX_DISPLAY_NAME");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Reads a properties file from the specified path.
     *
     * @param filePath Path to the credentials properties file
     * @return Properties object containing the credentials, or empty Properties if file doesn't exist or error occurs
     */
    public static Properties readCredentials(String filePath) {
        Properties properties = new Properties();

        if (filePath == null) return properties;

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) return properties;

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }
}
