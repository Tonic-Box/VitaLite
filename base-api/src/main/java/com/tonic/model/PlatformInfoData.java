package com.tonic.model;

import com.tonic.model.ui.PlatformInfoViewer;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class PlatformInfoData
{
    @Getter
    private static final Map<String, String> platformInfo = new HashMap<>();

    public static void post(Map<String, String> info) {
        platformInfo.putAll(info);
        PlatformInfoViewer.getInstance().postUpdate(platformInfo);
    }

    public static void print()
    {
        if(platformInfo.isEmpty()) {
            System.out.println("No platform info available.");
            return;
        }
        platformInfo.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}
