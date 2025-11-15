package com.tonic.injector.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Injector annotation for mixin classes.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Mixin {
    String value();

    /**
     * CHEAT/HACK: Identify by extending RL API interface
     * @return true if mixin target identifier is an interface, false if mappings
     */
    boolean isInterface() default false;
}