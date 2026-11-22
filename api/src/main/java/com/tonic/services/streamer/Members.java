package com.tonic.services.streamer;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Looks up the members of RuneLite classes the streamer mirror reads that are not public, made accessible.
 */
final class Members
{
    private Members()
    {
    }

    /**
     * @param type the declaring class
     * @param name the field's name
     * @return the field, accessible
     * @throws NoSuchFieldException if the class declares no such field
     */
    static Field field(Class<?> type, String name) throws NoSuchFieldException
    {
        return accessible(type.getDeclaredField(name));
    }

    /**
     * @param type the declaring class
     * @param name the method's name
     * @param parameters the method's parameter types
     * @return the method, accessible
     * @throws NoSuchMethodException if the class declares no such method
     */
    static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException
    {
        return accessible(type.getDeclaredMethod(name, parameters));
    }

    /**
     * @param type a class
     * @param name a field's name
     * @return the field the class or the nearest of its superclasses declares, accessible, or null when none does or it cannot be made accessible
     */
    static Field inherited(Class<?> type, String name)
    {
        for (Class<?> declaring = type; declaring != null; declaring = declaring.getSuperclass())
        {
            try
            {
                return field(declaring, name);
            }
            catch (NoSuchFieldException ignored)
            {
            }
            catch (RuntimeException e)
            {
                return null;
            }
        }
        return null;
    }

    /**
     * @param member a member
     * @param <T> the member's type
     * @return the member, made accessible
     */
    static <T extends AccessibleObject> T accessible(T member)
    {
        member.setAccessible(true);
        return member;
    }
}
