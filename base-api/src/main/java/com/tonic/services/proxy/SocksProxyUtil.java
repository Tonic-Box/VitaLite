package com.tonic.services.proxy;

import java.io.IOException;
import java.net.*;

public final class SocksProxyUtil {

    private static Proxy proxy;
    private static PasswordAuthentication auth;

    private SocksProxyUtil() {}

    public static void setProxy(String host, int port) {
        setProxy(host, port, null, null);
    }

    public static void setProxy(String host, int port, String username, String password) {
        System.setProperty("socksProxyHost", host);
        System.setProperty("socksProxyPort", String.valueOf(port));

        proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

        if (username != null && password != null) {
            System.setProperty("java.net.socks.username", username);
            System.setProperty("java.net.socks.password", password);

            auth = new PasswordAuthentication(username, password.toCharArray());

            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestingHost().equals(host)) {
                        return auth;
                    }
                    return null;
                }
            });
        }

        verify();
    }

    public static Proxy getProxy() {
        return proxy != null ? proxy : Proxy.NO_PROXY;
    }

    public static URLConnection openConnection(URL url) throws IOException {
        return url.openConnection(getProxy());
    }

    public static void clearProxy() {
        proxy = null;
        auth = null;
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("java.net.socks.username");
        System.clearProperty("java.net.socks.password");
        Authenticator.setDefault(null);
    }

    public static boolean isProxySet() {
        return System.getProperty("socksProxyHost") != null
                && System.getProperty("socksProxyPort") != null;
    }

    public static void verify() {
        String host = System.getProperty("socksProxyHost");
        String port = System.getProperty("socksProxyPort");

        if (host == null || port == null) {
            throw new IllegalStateException("SOCKS proxy not configured");
        }

        // Direct socket to proxy server (bypasses JVM proxy settings)
        try (Socket socket = new Socket(Proxy.NO_PROXY)) {
            socket.connect(new InetSocketAddress(host, Integer.parseInt(port)), 3000);
        } catch (Exception e) {
            throw new IllegalStateException("SOCKS proxy unreachable at " + host + ":" + port, e);
        }

        // Test actual traffic through the proxy
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            if (responseCode != 200) {
                throw new IllegalStateException("SOCKS proxy returned status " + responseCode);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("SOCKS proxy failed to route traffic", e);
        }
    }
}