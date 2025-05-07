package org.example.config.key_ed25519;

import org.example.config.core.AppConfiguration;
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

/**
 * Manages Ed25519 public key operations including loading, fetching, and saving.
 */
public class KeyManager {
    private static final String ED25519_ALGORITHM = "Ed25519";

    private final AppConfiguration appConfig;
    private final CliLogger logger;

    /**
     * Default constructor that uses the singleton AppConfiguration and LogManager.
     */
    public KeyManager() {
        this(AppConfiguration.getInstance(), LogManager.getInstance().getLogger());
    }

    /**
     * Constructor with dependencies injection for better testability.
     *
     * @param appConfig The application configuration
     * @param logger    The logger instance
     */
    public KeyManager(AppConfiguration appConfig,
                      CliLogger logger) {
        this.appConfig = appConfig;
        this.logger = logger;

        logger.debugEntry()
                .message("KeyManager initialized")

                .log();
    }

    /**
     * Loads the public key from file or fetches it if not found.
     *
     * @return The Ed25519 public key
     * @throws KeyManagerException if the key cannot be loaded or generated
     */
    public PublicKey loadPublicKey() throws KeyManagerException {
        logger.debugEntry()
                .message("Loading public key")
                .log();

        try {
            String base64Key = loadKeyFromFileOrFetch();
            PublicKey key = convertToPublicKey(base64Key);

            logger.debugEntry()
                    .message("Public key successfully loaded")
                    .field("KeyAlgorithm", ED25519_ALGORITHM)
                    .log();

            return key;
        } catch (KeyManagerException e) {
            logger.errorEntry()
                    .message("Failed to load public key")
                    .exception(e)
                    .log();
            throw e;
        }
    }

    /**
     * Attempts to load the key from file, fetches from server if not found.
     *
     * @return Base64 encoded public key string
     * @throws KeyManagerException if both file read and fetch operations fail
     */
    private String loadKeyFromFileOrFetch() throws KeyManagerException {
        logger.debugEntry()
                .message("Attempting to load key from file or fetch from server")
                .log();

        Optional<String> fileKey = readKeyFromFile();
        if (fileKey.isPresent()) {
            logger.debugEntry()
                    .message("Using key from file")
                    .log();
            return fileKey.get();
        }

        return fetchAndSavePublicKey();
    }

    /**
     * Converts a Base64 encoded key string to a PublicKey object.
     *
     * @param base64Key The Base64 encoded key string
     * @return The PublicKey object
     * @throws KeyManagerException if key conversion fails
     */
    private PublicKey convertToPublicKey(String base64Key) throws KeyManagerException {
        logger.debugEntry()
                .message("Converting Base64 key to PublicKey object")
                .field("KeyLength", base64Key.length())
                .log();

        try {
            byte[] derBytes = Base64.getDecoder().decode(base64Key);

            logger.debugEntry()
                    .message("Decoded Base64 key")

                    .field("DecodedLength", derBytes.length)
                    .log();

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ED25519_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            logger.debugEntry()
                    .message("Successfully created PublicKey object")

                    .field("KeyAlgorithm", publicKey.getAlgorithm())
                    .field("KeyFormat", publicKey.getFormat())
                    .log();

            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            logger.errorEntry()
                    .message("Ed25519 algorithm not supported by this JVM")

                    .exception(e)
                    .log();
            throw new KeyManagerException("Ed25519 algorithm not supported by this JVM", e);
        } catch (InvalidKeySpecException e) {
            logger.errorEntry()
                    .message("Invalid key format")

                    .exception(e)
                    .log();
            throw new KeyManagerException("Invalid key format", e);
        } catch (IllegalArgumentException e) {
            logger.errorEntry()
                    .message("Invalid Base64 encoding in key")

                    .exception(e)
                    .log();
            throw new KeyManagerException("Invalid Base64 encoding in key", e);
        }
    }

    /**
     * Reads the key from the configured file path.
     *
     * @return Optional containing the key if file exists and is readable, empty otherwise
     */
    private Optional<String> readKeyFromFile() {
        Path keyPath = appConfig.getPublicKeyPath();

        logger.debugEntry()
                .message("Reading key from file")
                .field("KeyPath", keyPath)
                .log();

        try {
            if (!Files.exists(keyPath)) {
                logger.infoEntry()
                        .message("Public key file not found")
                        .field("KeyPath", keyPath)
                        .log();
                return Optional.empty();
            }

            String key = Files.readString(keyPath).trim();

            if (key.isEmpty()) {
                logger.warnEntry()
                        .message("Public key file exists but is empty")
                        .field("KeyPath", keyPath)
                        .log();
                return Optional.empty();
            }

            logger.debugEntry()
                    .message("Public key successfully loaded from file")
                    .field("KeyPath", keyPath)
                    .field("KeyLength", key.length())
                    .log();
            return Optional.of(key);
        } catch (IOException e) {
            logger.warnEntry()
                    .message("Failed to read public key file")
                    .field("KeyPath", keyPath)
                    .field("Error", e.getMessage())
                    .exception(e)
                    .log();
            return Optional.empty();
        }
    }

