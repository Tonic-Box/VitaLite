package com.tonic.runelite.jvm;

import com.tonic.util.LauncherVersionUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class JvmParams
{
    public static void set()
    {

        final var hardwareAccelMode = HardwareAccelerationMode.defaultMode(OS.getOs());
        final Map<String, String> jvmProps = new LinkedHashMap<>(hardwareAccelMode.toParams(OS.getOs()));

        if (OS.getOs() == OS.OSType.MacOS)
        {
            jvmProps.put("apple.awt.application.appearance", "system");
        }

        jvmProps.put("runelite.launcher.version", LauncherVersionUtil.getLauncherVersion());
        setJvmParams(jvmProps);
    }
    private static void setJvmParams(final Map<String, String> params)
    {
        for (var entry : params.entrySet())
        {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }
}
