package org.example.config.security;

import org.example.config.core.ConfigManager;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Set;

public class FileSecurityManager {
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    private static final FileAccessPolicy
            dotenvFilePolicy =
            new FileAccessPolicy(DOTENV_PERMISSIONS, DOTENV_OWNER, DOTENV_GROUP);
    private static final File dotEnvFile = new File(ConfigManager.getEnVFilePath());

    public void ensureDotEnvPermissions() throws IOException {
        if (!isFilePermissionsSecure(dotEnvFile, dotenvFilePolicy)) {
            secureDotEnvPermissionsOwnerGroup();
        }
    }

    public boolean isFilePermissionsSecure(File file,
                                           FileAccessPolicy policy) throws IOException {
        Path path = file.toPath();
        boolean permsOk = hasCorrectPermissions(path, policy.permissions());
        boolean ownerOk = hasOwner(path, policy.owner());
        boolean groupOk = hasOwnerGroup(path, policy.group());


        String filename = path.getFileName().toString();

        if (!permsOk) {
            getLogger().
                    warn("[" +
                            filename +
                            "] File permissions are incorrect: expected " +
                            policy.permissions() +
                            " for path " +
                            path);
        }
        if (!ownerOk) {
            getLogger().
                    warn("[" +
                            filename +
                            "] File owner is incorrect: expected " +
                            policy.owner() +
                            " for path " +
                            path);
        }
        if (!groupOk) {
            getLogger().
                    warn("[" +
                            filename +
                            "] File group is incorrect: expected " +
                            policy.group() +
                            " for path " +
                            path);
        }

        return permsOk && ownerOk && groupOk;
    }

    private void secureDotEnvPermissionsOwnerGroup() throws IOException {
        enforceFileAccessPolicy(FileSecurityManager.dotEnvFile, dotenvFilePolicy);

    }

    public static boolean hasCorrectPermissions(Path path,
                                                String expectedPerms) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
        String currentPerms = PosixFilePermissions.toString(permissions);
        return expectedPerms.equals(currentPerms);
    }

    public static boolean hasOwner(Path path,
                                   String expectedOwner) throws IOException {
        UserPrincipal owner = Files.getOwner(path);
        return expectedOwner.equals(owner.getName());
    }

    public static boolean hasOwnerGroup(Path path,
                                        String expectedGroup) throws IOException {
        PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class);
        return expectedGroup.equals(attrs.group().getName());
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public void enforceFileAccessPolicy(File file,
                                        FileAccessPolicy policy) throws IOException {
        setOwner(file, policy.owner());
        setGroup(file, policy.group());
        setPermissions(file, policy.permissions());
    }

    public static void setOwner(File file,
                                String owner) throws IOException {
        Path path = file.toPath();
        UserPrincipal
                userPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(owner);
        Files.setAttribute(path, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
        getLogger().
                info("SET_OWNER " + "[" + owner + "] to " + path);
    }

    public static void setGroup(File file,
                                String group) throws IOException {
        Path path = file.toPath();
        GroupPrincipal
                groupPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
        Files.setAttribute(path, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
        getLogger().
                info("SET_GROUP " + "[" + group + "] to " + path);
    }

    public static void setPermissions(File file,
                                      String permissions) throws IOException {
        Path path = file.toPath();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);
        Files.setPosixFilePermissions(path, perms);
        getLogger().
                info("Set permissions" + "[" + permissions + "] to" + path);
    }
}
