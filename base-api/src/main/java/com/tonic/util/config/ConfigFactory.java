package com.tonic.util.config;

import com.tonic.services.ConfigManager;

import java.lang.reflect.Proxy;

public class ConfigFactory {

    /**
     * Create a proxy-based config instance
     */
    public static <T extends VitaConfig> T create(Class<T> configInterface) {
        if (!configInterface.isInterface()) {
            throw new IllegalArgumentException("Must be an interface: " + configInterface.getName());
        }

        if (!VitaConfig.class.isAssignableFrom(configInterface)) {
            throw new IllegalArgumentException("Interface must extend VitaConfig: " + configInterface.getName());
        }

        String configFileName = configInterface.getSimpleName();
        ConfigGroup group = configInterface.getAnnotation(ConfigGroup.class);
        if(group != null)
        {
            configFileName = group.value();
        }

        ConfigManager configManager = new ConfigManager(configFileName);
        ConfigProxyHandler handler = new ConfigProxyHandler(configManager);

        return (T) Proxy.newProxyInstance(
                configInterface.getClassLoader(),
                new Class<?>[] { configInterface },
                handler
        );
    }
}