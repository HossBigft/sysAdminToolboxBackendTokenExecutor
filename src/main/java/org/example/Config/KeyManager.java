package org.example.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyManager {
    private static final Path PUBLIC_KEY= Paths.get("pub.key");

    public PublicKey getPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        String base64Key = Files.readString(PUBLIC_KEY).trim();
        byte[] derBytes = Base64.getDecoder().decode(base64Key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return  kf.generatePublic(keySpec);
    }


}
