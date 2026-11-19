package com.tonic.services.hotswapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a plugin class whose jar has every class loaded when the plugin is loaded, for plugins that rely on their classes already being loaded; without it only the jar's plugin classes are, and the rest load when used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoadAllClasses
{
}
