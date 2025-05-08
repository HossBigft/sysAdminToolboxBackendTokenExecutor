package org.example.config.key_ed25519;

import org.example.config.security.FileAccessPolicy;
import org.example.exceptions.KeyManagerException;
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
import java.util.Optional;


public class KeyManager {
    private static final String ED25519_ALGORITHM = "Ed25519";
    private static final String KEYFILE_PERMISSIONS = "rw-------";
    private static final String KEYFILE_OWNER = "root";
    private static final String KEYFILE_GROUP = "root";
    private final Path keyPath;
    private final FileAccessPolicy accessPolicy;

    public KeyManager(Path keyPath) {
        this.keyPath = keyPath;

        this.accessPolicy = new FileAccessPolicy(keyPath.toFile()).permissions(KEYFILE_PERMISSIONS).owner(KEYFILE_OWNER)
                .group(KEYFILE_GROUP);
        getLogger().debugEntry()
                .message("KeyManager initialized")
                .log();
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }


    public PublicKey getPublicKeyOrFetch(URI keyURI) throws KeyManagerException {
        getLogger().debugEntry()
                .message("Loading public key")
                .log();

        try {
            String base64Key = loadKeyFromFileOrFetch(keyURI);
            PublicKey key = convertToPublicKey(base64Key);

            getLogger().debugEntry()
                    .message("Public key successfully loaded")
                    .field("KeyAlgorithm", ED25519_ALGORITHM)
                    .log();

            return key;
        } catch (KeyManagerException e) {
            getLogger().errorEntry()
                    .message("Failed to load public key")
                    .exception(e)
                    .log();
            throw e;
        }
    }


    private String loadKeyFromFileOrFetch(URI keyURI) throws KeyManagerException {
        getLogger().debugEntry()
                .message("Attempting to load key from file or fetch from server")
                .log();

        Optional<String> fileKey = readKeyFromFile();
        if (fileKey.isPresent()) {
            getLogger().debugEntry()
                    .message("Using key from file")
                    .log();
            return fileKey.get();
        }
        String keyStr = fetchPublicKey(keyURI);
        savePublicKey(keyStr);
        return keyStr;
    }

