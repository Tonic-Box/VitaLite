package com.tonic.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class HashUtil {
    public static String computeSha256(Path path) {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, md))
            {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {}
            }
            byte[] digest = md.digest();
            // convert to hex
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
