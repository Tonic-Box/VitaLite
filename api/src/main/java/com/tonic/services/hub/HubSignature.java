package com.tonic.services.hub;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Checks the official repository's plugins.json against its plugins.json.sig: ECDSA P-256 over SHA-256 of the manifest's
 * exact bytes, made with the key only the official repository's maintainer holds.
 */
public final class HubSignature {
    static final String OFFICIAL_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEO+tpWhwrHfctiC3QAiqCIVgL4ANbHG8mP5kdxDRWO2CtkisrOKUy8O0Slud1rgE6idbs0PfK0WLb7MQOKG4ffQ==";

    private HubSignature() {
    }

    /**
     * @param manifest  the manifest's bytes as downloaded
     * @param signature the signature file's text, Base64
     * @return whether the official key signed exactly these bytes
     */
    public static boolean official(byte[] manifest, String signature) {
        return verify(manifest, signature, OFFICIAL_PUBLIC_KEY);
    }

    /**
     * @param data      the signed bytes
     * @param signature the signature, Base64
     * @param publicKey the public key, Base64 X.509
     * @return whether the signature is valid for the data
     */
    public static boolean verify(byte[] data, String signature, String publicKey) {
        if (data == null || signature == null) {
            return false;
        }
        try {
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.trim())));
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(data);
            return verifier.verify(Base64.getDecoder().decode(signature.trim().getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
