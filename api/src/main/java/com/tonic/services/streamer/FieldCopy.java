package com.tonic.services.streamer;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Field-by-field copies of the client's objects, for the streamer mirror's snapshots and look-alike copies: every instance field up the class hierarchy, with arrays copied one level deep so writes to an element of either copy stay apart.
 */
final class FieldCopy
{
    private static final ClassValue<Field[]> FIELDS = new ClassValue<>()
    {
        @Override
        protected Field[] computeValue(Class<?> type)
        {
            List<Field> fields = new ArrayList<>();
            for (Class<?> at = type; at != null && at != Object.class; at = at.getSuperclass())
            {
                for (Field field : at.getDeclaredFields())
                {
                    if (!Modifier.isStatic(field.getModifiers()))
                    {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
            }
            return fields.toArray(new Field[0]);
        }
    };

    private static final ClassValue<Constructor<?>> CONSTRUCTORS = new ClassValue<>()
    {
        @Override
        protected Constructor<?> computeValue(Class<?> type)
        {
            try
            {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            }
            catch (NoSuchMethodException e)
            {
                throw new IllegalStateException(type.getName() + " has no constructor without arguments", e);
            }
        }
    };

    private FieldCopy()
    {
    }

    /**
     * @param type a class
     * @return its instance fields and those of its superclasses, accessible
     */
    static Field[] fields(Class<?> type)
    {
        return FIELDS.get(type);
    }

    /**
     * @param array an array
     * @return a new array of the same type holding the same elements
     */
    static Object copyArray(Object array)
    {
        int length = Array.getLength(array);
        Object copy = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, copy, 0, length);
        return copy;
    }

    /**
     * Makes a new object of the same class holding the same field values, its arrays copied one level deep.
     *
     * @param source the object to copy
     * @param <T> its type
     * @return the copy
     * @throws ReflectiveOperationException if the class cannot be made or a field cannot be read or written
     */
    @SuppressWarnings("unchecked")
    static <T> T copy(T source) throws ReflectiveOperationException
    {
        Class<?> type = source.getClass();
        Object copy = CONSTRUCTORS.get(type).newInstance();
        for (Field field : FIELDS.get(type))
        {
            Object value = field.get(source);
            field.set(copy, value != null && value.getClass().isArray() ? copyArray(value) : value);
        }
        return (T) copy;
    }
}
