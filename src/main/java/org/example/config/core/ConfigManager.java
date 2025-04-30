package org.example.config.core;

import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.security.FileSecurityManager;
import org.example.config.security.SudoPrivilegeManager;
import org.example.constants.EnvironmentConstants;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
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
    private static final ConfigFileHandler chandler = new ConfigFileHandler();
    private static Map<String, String> values = new HashMap<>();

    private ConfigManager() {
    }

    public static void initialize() {
        checkPrerequisites();
        try {
            loadConfig();
            ensureSetup();
            getLogger().debug("Config file " + getEnvFilePath() + " is loaded.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        } catch (CommandFailedException | URISyntaxException e) {
            getLogger().error("Failed to initialize", e);
            throw new RuntimeException(e);
        }
    }

    public static void checkPrerequisites() {
        boolean pleskExists = Files.isExecutable(Paths.get(Executables.PLESK_CLI_EXECUTABLE));
        boolean bindExists = Files.isExecutable(Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE));

        if (!pleskExists && !bindExists) {
            System.err.println("CRITICAL ERROR: Neither Plesk nor Bind are installed. Exiting immediately!");
            System.exit(1);
        }

    }

    public static void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        values = chandler.loadConfig(getEnvFile());

        boolean
                updated =
                computeIfAbsentOrBlank(values,
                        cprovider.getEnvDbPassFieldName(),
                        () -> Utils.generatePassword(cprovider.getDbUserPasswordLength()));
        if (updated) {
            updateDotEnv();
        }

    }

    private static void ensureSetup() throws IOException, URISyntaxException, CommandFailedException {
        new FileSecurityManager().ensureDotEnvPermissions();
        new DatabaseSetupCoordinator().ensureDatabaseSetup();
        new SudoPrivilegeManager().setupSudoPrivileges();
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public static String getEnvFilePath() {
        return cprovider.getEnvFile().getPath();
    }

    public static File getEnvFile() {
        return cprovider.getEnvFile();
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
        chandler.saveConfig(getEnvFile(), values);
    }

    public static String getDatabasePassword() {
        return getValue(cprovider.getEnvDbPassFieldName());
    }

    public static String getValue(String key) {
        return values.get(key);
    }

    public static File getConfigDir() {
        return cprovider.getConfigDir();
    }

    public static int getDatabasePasswordLength() {
        return cprovider.getDbUserPasswordLength();
    }

    public static String getDatabaseUser() {
        return EnvironmentConstants.APP_NAME;
    }

    public static void putValue(String key, String value) {
        values.put(key, value);
    }
}