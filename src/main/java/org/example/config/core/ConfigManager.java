package org.example.config.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.security.FileSecurityManager;
import org.example.config.security.SudoPrivilegeManager;
import org.example.constants.EnvironmentConstants;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;
import org.example.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigManager {
    public static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    private static final String ENV_FILE_PATH = getConfigDir() + "/" + EnvironmentConstants.ENV_FILENAME;
    public static Map<String, String> values = new HashMap<>();

    static {
        checkPrerequisites();

        try {
            loadConfig();
            getLogger().debug("Config file " + ENV_FILE_PATH + " is loaded.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        } catch (CommandFailedException | URISyntaxException e) {
            getLogger().error("Failed to initialize", e);
            throw new RuntimeException(e);
        }
    }

    private ConfigManager() {
    }

    public static String getEnVFilePath() {
        return ENV_FILE_PATH;
    }

    public static void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        File envFile = new File(ENV_FILE_PATH);
        ObjectMapper mapper = new ObjectMapper();

        try {
            values = mapper.readValue(envFile, new TypeReference<>() {
            });
            getLogger().debug("Loaded dotenv " + ENV_FILE_PATH);
        } catch (IOException e) {
            values = new HashMap<>();
            getLogger().info("Dotenv file not found or invalid.");
        }

        boolean
                updated =
                computeIfAbsentOrBlank(values,
                        ENV_DB_PASS_FIELD,
                        () -> Utils.generatePassword(DB_USER_PASSWORD_LENGTH));
        if (updated) {
            updateDotEnv();
        }

        new FileSecurityManager().ensureDotEnvPermissions();
        new DatabaseSetupCoordinator().ensureDatabaseSetup();
        new SudoPrivilegeManager().setupSudoPrivileges();

    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private static boolean computeIfAbsentOrBlank(Map<String, String> map, String key, Supplier<String> supplier) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            map.put(key, supplier.get());
            return true;
        }
        return false;
    }

    public static void updateDotEnv() throws IOException {


        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        initConfigDir();

        File envFile = new File(ENV_FILE_PATH);
        getLogger().info("Creating dotenv " + envFile);
        mapper.writeValue(envFile, Map.of(ENV_DB_PASS_FIELD, getDatabasePassword()));

        getLogger().info("New data written to " + ENV_FILE_PATH);
    }

    private static void initConfigDir() throws IOException {

        final File configDir = getConfigDir();
        if (!configDir.isDirectory()) {
            getLogger().warnEntry().message("Config directory is not present").field("File", configDir.toPath()).log();
            Files.createDirectories(configDir.toPath());
            getLogger().infoEntry().message("Created config directory").field("File", configDir.toPath()).log();
        }
    }

    public static String getDatabasePassword() {
        return values.get(ENV_DB_PASS_FIELD);
    }

    private static File getConfigDir() {
        final String appUser = ShellUtils.resolveToolBoxUser();
        return Paths.get("/home/" + appUser + "/." + EnvironmentConstants.APP_NAME).toFile();
    }

    public static String getDatabaseUser() {
        return EnvironmentConstants.APP_NAME;
    }

    public static void checkPrerequisites() {
        boolean pleskExists = Files.isExecutable(Paths.get(Executables.PLESK_CLI_EXECUTABLE));
        boolean bindExists = Files.isExecutable(Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE));

        if (!pleskExists && !bindExists) {
            System.err.println("CRITICAL ERROR: Neither Plesk nor Bind are installed. Exiting immediately!");
            System.exit(1);
        }

    }

}