package com.tonic.services.proxy;

import com.tonic.Logger;
import lombok.Getter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

public class ProxyManager
{
    @Getter
    private static volatile ProxyInfo proxy = null;

    public static void process(String input) throws Exception {
        String[] parts = input.split(":");
        if(parts.length == 2)
        {
            //addProxy(parts[0], Integer.parseInt(parts[1]));
            SocksProxyUtil.setProxy(parts[0], Integer.parseInt(parts[1]));
            proxy = new ProxyInfo(null, null, parts[0], Integer.parseInt(parts[1]));
            System.out.println("Connected to Proxy [" + parts[0] + ":" + parts[1] + "]");
        }
        else if(parts.length == 4)
        {
            //addProxy(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]);
            SocksProxyUtil.setProxy(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]);
            proxy = new ProxyInfo(parts[2], parts[3], parts[0], Integer.parseInt(parts[1]));
            System.out.println("Connected to Proxy [" + parts[0] + ":" + parts[1] + ":{Auth}]");
        }
        else
        {
            System.out.println("Invalid proxy format. Use host:port or host:port:user:pass");
        }
    }

//    public static void addProxy(String host, int port) throws Exception {
//        ProxyInfo proxyInfo = new ProxyInfo(null, null, host, port);
//        proxy = new ProxyMetrics(proxyInfo);
//    }
//
//    public static void addProxy(String host, int port, String user, String pass) throws Exception {
//        ProxyInfo proxyInfo = new ProxyInfo(user, pass, host, port);
//        proxy = new ProxyMetrics(proxyInfo);
//        Authenticator.setDefault(authenticator);
//    }
//
//    @Getter
//    private static final Authenticator authenticator = new Authenticator()
//    {
//        @Override
//        public PasswordAuthentication getPasswordAuthentication()
//        {
//            if(proxy == null || proxy.getProxyInfo().getUser() == null || proxy.getProxyInfo().getPass() == null)
//            {
//                return (new PasswordAuthentication("username", "password".toCharArray()));
//            }
//            return (new PasswordAuthentication(proxy.getProxyInfo().getUser(), proxy.getProxyInfo().getPass().toCharArray()));
//        }
//    };
}