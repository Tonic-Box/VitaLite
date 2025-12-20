package com.tonic.util;

import com.tonic.Logger;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import com.tonic.util.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class ExceptionUtil
{
    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private static Map<Integer, Pair<String, String>> parseStackTrace(String stackTrace) {
        Map<Integer, Pair<String, String>> stackTraceMap = new HashMap<>();
        String[] parts = stackTrace.split("\n");
        String[] parts2;
        for(int i = 1; i < parts.length; i++)
        {
            if(!parts[i].contains("//"))
                continue;
            parts2 = parts[i].split("//")[1].split("\\(")[0].split("\\.");
            stackTraceMap.put(i - 1, Pair.of(parts2[0], parts2[1]));
        }

        return stackTraceMap;
    }

    public static String formatExceptionSimple(Throwable throwable)
    {
        String data = getStackTraceAsString(throwable);
        var map = parseStackTrace(data);

        String obfuClazz = map.get(0).getKey();
        String obfuMethod = map.get(0).getValue();
        JClass jClazz = MappingProvider.getMappings().stream()
                .filter(c -> c.getObfuscatedName().equals(obfuClazz))
                .findFirst().orElse(null);
        JMethod jMethod = null;

        String out = data.split("\n")[0] + " @ [" + obfuClazz + "::" + obfuMethod + "]";

        if(jClazz != null)
        {
            jMethod = jClazz.getMethods().stream()
                    .filter(m -> m.getObfuscatedName().equals(obfuMethod))
                    .findFirst()
                    .orElse(null);
        }

        if(jMethod != null)
        {
            out += " " + jClazz.getName() + "::" + jMethod.getName();
        }

        return out;
    }

    public static String formatException(Throwable throwable)
    {
        try
        {
            throwable.printStackTrace();
            String data = getStackTraceAsString(throwable);
            var map = parseStackTrace(data);
            StringBuilder out = new StringBuilder("# Exception: " + data.split("\n")[0] + "\n");
            for(var entry : map.entrySet())
            {
                String obfuClazz = entry.getValue().getKey();
                String obfuMethod = entry.getValue().getValue();
                JClass jClazz = MappingProvider.getMappings().stream()
                        .filter(c -> c.getObfuscatedName().equals(obfuClazz))
                        .findFirst().orElse(null);
                JMethod jMethod = null;

                if(jClazz != null)
                {
                    jMethod = jClazz.getMethods().stream()
                            .filter(m -> m.getObfuscatedName().equals(obfuMethod))
                            .findFirst()
                            .orElse(null);
                }

                out.append("\t").append(entry.getKey()).append(") [").append(obfuClazz).append("::").append(obfuMethod).append("]");

                if(jMethod == null)
                {
                    out.append("\n");
                    continue;
                }

                out.append(" ").append(jClazz.getName()).append("::").append(jMethod.getName()).append("\n");
            }
            return out.toString();
        }
        catch (Exception e)
        {
            Logger.error(e);
        }
        return "???????";
    }
}
