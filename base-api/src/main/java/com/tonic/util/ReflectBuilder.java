package com.tonic.util;

import com.tonic.Static;
import com.tonic.util.reflection.*;
import lombok.SneakyThrows;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A builder for creating a chain of reflection calls.
 */
public class ReflectBuilder
{
    private final Deque<Element> reflectionChain = new ArrayDeque<>();
    private final Object start;

    /**
     * Starts a new reflection chain from the given object.
     * @param start the object to start from
     * @return a new ReflectBuilder
     */
    public static ReflectBuilder of(Object start)
    {
        return new ReflectBuilder(start);
    }

    /**
     * Starts a new reflection chain from the given class name.
     * @param classFqdn the fully qualified name of the class to start from
     * @return a new ReflectBuilder
     */
    public static ReflectBuilder ofClass(String classFqdn)
    {
        try
        {
            Class<?> navButtonClass = Static.getRuneLite()
                    .getRuneLiteMain()
                    .getClassLoader()
                    .loadClass(classFqdn);
            return new ReflectBuilder(navButtonClass);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Failed to load class: " + classFqdn, e);
        }
    }

    /**
     * Looks up a class by its fully qualified name using RuneLite's class loader.
     * @param classFqdn the fully qualified name of the class to look up
     * @return the Class object
     */
    public static Class<?> lookupClass(String classFqdn)
    {
        try
        {
            return Static.getRuneLite()
                    .getRuneLiteMain()
                    .getClassLoader()
                    .loadClass(classFqdn);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Failed to load class: " + classFqdn, e);
        }
    }

    /**
     * Starts a new reflection chain from the RuneLite main instance.
     * @return a new ReflectBuilder
     */
    public static ReflectBuilder runelite()
    {
        return new ReflectBuilder(Static.getRuneLite().getRuneLiteMain());
    }

    private ReflectBuilder(Object start)
    {
        this.start = start;
    }

    /**
     * Creates a new instance of the specified class using the given constructor parameters.
     * @param classFqdn the fully qualified name of the class to instantiate
     * @param parameterTypes the types of the constructor parameters
     * @param args the constructor arguments
     * @return a new ReflectBuilder starting from the created instance
     */
    public static ReflectBuilder newInstance(String classFqdn, Class<?>[] parameterTypes, Object[] args)
    {
        try
        {
            Class<?> clazz = Static.getRuneLite()
                    .getRuneLiteMain()
                    .getClassLoader()
                    .loadClass(classFqdn);

            if(parameterTypes== null || parameterTypes.length == 0)
                return new ReflectBuilder(clazz.getDeclaredConstructor().newInstance());
            else
                return new ReflectBuilder(clazz.getDeclaredConstructor(parameterTypes).newInstance(args));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create instance of class: " + classFqdn, e);
        }
    }

    /**
     * Adds a static field access to the reflection chain.
     * @param name the name of the static field
     * @return this ReflectBuilder
     */
    public ReflectBuilder staticField(String name)
    {
        reflectionChain.offer(new FieldElement(true, name));
        return this;
    }

    /**
     * Adds an instance field access to the reflection chain.
     * @param name the name of the instance field
     * @return this ReflectBuilder
     */
    public ReflectBuilder field(String name)
    {
        reflectionChain.offer(new FieldElement(false, name));
        return this;
    }

    /**
     * Adds a static method call to the reflection chain.
     * @param name the name of the static method
     * @param parameterTypes the types of the method parameters (can be null or empty for no parameters)
     * @param args the method arguments (can be null or empty for no arguments)
     * @return this ReflectBuilder
     */
    public ReflectBuilder staticMethod(String name, Class<?>[] parameterTypes, Object[] args)
    {
        if (parameterTypes == null)
        {
            parameterTypes = new Class<?>[]{};
        }
        if (args == null)
        {
            args = new Object[]{};
        }
        reflectionChain.offer(new MethodElement(true, name, parameterTypes, args));
        return this;
    }

    /**
     * Adds an instance method call to the reflection chain.
     * @param name the name of the instance method
     * @param parameterTypes the types of the method parameters (can be null or empty for no parameters)
     * @param args the method arguments (can be null or empty for no arguments)
     * @return this ReflectBuilder
     */
    public ReflectBuilder method(String name, Class<?>[] parameterTypes, Object[] args)
    {
        if (parameterTypes == null)
        {
            parameterTypes = new Class<?>[]{};
        }
        if (args == null)
        {
            args = new Object[]{};
        }
        reflectionChain.offer(new MethodElement(false, name, parameterTypes, args));
        return this;
    }

    /**
     * Executes the reflection chain and returns the final result.
     * @return the result of the reflection chain
     * @param <T> value
     */
    public <T> T get()
    {
        Object start = this.start;
        try
        {
            while(!reflectionChain.isEmpty())
            {
                Element element = reflectionChain.poll();
                start = element.get(start);
            }
            return (T) start;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to get value from reflection chain", e);
        }
    }

    public String getAsString() {
        return String.valueOf((Object) get());
    }
}