    private PublicKey convertToPublicKey(String keyStr) throws KeyManagerException {
        getLogger().debugEntry()
                .message("Converting key  string to PublicKey object")
                .field("Key", keyStr)
                .field("KeyLength", keyStr.length())
                .log();

        try {
            byte[] derBytes = Base64.getDecoder().decode(keyStr);

            getLogger().debugEntry()
                    .field("Decode Base64 key", "Success")
                    .field("DecodedLength", derBytes.length)
                    .log();

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ED25519_ALGORITHM);


            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            getLogger().errorEntry()
                    .message("Ed25519 algorithm not supported by this JVM")
                    .exception(e)
                    .log();
            throw new KeyManagerException("Ed25519 algorithm not supported by this JVM", e);
        } catch (InvalidKeySpecException e) {
            getLogger().errorEntry()
                    .message("Invalid key format")
                    .exception(e)
                    .log();
            throw new KeyManagerException("Invalid key format", e);
        } catch (IllegalArgumentException e) {
            getLogger().errorEntry()
                    .message("Invalid Base64 encoding in key")
                    .exception(e)
                    .log();
            throw new KeyManagerException("Invalid Base64 encoding in key", e);
        }
    }

    public Optional<String> readKeyFromFile() {

        getLogger().debugEntry()
                .message("Reading key from file")
                .field("KeyPath", keyPath)
                .log();

        try {
            if (!Files.exists(keyPath)) {
                getLogger().infoEntry()
                        .message("Public key file not found")
                        .field("KeyPath", keyPath)
                        .log();
                return Optional.empty();
            }

            String key = Files.readString(keyPath).trim();

            if (key.isEmpty()) {
                getLogger().warnEntry()
                        .message("Public key file exists but is empty")
                        .field("KeyPath", keyPath)
                        .log();
                return Optional.empty();
            }

            getLogger().debugEntry()
                    .message("Public key successfully loaded from file")
                    .field("KeyPath", keyPath)
                    .field("KeyLength", key.length())
                    .log();
            return Optional.of(key);
        } catch (IOException e) {
            getLogger().warnEntry()
                    .message("Failed to read public key file")
                    .field("KeyPath", keyPath)
                    .field("Error", e.getMessage())
                    .exception(e)
                    .log();
            return Optional.empty();
        }
    }

    public String fetchPublicKey(URI uri) throws KeyManagerException {
        if (uri == null) {
            getLogger().errorEntry()
                    .message("Cannot fetch public key: URI is null")
                    .log();
            throw new KeyManagerException("Public key URI is null");
        }

        getLogger().infoEntry()
                .message("Fetching public key from server")
                .field("URI", uri)
                .log();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

        try {
            getLogger().debugEntry()
                    .message("Sending HTTP request")
                    .field("Method", request.method())
                    .field("URI", request.uri())
                    .log();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            getLogger().debugEntry()
                    .message("Received HTTP response")
                    .field("StatusCode", response.statusCode())
                    .field("Body", response.body())
                    .log();

            if (response.statusCode() != 200) {
                getLogger().errorEntry()
                        .message("Failed to fetch public key: Non-200 HTTP status")
                        .field("StatusCode", response.statusCode())
                        .field("URI", uri)
                        .log();
                throw new KeyManagerException("Failed to fetch public key: HTTP " + response.statusCode());
            }

            String keyStr = response.body();
            if (keyStr == null || keyStr.isBlank()) {
                getLogger().errorEntry()
                        .message("Received empty public key from server")
                        .field("URI", uri)
                        .log();
                throw new KeyManagerException("Received empty public key");
            }

            getLogger().infoEntry()
                    .message("Public key successfully fetched")
                    .field("URI", uri)
                    .field("Key", keyStr)
                    .log();
            return keyStr;
        } catch (IOException e) {
            getLogger().errorEntry()
                    .message("Network error while fetching public key")
                    .field("URI", uri)
                    .exception(e)
                    .log();
            throw new KeyManagerException("Network error while fetching public key", e);
        } catch (InterruptedException e) {
            getLogger().errorEntry()
                    .message("Thread interrupted while fetching public key")
                    .field("URI", uri)
                    .exception(e)
                    .log();

            Thread.currentThread().interrupt();
            throw new KeyManagerException("Interrupted while fetching public key", e);
        }
    }

    private void savePublicKey(String keyStr) throws KeyManagerException {
        if (keyStr == null || keyStr.isBlank()) {
            getLogger().errorEntry()
                    .message("Cannot save empty or null public key")
                    .log();
            throw new KeyManagerException("Cannot save empty or null public key");
        }

        String keyWithoutPem = keyStr.replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");


        getLogger().infoEntry()
                .message("Saving public key to file")
                .field("KeyPath", keyPath)
                .field("KeyLength", keyStr.length())
                .log();

        try {
            Path parentDir = keyPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                getLogger().debugEntry()
                        .message("Creating parent directories for key file")
                        .field("Directory", parentDir)
                        .log();

                Files.createDirectories(parentDir);
            }

            Files.writeString(keyPath, keyWithoutPem);
            accessPolicy.enforce();

            getLogger().infoEntry()
                    .message("Public key successfully saved to file")
                    .field("Key", keyWithoutPem)
                    .field("KeyPath", keyPath)
                    .log();
        } catch (IOException e) {
            getLogger().errorEntry()
                    .message("Failed to save public key to file")
                    .field("KeyPath", keyPath)
                    .exception(e)
                    .log();
            throw new KeyManagerException("Failed to save public key to file: " + keyPath, e);
        }
    }

    public void fetchKeyAndSave(URI keyURI) throws KeyManagerException {
        String newKeyStr = fetchPublicKey(keyURI);
        savePublicKey(newKeyStr);
    }
}
