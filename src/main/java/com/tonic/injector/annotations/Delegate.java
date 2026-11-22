package com.tonic.injector.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Has the injector write a native hook's body as one call to a public static method of VitaLite, named here so main never compiles against the code it calls. The call passes the receiver first for an instance hook, then the hook's arguments cast to the method's parameter types, and returns its result cast to the hook's return type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delegate
{
    /**
     * @return the method, as "owner/Class.method"
     */
    String value();
}
