package com.tonic.util.config;

/**
 * Marker interface for proxy-based configs
 */
public interface VitaConfig {

    default <T> T getInstance()
    {
        return (T) ConfigFactory.create(getClass());
    }
}