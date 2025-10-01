package com.tonic.services.proxy;

import lombok.Getter;
import java.net.*;

@Getter
public class ProxyMetrics
{
    public ProxyMetrics(ProxyInfo proxyInfo) throws Exception {
        this.proxyInfo = proxyInfo;
        InetAddress address = InetAddress.getByName(proxyInfo.getHost());
        SocketAddress sa = new InetSocketAddress(address, proxyInfo.getPort());
        proxy = new Proxy(Proxy.Type.SOCKS, sa);
    }
    private final Proxy proxy;
    private final ProxyInfo proxyInfo;
}