package org.example.TokenHandler;

import org.example.Constants.EnvironmentConstants;
import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;

import org.example.ValueTypes.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class TokenLifecycleManager {
    private static final Path STORAGE_DIR = Path.of("/tmp/" + EnvironmentConstants.APP_USER + "/");
    private static final String STORAGE_FILENAME = "used_tokens.txt";
    private static final Path FULLPATH = STORAGE_DIR.resolve(STORAGE_FILENAME);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final CliLogger logger = LogManager.getInstance().getLogger();

    static {
        File directory = new File(String.valueOf(STORAGE_DIR));
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created token storage directory: " + STORAGE_DIR);
        }
    }

    public static boolean isTokenUsed(Token token) {
        try (Stream<String> lines = Files.lines(FULLPATH)) {

            return lines.anyMatch(line -> line.contains(token.signature()));
        } catch (IOException e) {
            logger.errorEntry().message("Storage file could not be read")
                    .field("Path", FULLPATH.toString()).log();
            return false;
        }
    }

    public static void markTokenAsUsed(Token token) throws IOException {
        try {
            Files.createDirectories(STORAGE_DIR);


            boolean fileExistsBefore = Files.exists(FULLPATH);
            if (!fileExistsBefore) {
                logger.infoEntry()
                        .message("Token file created")
                        .field("Path", FULLPATH.toString()).log();

            }

            Files.write(
                    FULLPATH,
                    ("[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + token + "\n").getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );


            logger.infoEntry()
                    .message("Token was marked as used")
                    .field("Token", token.value())
                    .log();

        } catch (IOException e) {
            logger.errorEntry()
                    .message("Failed to mark token as used")
                    .field("Token", token.value())
                    .field("Path", FULLPATH.toString())
                    .exception(e)
                    .log();
            throw new IOException(e);
        }
    }


}
