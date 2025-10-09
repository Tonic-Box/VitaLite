package com.tonic.plugins.multiclientutils.model;

import com.tonic.Logger;
import com.tonic.services.ipc.Message;
import lombok.Getter;

public class MultiMessage
{
    @Getter
    private final String sender;
    @Getter
    private final String command;
    private final String[] args = new String[10];
    public MultiMessage(Message message)
    {
        sender = message.get("sender").toString();
        command = message.get("command").toString();
        String key = "arg";
        int i = -1;
        while(message.get(key + ++i) != null)
        {
            args[i] = message.get(key + i).toString();
        }
    }

    public String getString(int i)
    {
        if(i >= args.length)
            return null;
        return args[i];
    }

    public int getInt(int i)
    {
        if(i >= args.length)
            return -1;
        return Integer.parseInt(args[i]);
    }
}
