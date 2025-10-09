package com.tonic.util.config;

import com.tonic.services.ConfigManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ConfigProxyHandler implements InvocationHandler {
    private final ConfigManager configManager;

    public ConfigProxyHandler(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle getConfigManager()
        if (method.getName().equals("getConfigManager") && method.getParameterCount() == 0) {
            return configManager;
        }

        String methodName = method.getName();
        ConfigKey annotation = method.getAnnotation(ConfigKey.class);

        // Getter
        if (methodName.startsWith("get") || methodName.startsWith("is") || methodName.startsWith("should")) {
            return handleGetter(method, annotation);
        }

        // Setter
        if (methodName.startsWith("set") && args != null && args.length == 1) {
            handleSetter(method, annotation, args[0]);
            return null;
        }

        throw new UnsupportedOperationException("Method not supported: " + method.getName());
    }

    private Object handleGetter(Method method, ConfigKey annotation) {
        String key = getConfigKey(method, annotation);
        String defaultValue = annotation != null ? annotation.defaultValue() : "";
        Class<?> returnType = method.getReturnType();

        // Boolean
        if (returnType == boolean.class || returnType == Boolean.class) {
            if (defaultValue.isEmpty()) {
                return configManager.getBoolean(key);
            }
            return configManager.getBooleanOrDefault(key, Boolean.parseBoolean(defaultValue));
        }

        // Integer
        if (returnType == int.class || returnType == Integer.class) {
            if (defaultValue.isEmpty()) {
                return configManager.getInt(key);
            }
            return configManager.getIntOrDefault(key, Integer.parseInt(defaultValue));
        }

        // String
        if (returnType == String.class) {
            if (defaultValue.isEmpty()) {
                return configManager.getString(key);
            }
            return configManager.getStringOrDefault(key, defaultValue);
        }

        // Enum
        if (returnType.isEnum()) {
            String value = configManager.getStringOrDefault(key, defaultValue);
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return Enum.valueOf((Class<Enum>) returnType, value);
            } catch (IllegalArgumentException e) {
                if (!defaultValue.isEmpty()) {
                    return Enum.valueOf((Class<Enum>) returnType, defaultValue);
                }
                return null;
            }
        }

        // Long
        if (returnType == long.class || returnType == Long.class) {
            String value = configManager.getStringOrDefault(key, defaultValue);
            return value != null ? Long.parseLong(value) : 0L;
        }

        // Double
        if (returnType == double.class || returnType == Double.class) {
            String value = configManager.getStringOrDefault(key, defaultValue);
            return value != null ? Double.parseDouble(value) : 0.0;
        }

        // Float
        if (returnType == float.class || returnType == Float.class) {
            String value = configManager.getStringOrDefault(key, defaultValue);
            return value != null ? Float.parseFloat(value) : 0.0f;
        }

        throw new UnsupportedOperationException("Unsupported return type: " + returnType);
    }

    private void handleSetter(Method method, ConfigKey annotation, Object value) {
        String key = getConfigKey(method, annotation);

        if (value == null) {
            configManager.setProperty(key, "");
            return;
        }

        // Enum - store as name
        if (value instanceof Enum) {
            configManager.setProperty(key, ((Enum<?>) value).name());
            return;
        }

        // Everything else
        configManager.setProperty(key, value);
    }

    private String getConfigKey(Method method, ConfigKey annotation) {
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        // Derive from method name: getClickStrategy -> clickStrategy
        String methodName = method.getName();
        String prefix = "";

        if (methodName.startsWith("get")) {
            prefix = "get";
        } else if (methodName.startsWith("set")) {
            prefix = "set";
        } else if (methodName.startsWith("is")) {
            prefix = "is";
        } else if (methodName.startsWith("should")) {
            prefix = "should";
        }

        String key = methodName.substring(prefix.length());
        return Character.toLowerCase(key.charAt(0)) + key.substring(1);
    }
}