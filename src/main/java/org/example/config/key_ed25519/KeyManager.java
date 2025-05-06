package org.example.config.key_ed25519;

import org.example.config.core.AppConfiguration;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class KeyManager {


    public PublicKey loadPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Path keyPath = AppConfiguration.getInstance().getPublicKeyPath();
        String base64Key;
        try {
            base64Key = Files.readString(keyPath).trim();
        } catch (IOException e) {
            getLogger().infoEntry().message("Public key file not found.").field("File", keyPath).log();
            base64Key = getPublicKeyStr();
            savePublicKey(base64Key);
        }
        byte[] derBytes = Base64.getDecoder().decode(base64Key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePublic(keySpec);
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public String fetchPublicKey(URI link) {
        HttpRequest request = HttpRequest.newBuilder().uri(link).GET().build();
        getLogger().infoEntry().message("Fetching backend public key.").log();
        HttpResponse<String> response;
        try {
            response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            getLogger().errorEntry().message("Failed to fetch public key.").field("URI", link).exception(e).log();
            throw new RuntimeException(e);
        }
        String keyStr = response.body();
        getLogger().debugEntry().message("Public key fetched").field("Public key", keyStr).log();
        AppConfiguration.getInstance().setPublicKeyURI(link.toString());
        return keyStr;
    }


    public void savePublicKey(String keyStr) {
        Path keyPath = AppConfiguration.getInstance().getPublicKeyPath();
        try {
            getLogger().infoEntry().message("Saving public key to file.").field("File", keyPath).log();
            Files.write(keyPath, keyStr.getBytes());
        } catch (IOException e) {
            getLogger().errorEntry().message("Failed to save public key to file").field("File", keyPath).exception(e)
                    .log();
        }
    }

    private String getPublicKeyStr() {
        String keyStr = fetchPublicKey(AppConfiguration.getInstance().getPublicKeyURI());
        savePublicKey(keyStr);
        return keyStr;
    }
}
