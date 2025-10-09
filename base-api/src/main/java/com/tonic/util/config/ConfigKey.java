package com.tonic.util.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the config key for a property
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConfigKey {
    /**
     * The config property name
     */
    String value();

    /**
     * Default value (as string, will be converted)
     */
    String defaultValue() default "";
}