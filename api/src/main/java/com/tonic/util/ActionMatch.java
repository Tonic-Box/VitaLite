package com.tonic.util;

/**
 * Finds a menu action by name, preferring an exact match so that "Tether" does not pick "Untether".
 */
public final class ActionMatch
{
    private ActionMatch()
    {
    }

    /**
     * Finds an action: the first exact match ignoring case, otherwise the first action that contains the name.
     *
     * @param actions the actions, which may hold nulls
     * @param action the action name
     * @return the index, or -1 when none matches
     */
    public static int indexOf(String[] actions, String action)
    {
        if (actions == null || action == null)
        {
            return -1;
        }
        for (int i = 0; i < actions.length; i++)
        {
            if (actions[i] != null && actions[i].equalsIgnoreCase(action))
            {
                return i;
            }
        }
        String part = action.toLowerCase();
        for (int i = 0; i < actions.length; i++)
        {
            if (actions[i] != null && actions[i].toLowerCase().contains(part))
            {
                return i;
            }
        }
        return -1;
    }
}
