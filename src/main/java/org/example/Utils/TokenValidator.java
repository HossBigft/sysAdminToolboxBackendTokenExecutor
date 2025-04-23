package org.example.Utils;

import org.example.Config.KeyManager;
import org.example.Utils.Logging.LogManager;
import org.example.ValueTypes.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class TokenValidator {
    public static boolean isValid(Token token) {
        try {
            return !token.isExpired() || isSignatureValid(token);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSignatureValid(Token token) throws Exception {
        return verifyDigitalSignature(token.getMessage(), token.signature());
    }

    private static boolean verifyDigitalSignature(String message,
                                                  String digitalSignature) throws
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        LogManager.debug("Signed message " + message);
        LogManager.debug("Digital signature " + digitalSignature);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] signatureBytes = Base64.getDecoder().decode(digitalSignature);
        if (signatureBytes.length != 64) {
            throw new IllegalArgumentException("Invalid signature length: " + signatureBytes.length);
        }
        PublicKey publicKey = new KeyManager().getPublicKey();

        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(publicKey);
        signature.update(messageBytes);

        return signature.verify(signatureBytes);
    }
}
