package com.tonic.injector.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replaces matched calls in a target method with calls to the annotated static method, which takes the call's receiver first for an instance call and returns what the call returned.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Redirect
{
    /**
     * @return the target method's name, or "*" for every method of the target class
     */
    String method();

    /**
     * @return the target method's descriptor, or empty to match by name only
     */
    String desc() default "";

    /**
     * @return the call to redirect, an INVOKE pattern
     */
    At at();

    /**
     * @return whether every match is redirected rather than the one at the ordinal
     */
    boolean all() default false;

    /**
     * @return which match to redirect when not all, -1 for the last
     */
    int ordinal() default 0;

    /**
     * @return the part of the method searched
     */
    Slice slice() default @Slice;

    /**
     * @return how many calls the pattern must match across the searched methods, or -1 for any number
     */
    int expectedMatches() default -1;

    /**
     * @return a static boolean field, as "owner/Class.field", that must be true for the redirect to run, the original call running otherwise; empty for none
     */
    String gate() default "";
}
