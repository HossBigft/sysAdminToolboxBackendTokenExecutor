package org.example;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.util.*;

public class Utils {
    static String generatePassword(int length) {

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

}