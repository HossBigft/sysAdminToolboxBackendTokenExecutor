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
    private static final ConfigProvider cprovider = new ConfigProvider();
    private static Map<String, String> values = new HashMap<>();

    static {
        checkPrerequisites();

        try {
            loadConfig();
            getLogger().debug("Config file " + getEnvFilePath() + " is loaded.");
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
        return getEnvFilePath();
    }

    public static String getEnvFilePath() {
        return cprovider.getEnvFile().getPath();
    }

    public static void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        File envFile = new File(getEnvFilePath());
        ObjectMapper mapper = new ObjectMapper();

        try {
            values = mapper.readValue(envFile, new TypeReference<>() {
            });
            getLogger().debug("Loaded dotenv " + getEnvFilePath());
        } catch (IOException e) {
            values = new HashMap<>();
            getLogger().info("Dotenv file not found or invalid.");
        }

        boolean
                updated =
                computeIfAbsentOrBlank(values,
                        cprovider.getEnvDbPassFieldName(),
                        () -> Utils.generatePassword(cprovider.getDbUserPasswordLength()));
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

        File envFile = new File(getEnvFilePath());
        getLogger().info("Creating dotenv " + envFile);
        mapper.writeValue(envFile, Map.of(cprovider.getEnvDbPassFieldName(), getDatabasePassword()));

        getLogger().info("New data written to " + getEnvFilePath());
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
        return getValue(cprovider.getEnvDbPassFieldName());
    }

    public static File getConfigDir() {
        final String appUser = ShellUtils.resolveAppUser();
        return Paths.get("/home/" + appUser + "/." + EnvironmentConstants.APP_NAME).toFile();
    }

    public static int getDatabasePasswordLength() {
        return cprovider.getDbUserPasswordLength();
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

    public static String getValue(String key) {
        return values.get(key);
    }

    public static void putValue(String key, String value) {
        values.put(key, value);
    }
}