package org.example.Config;

import org.example.Constants.EnvironmentConstants;
import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;


import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Set;

public class PermissionManager {
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    private static final FileAccessPolicy
            dotenvFilePolicy =
            new FileAccessPolicy(DOTENV_PERMISSIONS, DOTENV_OWNER, DOTENV_GROUP);
    private static final File dotEnvFile = new File(EnvironmentConstants.ENV_PATH);

    private static final CliLogger logger = LogManager.getInstance().getLogger();

    public void ensureDotEnvPermissions() throws IOException {
        if (isFilePermissionsSecureNot(dotEnvFile, dotenvFilePolicy)) {
            secureDotEnvPermissionsOwnerGroup(dotEnvFile);
        }
    }


    private boolean isFilePermissionsSecureNot(File file, FileAccessPolicy policy) throws IOException {
        Path path = file.toPath();
        boolean permsOk = hasPermissions(path, policy.permissions());
        boolean ownerOk = hasOwner(path, policy.owner());
        boolean groupOk = hasOwnerGroup(path, policy.group());


        String filename = path.getFileName().toString();

        if (!permsOk) {
            logger.warn("[" +
                    filename +
                    "] File permissions are incorrect: expected " +
                    policy.permissions() +
                    " for path " +
                    path);
        }
        if (!ownerOk) {
            logger.warn("[" + filename + "] File owner is incorrect: expected " + policy.owner() + " for path " + path);
        }
        if (!groupOk) {
            logger.warn("[" + filename + "] File group is incorrect: expected " + policy.group() + " for path " + path);
        }

        return !permsOk || !ownerOk || !groupOk;
    }


    private void secureDotEnvPermissionsOwnerGroup(File envFile) throws IOException {
        Path path = envFile.toPath();

        setPermissions(path, DOTENV_PERMISSIONS);
        setOwner(path, DOTENV_OWNER);
        setGroup(path, DOTENV_GROUP);
        logger.debug("Permissions " + DOTENV_PERMISSIONS + " applied to " + dotEnvFile.getName());
    }

    public static boolean hasPermissions(Path path, String expectedPerms) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
        String currentPerms = PosixFilePermissions.toString(permissions);
        return expectedPerms.equals(currentPerms);
    }

    public static boolean hasOwner(Path path, String expectedOwner) throws IOException {
        UserPrincipal owner = Files.getOwner(path);
        return expectedOwner.equals(owner.getName());
    }

    public static boolean hasOwnerGroup(Path path, String expectedGroup) throws IOException {
        PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class);
        return expectedGroup.equals(attrs.group().getName());
    }

    public static void setPermissions(Path path, String permissions) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);
        Files.setPosixFilePermissions(path, perms);
        logger.info("Set permissions" + "[" + permissions + "] to" + path.toString());
    }

    public static void setOwner(Path path, String owner) throws IOException {
        UserPrincipal
                userPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(owner);
        Files.setAttribute(path, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
        logger.info("SET_OWNER " + "[" + owner + "] to " + path);
    }

    public static void setGroup(Path path, String group) throws IOException {

        GroupPrincipal
                groupPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
        Files.setAttribute(path, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
        logger.info("SET_GROUP " + "[" + group + "] to " + path);
    }
}
