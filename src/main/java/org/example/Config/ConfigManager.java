package org.example.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mysql.cj.log.Log;
import org.example.Constants.EnvironmentConstants;
import org.example.Exceptions.CommandFailedException;
import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;
import org.example.Logging.implementations.DefaultCliLogger;
import org.example.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigManager {
    static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    public static Map<String, String> values = new HashMap<>();
    
    private static final CliLogger logger = LogManager.getInstance().getLogger();

    static {
        try {
            loadConfig();
            logger.debug("Config file " + EnvironmentConstants.ENV_PATH + " is loaded.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        } catch (CommandFailedException | URISyntaxException e) {
            logger.error("Failed to initialize", e);
            throw new RuntimeException(e);
        }
    }

    private ConfigManager() {
    }

    private static void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        File envFile = new File(EnvironmentConstants.ENV_PATH);
        ObjectMapper mapper = new ObjectMapper();

        try {
            values = mapper.readValue(envFile, new TypeReference<>() {
            });
            logger.debug("Loaded dotenv " + EnvironmentConstants.ENV_PATH);
        } catch (IOException e) {
            values = new HashMap<>();
            logger.info("Dotenv file not found or invalid.");
        }

        boolean updated = computeIfAbsentOrBlank(values, ENV_DB_PASS_FIELD,
                () -> Utils.generatePassword(DB_USER_PASSWORD_LENGTH));
        if (updated) {
            updateDotEnv();
        }

        new PermissionManager().ensureDotEnvPermissions();
        DatabaseProvisioner.ensureDatabaseSetup();
        new SudoersManager().ensureSudoersRuleIsPresent();

    }

    private static boolean computeIfAbsentOrBlank(Map<String, String> map,
                                                  String key,
                                                  Supplier<String> supplier) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            map.put(key, supplier.get());
            return true;
        }
        return false;
    }

    static void updateDotEnv() throws IOException {
        logger.info("Creating dotenv" + EnvironmentConstants.ENV_PATH);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File envFile = new File(EnvironmentConstants.ENV_PATH);


        mapper.writeValue(envFile, Map.of(ENV_DB_PASS_FIELD, getDatabasePassword()));

        logger.info("New data written to" + EnvironmentConstants.ENV_PATH);
    }

    public static String getDatabasePassword() {
        return values.get(ENV_DB_PASS_FIELD);
    }

    public static String getDatabaseUser() {
        return EnvironmentConstants.APP_USER;
    }


}