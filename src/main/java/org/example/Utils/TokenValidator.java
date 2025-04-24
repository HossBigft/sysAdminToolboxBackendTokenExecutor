package org.example.Utils;

import org.example.TokenHandler.KeyManager;
import org.example.Logging.facade.LogManager;
import org.example.Logging.implementations.DefaultCliLogger;
import org.example.ValueTypes.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class TokenValidator {
    private static final DefaultCliLogger logger = LogManager.getLogger();
    public static boolean isValid(Token token) {

        try {
            if (token.isExpired()) {
                logger.debugEntry()
                        .message("Token is expired")
                        .field("Token", token.toString())
                        .log();
                return false;
            }

            boolean signatureValid = isSignatureValid(token);
            logger.debugEntry()
                    .message("Token signature verification result")
                    .field("Token", token.toString())
                    .field("SignatureValid", signatureValid)
                    .log();

            return signatureValid;

        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }

    private static boolean isSignatureValid(Token token) throws Exception {
        return verifyDigitalSignature(token.getMessage(), token.signature());
    }

    private static boolean verifyDigitalSignature(String message, String digitalSignature)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, SignatureException {

        logger.debugEntry()
                .message("Starting signature verification")
                .field("SignedMessage", message)
                .field("SignatureBase64", digitalSignature)
                .log();

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] signatureBytes = Base64.getDecoder().decode(digitalSignature);

        if (signatureBytes.length != 64) {
            LogManager.warn("Invalid signature length: " + signatureBytes.length);
            throw new IllegalArgumentException("Invalid signature length: " + signatureBytes.length);
        }

        PublicKey publicKey = new KeyManager().getPublicKey();

        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(publicKey);
        signature.update(messageBytes);

        boolean valid = signature.verify(signatureBytes);

        logger.debugEntry()
                .message("Signature verification completed")
                .field("Valid", valid)
                .log();

        return valid;
    }
}
