package com.tonic.services.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProxyInfo {
    private final String user;
    private final String pass;
    private final String host;
    private final int port;

    @Override
    public String toString()
    {
        if(user == null || pass == null)
        {
            return host + ":" + port;
        }
        return user + ":" + pass + ":" + host + ":" + port;
    }
}