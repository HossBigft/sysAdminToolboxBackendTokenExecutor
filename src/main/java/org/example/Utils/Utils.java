package org.example.Utils;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Utils {
    public static String generatePassword(int length) {

        PasswordGenerator generator = new PasswordGenerator();

        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase, 1);
        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase, 1);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, 1);

        CharacterData safeSpecials = new CharacterData() {
            public String getErrorCode() {
                return "SHELL_QUOTE_CHARS_PROHIBITED";
            }

            public String getCharacters() {
                return "!#$%&()*+,-./:;<=>?@[\\]^_{|}~";
            }
        };
        CharacterRule specialRule = new CharacterRule(safeSpecials, 1);

        List<CharacterRule> rules = Arrays.asList(lowerCaseRule, upperCaseRule, digitRule, specialRule);

        return generator.generatePassword(length, rules);
    }

    public static boolean verifyDigitalSignature(String message,
                                                 String digitalSignature,
                                                 Path pubKey) throws
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] messageBytes = message.getBytes();
        byte[] signatureBytes = Base64.getDecoder().decode(digitalSignature);
        if (signatureBytes.length != 64) {
            throw new IllegalArgumentException("Invalid signature length: " + signatureBytes.length);
        }
        String base64Key = Files.readString(pubKey).trim();
        byte[] derBytes = Base64.getDecoder().decode(base64Key);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        PublicKey publicKey = kf.generatePublic(keySpec);

        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(publicKey);
        signature.update(messageBytes);

        return signature.verify(signatureBytes);

    }

}