    /**
     * Fetches the public key from the server and saves it locally.
     *
     * @return The fetched public key string
     * @throws KeyManagerException if fetching fails
     */
    private String fetchAndSavePublicKey() throws KeyManagerException {
        URI uri = appConfig.getPublicKeyURI();

        logger.debugEntry()
                .message("Fetching public key")
                .field("URI", uri)
                .log();

        try {
            String keyStr = fetchPublicKey(uri);
            savePublicKey(keyStr);
            return keyStr;
        } catch (KeyManagerException e) {
            logger.errorEntry()
                    .message("Failed to fetch and save public key")
                    .field("URI", uri)
                    .exception(e)
                    .log();
            throw e;
        }
    }

    /**
     * Fetches the public key from the specified URI.
     *
     * @param uri The URI to fetch the key from
     * @return The public key string
     * @throws KeyManagerException if fetching fails
     */
    public String fetchPublicKey(URI uri) throws KeyManagerException {
        if (uri == null) {
            logger.errorEntry()
                    .message("Cannot fetch public key: URI is null")
                    .log();
            throw new KeyManagerException("Public key URI is null");
        }

        logger.infoEntry()
                .message("Fetching public key from server")
                .field("URI", uri)
                .log();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

        try {
            logger.debugEntry()
                    .message("Sending HTTP request")
                    .field("Method", request.method())
                    .field("URI", request.uri())
                    .log();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            logger.debugEntry()
                    .message("Received HTTP response")
                    .field("StatusCode", response.statusCode())
                    .field("Body", response.body())
                    .log();

            if (response.statusCode() != 200) {
                logger.errorEntry()
                        .message("Failed to fetch public key: Non-200 HTTP status")
                        .field("StatusCode", response.statusCode())
                        .field("URI", uri)
                        .log();
                throw new KeyManagerException("Failed to fetch public key: HTTP " + response.statusCode());
            }

            String keyStr = response.body();
            if (keyStr == null || keyStr.isBlank()) {
                logger.errorEntry()
                        .message("Received empty public key from server")
                        .field("URI", uri)
                        .log();
                throw new KeyManagerException("Received empty public key");
            }

            logger.infoEntry()
                    .message("Public key successfully fetched")
                    .field("URI", uri)
                    .field("Key", keyStr)
                    .log();

            appConfig.setPublicKeyURI(uri.toString());
            return keyStr;
        } catch (IOException e) {
            logger.errorEntry()
                    .message("Network error while fetching public key")
                    .field("URI", uri)
                    .exception(e)
                    .log();
            throw new KeyManagerException("Network error while fetching public key", e);
        } catch (InterruptedException e) {
            logger.errorEntry()
                    .message("Thread interrupted while fetching public key")
                    .field("URI", uri)
                    .exception(e)
                    .log();

            Thread.currentThread().interrupt();
            throw new KeyManagerException("Interrupted while fetching public key", e);
        }
    }

    /**
     * Saves the public key to the configured file path.
     *
     * @param keyStr The key string to save
     * @throws KeyManagerException if saving fails
     */
    public void savePublicKey(String keyStr) throws KeyManagerException {
        if (keyStr == null || keyStr.isBlank()) {
            logger.errorEntry()
                    .message("Cannot save empty or null public key")
                    .log();
            throw new KeyManagerException("Cannot save empty or null public key");
        }

        Path keyPath = appConfig.getPublicKeyPath();

        logger.infoEntry()
                .message("Saving public key to file")
                .field("KeyPath", keyPath)
                .field("KeyLength", keyStr.length())
                .log();

        try {
            Path parentDir = keyPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                logger.debugEntry()
                        .message("Creating parent directories for key file")
                        .field("Directory", parentDir)
                        .log();

                Files.createDirectories(parentDir);
            }

            Files.writeString(keyPath, keyStr);

            logger.infoEntry()
                    .message("Public key successfully saved to file")
                    .field("KeyPath", keyPath)
                    .log();
        } catch (IOException e) {
            logger.errorEntry()
                    .message("Failed to save public key to file")
                    .field("KeyPath", keyPath)
                    .exception(e)
                    .log();
            throw new KeyManagerException("Failed to save public key to file: " + keyPath, e);
        }
    }
}
