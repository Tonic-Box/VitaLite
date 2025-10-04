package com.tonic.plugins.authgen;

import lombok.SneakyThrows;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

public class TOTP {
    @SneakyThrows
    public static String getCode(String secret)
    {
        Clock clock = new Clock(30);
        Totp totp = new Totp(secret, clock);
        return totp.now();
    }
}
