package org.example.Utils;

import org.example.Config.KeyManager;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
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
                                                 String digitalSignature) throws
            IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] messageBytes = message.getBytes();
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