package com.tonic.model;

import com.tonic.services.ConfigManager;

import java.io.*;

public class DeviceID
{
    private static final ConfigManager cachedUUIDProperties = new ConfigManager("CachedUUID");

    public static String getCachedUUID(String identifier)
    {
        return cachedUUIDProperties.getString(identifier);
    }

    public static void writeCachedUUID(String identifier, String UUID)
    {
        cachedUUIDProperties.setProperty(identifier, UUID);
    }

    public static String vanillaGetDeviceID(int osType)
    {
        String command = "";
        String deviceId = "12345678-0000-0000-0000-123456789012";
        switch(osType) {
            case 1:
                command = "wmic csproduct get UUID";
                break;
            case 2:
                command = "system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }'";
                break;
            case 3:
                command = "cat /etc/machine-id";
                break;
            default:
                return "Unknown";
        }

        BufferedReader outputReader = null;

        try {
            Process cmdProcess = Runtime.getRuntime().exec(command);
            outputReader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream()));
            StringBuilder output = new StringBuilder();

            String line;
            while ((line = outputReader.readLine()) != null) {
                output.append(line).append("\n");
            }

            if (osType == 1) {
                deviceId = output.substring(output.indexOf("\n"), output.length()).trim();
            } else if (osType == 2) {
                int uuidEndIndex = output.indexOf("UUID: ") + 36;
                deviceId = output.substring(output.indexOf("UUID: "), uuidEndIndex).replace("UUID: ", "");
            } else {
                if (output.length() == 33) {
                    output = new StringBuilder(output.substring(0, output.length() - 1));
                }

                if (output.length() == 32) {
                    output.insert(20, "-");
                    output.insert(16, "-");
                    output.insert(12, "-");
                    output.insert(8, "-");
                    deviceId = output.toString();
                } else {
                    deviceId = "12345678-0000-0000-0000-123456789012";
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (outputReader != null) {
                    outputReader.close();
                }
            } catch (IOException ignored) {
            }

        }

        return deviceId;
    }
}
