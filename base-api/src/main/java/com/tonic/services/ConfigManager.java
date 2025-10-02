package com.tonic.services;

import com.tonic.Logger;
import com.tonic.Static;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistent config service
 */
@Getter
@SuppressWarnings({"ResultOfMethodCallIgnored","unused"})
public class ConfigManager {
    private File configFile;

    private FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    /**
     * init
     */
    public ConfigManager(String config) {
        configFile = new File(Static.VITA_DIR.toFile(), config);
        loadConfigFromFile();
    }


    /**
     * Loads the config
     */
    public void loadConfigFromFile() {
        Parameters params = new Parameters();
        if(!configFile.exists())
        {
            try {
                configFile.getParentFile().mkdirs();
                FileWriter writer = new FileWriter(configFile);
                writer.close();
            } catch (IOException ignored) {
            }
        }
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(params.fileBased().setFile(configFile));
        builder.setAutoSave(true);
    }

    /**
     * loads the config from an arbitrarily defined file path
     * @param fileName file path
     */
    public void loadConfigFromFile(String fileName) {
        configFile = new File(System.getProperty("user.home") + "/VitaX/" + fileName);
        if(!configFile.exists())
        {
            configFile.mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException ignored) {
            }
        }

        loadConfigFromFile();
    }

    /**
     * saves the config
     */
    @SneakyThrows
    public void saveConfig() {
        builder.save();
    }

    /**
     * set a config property
     * @param propertyName name
     * @param value value
     */
    @SneakyThrows
    public void setProperty(String propertyName, Object value) {
        builder.getConfiguration().setProperty(propertyName, value);
    }

    /**
     * reset the config to default values
     */
    @SneakyThrows
    public void reset()
    {
        builder.reset();
        builder.save();
    }

    /**
     * set a property name tied to a config group
     * @param group group
     * @param key name
     * @param value value
     */
    @SneakyThrows
    public void setProperty(String group, String key, Object value) {
        setProperty(group + key, value);
    }

    /**
     * add a new property to config
     * @param propertyName name
     * @param value value
     */
    @SneakyThrows
    public void addProperty(String propertyName, Object value) {
        builder.getConfiguration().addProperty(propertyName, value);
    }

    /**
     * add a new property tied to a config group
     * @param group group
     * @param key name
     * @param value value
     */
    @SneakyThrows
    public void addProperty(String group, String key, Object value) {
        addProperty(group + key, value);
    }

    /**
     * get a config property
     * @param propertyName name
     * @param clazz class
     * @return value
     */
    public <T> T getProperty(String propertyName, Class<T> clazz) {
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                return builder.getConfiguration().get(clazz, propertyName);
        } catch (ConfigurationException ignored) {
        }
        return null;
    }

    /**
     * get a config property
     * @param propertyName name
     * @return value
     */
    public Object getProperty(String propertyName) {
        Object property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getProperty(propertyName);
        } catch (ConfigurationException ignored) {
        }
        return property;
    }

    /**
     * get a config property or a default value if it does not exist
     * @param propertyName name
     * @param defaultValue default value
     * @return value
     */
    public Object getPropertyOrDefault(String propertyName, Object defaultValue) {
        Object property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getProperty(propertyName);
        } catch (ConfigurationException ignored) {
        }
        return property == null ? defaultValue : property;
    }

    /**
     * check if a property exists
     * @param propertyName name
     * @return true if it exists, false otherwise
     */
    public boolean hasProperty(String propertyName) {
        try {
            return builder.getConfiguration().containsKey(propertyName);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get a property from a group
     * @param group group
     * @param key name
     * @return value
     */
    public Object getProperty(String group, String key) {
        return getProperty(group + key);
    }

    /**
     * get a property as a string
     * @param propertyName name
     * @return value
     */
    public String getString(String propertyName) {
        String property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getString(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return property;
    }

    /**
     * get a property as a string or a default value if it does not exist
     * @param propertyName name
     * @param defaultValue default value
     * @return value
     */
    public String getStringOrDefault(String propertyName, String defaultValue) {
        String property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getString(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return property == null ? defaultValue : property;
    }

    /**
     * get a property as a collection
     * @param propertyName name
     * @return value
     */
    public <T> Collection<T> getCollection(String propertyName, Class<T> cls) {
        Collection<T> property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getCollection(cls, propertyName, null);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return property;
    }

    /**
     * get a property as a string from a group
     * @param group group
     * @param key name
     * @return value
     */
    public String getString(String group, String key) {
        return getString(group + key);
    }

    /**
     * get a property as an int
     * @param propertyName name
     * @return value
     */
    public int getInt(String propertyName) {
        try {
            if (!builder.getConfiguration().containsKey(propertyName)) return 0;
            return builder.getConfiguration().getInt(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * get a property as an int or a default value if it does not exist
     * @param propertyName name
     * @param defaultValue default value
     * @return value
     */
    public int getIntOrDefault(String propertyName, int defaultValue) {
        try {
            if (!builder.getConfiguration().containsKey(propertyName)) return 0;
            return builder.getConfiguration().getInt(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return defaultValue;
    }

    /**
     * get a property as an int from a config group
     * @param group group
     * @param key name
     * @return value
     */
    public int getInt(String group, String key) {
        return getInt(group + key);
    }

    /**
     * get a property as a boolean
     * @param propertyName property
     * @return value
     */
    public boolean getBoolean(String propertyName) {
        try {
            if (!builder.getConfiguration().containsKey(propertyName)) return false;
            return builder.getConfiguration().getBoolean(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * get a property as a boolean or a default value if it does not exist
     * @param propertyName name
     * @param defaultValue default value
     * @return value
     */
    public boolean getBooleanOrDefault(String propertyName, boolean defaultValue) {
        try {
            if (!builder.getConfiguration().containsKey(propertyName)) return false;
            return builder.getConfiguration().getBoolean(propertyName);
        } catch (ConfigurationException e) {
            Logger.error(e);
        }
        return defaultValue;
    }

    /**
     * get a property as a boolean from a group
     * @param group group
     * @param key name
     * @return value
     */
    public boolean getBoolean(String group, String key) {
        return getBoolean(group + key);
    }

    /**
     * ensures values exist in a config, if they do not it created them with the default
     * value supplied.
     * @param configMap map of key -> value
     * @return true if a value was missing, false if no value was missing
     */
    public boolean ensure(Map<String,Object> configMap)
    {
        AtomicBoolean requiredChange = new AtomicBoolean(false);
        configMap.forEach((key, value) -> {
            if (getProperty(key) == null) {
                addProperty(key, value);
                requiredChange.set(true);
            }
        });
        return requiredChange.get();
    }
}