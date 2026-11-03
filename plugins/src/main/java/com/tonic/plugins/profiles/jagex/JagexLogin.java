package com.tonic.plugins.profiles.jagex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The launcher's browser sign-in: the user signs in to account.jagex.com in their browser and the OAuth
 * result comes back to a one-shot server on the loopback. The registered redirect is {@code http://localhost},
 * so the server listens on port 80; the result travels in the URL fragment, which a server never sees, so
 * {@code /} serves a page whose script rewrites {@code localhost/#...} to {@code localhost/capture?...}.
 * One instance serves one sign-in and can be cancelled from another thread.
 */
public class JagexLogin {
    public static final String AUTHORIZE_ENDPOINT = "https://account.jagex.com/oauth2/auth";
    public static final String CLIENT_ID = "1fddee4e-b100-4f4e-b2b0-097f9088f9d2";
    public static final int LOOPBACK_PORT = 80;
    static final int CONNECTION_BUDGET = 12;
    private static final int NONCE_BYTES = 48;
    private static final int STATE_BYTES = 12;
    private static final String SHIM_PAGE = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n"
            + "<html><head><script>"
            + "var u=window.location.href;"
            + "if(u.indexOf('/#')!==-1){window.location.replace(u.replace('/#','/capture?'));}"
            + "else{document.body.innerText='No login fragment received.';}"
            + "</script></head><body>Completing sign-in...</body></html>";
    private static final String DONE_PAGE = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n"
            + "<html><body style=\"font-family:sans-serif\">"
            + "<h2>Sign-in received, you can close this tab.</h2></body></html>";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int port;
    private final String state = randomToken(STATE_BYTES);
    private final String authorizeUrl = AUTHORIZE_ENDPOINT + "?response_type=id_token+code&client_id=" + CLIENT_ID
            + "&nonce=" + percentEncode(randomToken(NONCE_BYTES)) + "&state=" + percentEncode(state)
            + "&prompt=login&scope=openid+offline";
    private volatile ServerSocket server;
    private volatile boolean cancelled;

    public JagexLogin() {
        this(LOOPBACK_PORT);
    }

    /**
     * A sign-in served on another loopback port, for tests.
     *
     * @param port the loopback port
     */
    public JagexLogin(int port) {
        this.port = port;
    }

    /**
     * @return the URL the browser signs in at, with this sign-in's random nonce and state
     */
    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    /**
     * Binds the loopback, opens the browser at the authorize URL and serves requests until the sign-in comes
     * back, the connection budget or the timeout runs out, or the sign-in is cancelled.
     *
     * @param openBrowser opens the authorize URL; it is called once the loopback is listening
     * @param timeout how long to wait for the browser
     * @return the id token
     * @throws JagexLoginException when no id token came back, with the reason
     * @throws IOException when the loopback cannot be served for another reason
     */
    public String await(Consumer<String> openBrowser, Duration timeout) throws IOException, JagexLoginException {
        long deadline = System.nanoTime() + timeout.toNanos();
        ServerSocket socket;
        try {
            socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        } catch (BindException e) {
            throw new JagexLoginException(JagexLoginException.Kind.PORT_IN_USE);
        }
        server = socket;
        try (ServerSocket listening = socket) {
            if (cancelled) {
                throw new JagexLoginException(JagexLoginException.Kind.CANCELLED);
            }
            openBrowser.accept(authorizeUrl);
            for (int budget = CONNECTION_BUDGET; budget > 0; budget--) {
                long remaining = (deadline - System.nanoTime()) / 1_000_000L;
                if (remaining <= 0) {
                    throw new JagexLoginException(JagexLoginException.Kind.LOOPBACK_TIMEOUT);
                }
                listening.setSoTimeout((int) Math.min(Integer.MAX_VALUE, remaining));
                Socket connection;
                try {
                    connection = listening.accept();
                } catch (SocketTimeoutException e) {
                    throw new JagexLoginException(JagexLoginException.Kind.LOOPBACK_TIMEOUT);
                } catch (SocketException e) {
                    if (cancelled) {
                        throw new JagexLoginException(JagexLoginException.Kind.CANCELLED);
                    }
                    throw e;
                }
                Map<String, String> callback = serve(connection);
                if (callback != null) {
                    return checked(callback);
                }
            }
            throw new JagexLoginException(JagexLoginException.Kind.LOOPBACK_TIMEOUT);
        } finally {
            server = null;
        }
    }

    /**
     * Ends a running await from another thread, or the next one before it opens the browser.
     */
    public void cancel() {
        cancelled = true;
        ServerSocket socket = server;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String checked(Map<String, String> callback) throws JagexLoginException {
        if (callback.get("error") != null) {
            throw new JagexLoginException(JagexLoginException.Kind.AUTHORIZATION_DENIED);
        }
        if (!state.equals(callback.get("state"))) {
            throw new JagexLoginException(JagexLoginException.Kind.STATE_MISMATCH);
        }
        String idToken = callback.get("id_token");
        if (idToken == null || idToken.isEmpty()) {
            throw new JagexLoginException(JagexLoginException.Kind.NO_ID_TOKEN);
        }
        return idToken;
    }

    private static Map<String, String> serve(Socket connection) {
        try (Socket socket = connection) {
            socket.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            String requestLine = in.readLine();
            if (requestLine == null) {
                return null;
            }
            boolean capture = pathOf(requestLine).startsWith("/capture");
            OutputStream out = socket.getOutputStream();
            out.write((capture ? DONE_PAGE : SHIM_PAGE).getBytes(StandardCharsets.UTF_8));
            out.flush();
            return capture ? parseQuery(requestLine) : null;
        } catch (IOException e) {
            return null;
        }
    }

    static Map<String, String> parseQuery(String requestLine) {
        String target = targetOf(requestLine);
        int query = target.indexOf('?');
        Map<String, String> params = new HashMap<>();
        if (query >= 0) {
            for (String pair : target.substring(query + 1).split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                params.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return params;
    }

    static String pathOf(String requestLine) {
        String target = targetOf(requestLine);
        int query = target.indexOf('?');
        return query < 0 ? target : target.substring(0, query);
    }

    private static String targetOf(String requestLine) {
        String[] tokens = requestLine.trim().split(" ");
        return tokens.length > 1 ? tokens[1] : "/";
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return text;
        }
    }

    static String randomToken(int length) {
        byte[] raw = new byte[length];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    static String percentEncode(String text) {
        StringBuilder out = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~';
            if (unreserved) {
                out.append((char) c);
            } else {
                out.append('%').append(String.format("%02X", c));
            }
        }
        return out.toString();
    }
}
