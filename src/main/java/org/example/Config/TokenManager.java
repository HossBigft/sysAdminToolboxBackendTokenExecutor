package org.example.Config;

import org.example.Constants.EnvironmentConstants;
import org.example.ValueTypes.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class TokenManager {
    private static final Path STORAGE_DIR = Path.of("/tmp/" + EnvironmentConstants.APP_USER + "/");
    private static final String STORAGE_FILENAME = "used_tokens.txt";
    private static final Path FULLPATH = STORAGE_DIR.resolve(STORAGE_FILENAME);

    static {
        File directory = new File(String.valueOf(STORAGE_DIR));
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static boolean isTokenUsed(Token token) {
        try (Stream<String> lines = Files.lines(FULLPATH)) {
            return lines.anyMatch(line -> line.contains(token.signature()));
        } catch (IOException e) {
            return false;
        }
    }

    public static void markTokenAsUsed(Token token) {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR.toUri()));
            Files.write(
                    FULLPATH,
                    (token + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error appending to used tokens file: " + e.getMessage());
        }
    }
}
