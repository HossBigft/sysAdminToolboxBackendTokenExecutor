package org.example.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.Exceptions.CommandFailedException;
import org.example.Utils.Logging.LogManager;
import org.example.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigManager {
    public static final String ENV_PATH = ".env.json";
    static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String DB_USER = "sysAdminToolBox";
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    public static Map<String, String> values = new HashMap<>();

    static {
        try {
            LogManager.log().action("INIT", "ConfigManager loaded").info();
            loadConfig();
            LogManager.log().action("INIT", "ConfigManager loaded", true).info();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        } catch (CommandFailedException | URISyntaxException e) {
            LogManager.log().action("INIT", "Initialization error").error(e);
            throw new RuntimeException(e);
        }
    }

    private ConfigManager() {
    }

    private static void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        File envFile = new File(ENV_PATH);
        ObjectMapper mapper = new ObjectMapper();

        try {
            values = mapper.readValue(envFile, new TypeReference<>() {
            });
            LogManager.log().action("LOAD_DOTENV", ENV_PATH).debug();
        } catch (IOException e) {
            values = new HashMap<>();
            LogManager.log().info("Dotenv file not found or invalid.");
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
        LogManager.log().info("New values will be written to dotenv.");
        LogManager.log().action("CREATING_DOTENV", ENV_PATH).info();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File envFile = new File(ENV_PATH);


        mapper.writeValue(envFile, Map.of(ENV_DB_PASS_FIELD, getDatabasePassword()));

        LogManager.log().action("UPDATE_DOTENV", ENV_PATH, true).info();
    }

    public static String getDatabasePassword() {
        return values.get(ENV_DB_PASS_FIELD);
    }

    public static String getDatabaseUser() {
        return DB_USER;
    }


}