package org.example.token_handler;

import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;
import org.example.ValueTypes.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
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
                        .field("Token", token.toString())
                        .field("SignatureValid", signatureValid)
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
                throws IOException, NoSuchAlgorithmException, InvalidKeySpecException,
                InvalidKeyException, SignatureException {

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

            PublicKey publicKey = new KeyManager().getPublicKey();

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(messageBytes);

            boolean valid = signature.verify(signatureBytes);

            getLogger().
                    debugEntry()
                    .message("Signature verification completed")
                    .field("Valid", valid)
                    .log();

            return valid;
        }
    }
}
