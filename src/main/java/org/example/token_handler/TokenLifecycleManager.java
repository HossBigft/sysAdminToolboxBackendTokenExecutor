package org.example.token_handler;

import org.example.config.constants.EnvironmentConstants;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.value_types.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class TokenLifecycleManager {
    private static final Path STORAGE_DIR = Path.of("/tmp/" + EnvironmentConstants.APP_NAME + "/");
    private static final String STORAGE_FILENAME = "used_tokens.txt";
    private static final Path FULLPATH = STORAGE_DIR.resolve(STORAGE_FILENAME);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        File directory = new File(String.valueOf(STORAGE_DIR));
        if (!directory.exists()) {
            directory.mkdirs();
            getLogger().
                    info("Created token storage directory: " + STORAGE_DIR);
        }
    }

    public static boolean isTokenUsed(Token token) {
        try (Stream<String> lines = Files.lines(FULLPATH)) {

            return lines.anyMatch(line -> line.contains(token.signature()));
        } catch (IOException e) {
            getLogger().
                    errorEntry().message("Storage file could not be read")
                    .field("Path", FULLPATH.toString()).log();
            return false;
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public static void markTokenAsUsed(Token token) throws IOException {
        try {
            Files.createDirectories(STORAGE_DIR);


            boolean fileExistsBefore = Files.exists(FULLPATH);
            if (!fileExistsBefore) {
                getLogger().
                        infoEntry()
                        .message("Token file created")
                        .field("Path", FULLPATH.toString()).log();

            }

            Files.write(
                    FULLPATH,
                    ("[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + token + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );


            getLogger().
                    infoEntry()
                    .message("Token was marked as used")
                    .field("Token", token.value())
                    .log();

        } catch (IOException e) {
            getLogger().
                    errorEntry()
                    .message("Failed to mark token as used")
                    .field("Token", token.value())
                    .field("Path", FULLPATH.toString())
                    .exception(e)
                    .log();
            throw new IOException(e);
        }
    }


}
