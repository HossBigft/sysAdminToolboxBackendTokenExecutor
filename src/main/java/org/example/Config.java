package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.example.sysAdminToolboxBackendTokenExecutor.generatePassword;

class Config {
    private static final String ENV_PATH = ".env.json";
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    static final int DB_USER_PASSWORD_LENGTH = 15;
    public static Map<String, String> values = new HashMap<>();

    static {
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
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

        boolean updated = false;
        updated |= computeIfAbsentOrBlank(values, "DATABASE_USER", Config::resolveUser);
        updated |= computeIfAbsentOrBlank(values, "DATABASE_PASSWORD",
                () -> generatePassword(DB_USER_PASSWORD_LENGTH));
        if (updated) {
            updateDotEnv();
        }
        if (!isEnvPermissionsSecure(envFile)) {
            setEnvPermissionsOwner(envFile);
        }
        org.example.DatabaseSetup.ensureDatabaseSetup();

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

    public static String resolveUser() {
        return Stream.of(getSudoUser(), getSystemUser(), getUserFromPath()).flatMap(Optional::stream)
                .filter(Config::isValidUser).findFirst().orElseThrow(
                        () -> new IllegalStateException("Could not determine valid user for running executable."));
    }

    static void updateDotEnv() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File envFile = new File(ENV_PATH);
        mapper.writeValue(envFile, values);
    }

    private static boolean isEnvPermissionsSecure(File envFile) throws IOException {
        Path envPath = envFile.toPath();
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(envPath);

        String permString = PosixFilePermissions.toString(permissions);
        boolean permsOk = DOTENV_PERMISSIONS.equals(permString);

        UserPrincipal owner = Files.getOwner(envPath);
        boolean ownerOk = DOTENV_OWNER.equals(owner.getName());

        PosixFileAttributes attrs = Files.readAttributes(envPath, PosixFileAttributes.class);
        boolean groupOk = DOTENV_GROUP.equals(attrs.group().getName());

        return permsOk && ownerOk && groupOk;

    }

    private static void setEnvPermissionsOwner(File envFile) throws IOException {
        Path filePath = envFile.toPath();
        Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString(DOTENV_PERMISSIONS));
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(DOTENV_OWNER);
        GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(DOTENV_GROUP);

        Files.setAttribute(filePath, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
        Files.setAttribute(filePath, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
    }

    private static Optional<String> getSudoUser() {
        return Optional.ofNullable(System.getenv("SUDO_USER"));
    }

    private static Optional<String> getSystemUser() {
        String systemUser = System.getProperty("user.name");
        return Optional.ofNullable(systemUser);
    }

    private static Optional<String> getUserFromPath() {
        String cwd = System.getProperty("user.dir");
        if (cwd == null) return Optional.empty();

        Path path = Paths.get(cwd).toAbsolutePath();
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            if ("home".equals(path.getName(i).toString())) {
                String username = path.getName(i + 1).toString();
                return Optional.of(username);
            }
        }
        return Optional.empty();
    }

    private static boolean isValidUser(String user) {
        return !"root".equals(user);
    }

    static String getDatabaseUser() {
        return values.get("DATABASE_USER");
    }

    static String getDatabasePassword() {
        return values.get("DATABASE_PASSWORD");
    }


}