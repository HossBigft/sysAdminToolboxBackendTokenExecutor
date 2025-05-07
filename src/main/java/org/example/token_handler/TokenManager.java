package org.example.token_handler;

import org.example.config.key_ed25519.KeyManager;
import org.example.exceptions.KeyManagerException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.value_types.Token;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class TokenManager {
    public static class TokenValidator {
        public static boolean isValid(Token token) {

            try {
                if (token.isExpired()) {
                    getLogger().
                            debugEntry()
                            .message("Token is expired")
                            .field("Token", token.toString())
                            .log();
                    return false;
                }

                boolean signatureValid = isSignatureValid(token);
                getLogger().
                        debugEntry()
                        .message("Token signature verification result")
                        .field("Result", signatureValid ? "True" : "False")
                        .field("Token", token.toString())
                        .log();

                return signatureValid;

            } catch (Exception e) {
                getLogger().
                        error("Token validation failed", e);
                return false;
            }
        }

        private static CliLogger getLogger() {
            return LogManager.getInstance().getLogger();
        }

        private static boolean isSignatureValid(Token token) throws Exception {
            return verifyDigitalSignature(token.getMessage(), token.signature());
        }

        private static boolean verifyDigitalSignature(String message,
                                                      String digitalSignature)
                throws NoSuchAlgorithmException,
                InvalidKeyException, SignatureException, KeyManagerException {

            getLogger().
                    debugEntry()
                    .message("Starting signature verification")
                    .field("SignedMessage", message)
                    .field("SignatureBase64", digitalSignature)
                    .log();

            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] signatureBytes = Base64.getDecoder().decode(digitalSignature);

            if (signatureBytes.length != 64) {
                getLogger().
                        warn("Invalid signature length: " + signatureBytes.length);
                throw new IllegalArgumentException("Invalid signature length: " + signatureBytes.length);
            }


            PublicKey publicKey = new KeyManager().loadPublicKey();

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(messageBytes);

            boolean valid = signature.verify(signatureBytes);

            getLogger().
                    debugEntry()
                    .message("Signature verification completed")
                    .field("Result", valid ? "True" : "False")
                    .log();

            return valid;
        }
    }
}
