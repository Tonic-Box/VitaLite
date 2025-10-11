package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;

/**
 * Varbit and Varp related API
 */
public class VarAPI
{
    /**
     * Gets the value of a varbit
     * @param varbit the varbit id
     * @return the value of the varbit
     */
    public static int getVar(int varbit)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarbitValue(varbit));
    }

    /**
     * Gets the value of a varp
     * @param varp the varp id
     * @return the value of the varp
     */
    public static int getVarp(int varp)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarpValue(varp));
    }

    public static int getVarcInteger(int varc)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarcIntValue(varc));
    }

    public static String getVarcString(int varc)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarcStrValue(varc));
    }

    /**
     * Sets the value of a varbit
     * @param varbit the varbit id
     * @param value the value to set
     */
    public static void setVar(int varbit, int value)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.setVarbit(varbit, value));
    }
}
