package org.example.Config;

import org.example.Constants.EnvironmentConstants;
import org.example.Utils.Logging.LogManager;
import org.example.ValueTypes.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            LogManager.log().info("Created token storage directory: " + STORAGE_DIR);
        }
    }

    public static boolean isTokenUsed(Token token) {
        try (Stream<String> lines = Files.lines(FULLPATH)) {
            boolean tokenUsed = lines.anyMatch(line -> line.contains(token.signature()));

            if (tokenUsed) {
                LogManager.log().warn()
                        .message("Token has already been used")
                        .field("Token", token.value())
                        .log();
            } else {
                LogManager.log().info()
                        .message("Token is not used yet")
                        .field("Token", token.value())
                        .log();
            }

            return tokenUsed;
        } catch (IOException e) {
            new LogManager.LogEntryBuilder(LogManager.LogLevel.ERROR)
                    .message("Storage file could not be read")
                    .field("Path", FULLPATH.toString())
                    .log();
            return false;
        }
    }

    public static void markTokenAsUsed(Token token) {
        try {
            Files.createDirectories(STORAGE_DIR);


            boolean fileExistsBefore = Files.exists(FULLPATH);
            if (!fileExistsBefore) {
                LogManager.log().info()
                        .message("Token file created")
                        .field("Path", FULLPATH.toString())
                        .log();
            }

            Files.write(
                    FULLPATH,
                    (token + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );


            LogManager.log().info()
                    .message("Token was marked as used")
                    .field("Token", token.value())
                    .log();

        } catch (IOException e) {
            new LogManager.LogEntryBuilder(LogManager.LogLevel.ERROR)
                    .message("Failed to mark token as used")
                    .field("Token", token.value())
                    .field("Path", FULLPATH.toString())
                    .exception(e)
                    .log();
        }
    }


}
