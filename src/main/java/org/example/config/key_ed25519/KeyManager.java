package org.example.config.key_ed25519;

import org.example.config.core.AppConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class KeyManager {


    public PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Path keyPath = AppConfiguration.getInstance().getPublicKeyPath();
        String base64Key = Files.readString(keyPath).trim();
        byte[] derBytes = Base64.getDecoder().decode(base64Key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePublic(keySpec);
    }


}
