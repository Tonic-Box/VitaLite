package com.tonic.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tonic.VitaLite;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import com.tonic.util.dto.JMethod;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides methods to retrieve mappings for classes, methods, and fields.
 */
@Getter
public class MappingProvider
{
    @Getter
    private static final List<JClass> mappings = new ArrayList<>();

    static
    {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try (InputStream inputStream = VitaLite.class.getResourceAsStream("injector/mappings.json")) {
            if( inputStream == null) {
                throw new IOException("Mappings file not found");
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String fileContent = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            mappings.addAll(gson.fromJson(fileContent, new TypeToken<List<JClass>>(){}.getType()));
        }
        catch (IOException e)
        {
            //will fail in release builds
        }
    }

    public static JClass getClass(String name)
    {
        for (JClass jClass : mappings)
        {
            if (jClass.getName() != null && jClass.getName().equals(name))
            {
                return jClass;
            }
        }
        return null;
    }

    public static JClass getClassByObfuscatedName(String obfuscatedName)
    {
        for (JClass jClass : mappings)
        {
            if (jClass.getObfuscatedName().equals(obfuscatedName))
            {
                return jClass;
            }
        }
        return null;
    }

    public static JMethod getMethod(JClass owner, String name)
    {
        for (JMethod jMethod : owner.getMethods())
        {
            if (jMethod.getName() != null && jMethod.getName().equals(name))
            {
                return jMethod;
            }
        }
        return getStaticMethod(name);
    }

    public static JMethod getMethodByObfuscatedName(JClass owner, String obfuscatedName)
    {
        for (JMethod jMethod : owner.getMethods())
        {
            if (jMethod.getObfuscatedName().equals(obfuscatedName))
            {
                return jMethod;
            }
        }
        return null;
    }

    public static JMethod getStaticMethod(String name)
    {
        for (JClass jClass : mappings)
        {
            for (JMethod jMethod : jClass.getMethods())
            {
                if (jMethod.getName() != null && jMethod.getName().equals(name) && jMethod.isStatic())
                {
                    return jMethod;
                }
            }
        }
        return null;
    }

    public static JField getField(@Nullable JClass owner, String name)
    {
        if(owner == null)
            return getStaticField(name);
        for (JField jField : owner.getFields())
        {
            if (jField.getName() != null && jField.getName().equals(name))
            {
                return jField;
            }
        }
        return getStaticField(name);
    }

    public static JField getFieldByObfuscatedName(JClass owner, String obfuscatedName)
    {
        for (JField jField : owner.getFields())
        {
            if (jField.getObfuscatedName().equals(obfuscatedName))
            {
                return jField;
            }
        }
        return null;
    }

    public static JField getStaticField(String name)
    {
        for (JClass jClass : mappings)
        {
            for (JField jField : jClass.getFields())
            {
                if (jField.getName() != null && jField.getName().equals(name) && jField.isStatic())
                {
                    return jField;
                }
            }
        }
        return null;
    }
}
