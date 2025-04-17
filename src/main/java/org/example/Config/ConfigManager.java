package org.example.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigManager {
    static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String ENV_PATH = ".env.json";
    private static final String DB_USER = "sysAdminToolBox";
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    public static Map<String, String> values = new HashMap<>();

    static {
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private ConfigManager() {
    }

    private static void loadConfig() throws IOException {
        File envFile = new File(ENV_PATH);
        ObjectMapper mapper = new ObjectMapper();

        try {
            values = mapper.readValue(envFile, new TypeReference<Map<String, String>>() {
            });
        } catch (IOException e) {
            values = new HashMap<>();
        }

        boolean updated = computeIfAbsentOrBlank(values, ENV_DB_PASS_FIELD,
                () -> Utils.generatePassword(DB_USER_PASSWORD_LENGTH));
        if (updated) {
            updateDotEnv();
        }
        if (!ShellUtils.isEnvPermissionsSecure(envFile)) {
            ShellUtils.setEnvPermissionsOwner(envFile);
        }
        DatabaseProvisioner.ensureDatabaseSetup();

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
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File envFile = new File(ENV_PATH);

        mapper.writeValue(envFile, Map.of(ENV_DB_PASS_FIELD, getDatabasePassword()));
    }

    public static String getDatabasePassword() {
        return values.get(ENV_DB_PASS_FIELD);
    }

    public static String getDatabaseUser() {
        return DB_USER;
    }


}