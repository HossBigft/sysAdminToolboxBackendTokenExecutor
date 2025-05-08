package org.example.config.security;

import com.google.errorprone.annotations.CheckReturnValue;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.Optional;
import java.util.Set;

public class FileAccessPolicy {
    private final Path path;
    private Optional<Set<PosixFilePermission>> permissions = Optional.empty();
    private Optional<String> owner = Optional.empty();
    private Optional<String> group = Optional.empty();

    @CheckReturnValue
    public FileAccessPolicy(File file) {
        this.path = file.toPath();
    }

    public FileAccessPolicy permissions(String permissions) {
        if (permissions == null || !permissions.matches("[-rwx]{9}")) {
            throw new IllegalArgumentException("Permissions must be 9-character string like rwxr-xr--");
        }
        this.permissions = Optional.of(PosixFilePermissions.fromString(permissions));
        return this;
    }

    public FileAccessPolicy owner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be null or blank.");
        }
        this.owner = Optional.of(owner);
        return this;
    }

    public FileAccessPolicy group(String group) {

        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Group must not be null or blank.");
        }
        this.group = Optional.of(group);
        return this;
    }

    public void enforce() {
        if (!isSecured()) {
            owner.ifPresent(o -> quietly(() -> setOwner(o)));
            group.ifPresent(g -> quietly(() -> setGroup(g)));
            permissions.ifPresent(p -> quietly(() -> setPermissions(p)));
        }
    }

    public boolean isSecured() {

        boolean permsOk = permissions.map(this::hasCorrectPermissions).orElse(true);
        boolean ownerOk = owner.map(this::hasCorrectOwner).orElse(true);
        boolean groupOk = group.map(this::hasCorrectGroup).orElse(true);


        if (!permsOk) {
            permissions.map(PosixFilePermissions::toString)
                    .ifPresent(permsStr -> getLogger().infoEntry()
                            .message("File permissions is incorrect")
                            .field("expected", permsStr)
                            .field("path", path.toString())
                            .log());
        }
        if (!ownerOk) {
            getLogger().warnEntry()
                    .message("File owner is incorrect")
                    .field("expected", owner.orElse("N/A"))
                    .field("path", path.toString())
                    .log();
        }
        if (!groupOk) {
            getLogger().warnEntry()
                    .message("File group is incorrect")
                    .field("expected", group.orElse("N/A"))
                    .field("path", path.toString())
                    .log();
        }

        return permsOk && ownerOk && groupOk;
    }

    private void quietly(IORunnable r) {
        try {
            r.run();
        } catch (IOException e) {
            getLogger().errorEntry()
                    .message("Failed to apply file policy")
                    .field("path", path.toString())
                    .exception(e)
                    .log();
            throw new RuntimeException("File policy enforcement failed", e);
        }
    }

    private void setOwner(String owner) throws IOException {
        UserPrincipal
                userPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(owner);
        Files.setAttribute(path, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
        getLogger().infoEntry().message("Set owner [" + owner + "]").field("File", path).log();
    }

    private void setGroup(String group) throws IOException {
        GroupPrincipal
                groupPrincipal =
                FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
        Files.setAttribute(path, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
        getLogger().infoEntry().message("Set group [" + group + "]").field("File", path).log();
    }

    private void setPermissions(Set<PosixFilePermission> permissions) throws IOException {
        Files.setPosixFilePermissions(path, permissions);
        getLogger().infoEntry()
                .message("Set file permissions")
                .field("permissions", PosixFilePermissions.toString(permissions))
                .field("path", path.toString())
                .log();
    }

    private boolean hasCorrectPermissions(Set<PosixFilePermission> expectedPerms) {
        try {
            Set<PosixFilePermission> currentPerms = Files.getPosixFilePermissions(path);
            return expectedPerms.equals(currentPerms);
        } catch (IOException e) {
            getLogger().errorEntry()
                    .message("Failed to read file permissions")
                    .field("path", path.toString())
                    .exception(e)
                    .log();
            return false;
        }
    }

    private boolean hasCorrectOwner(String expectedOwner) {
        try {
            UserPrincipal currentOwner = Files.getOwner(path);
            return expectedOwner.equals(currentOwner.getName());
        } catch (IOException e) {
            getLogger().errorEntry()
                    .message("Failed to read file owner")
                    .field("path", path.toString())
                    .exception(e)
                    .log();
            return false;
        }
    }

    private boolean hasCorrectGroup(String expectedGroup) {
        try {
            PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class);
            return expectedGroup.equals(attrs.group().getName());
        } catch (IOException e) {

            getLogger().errorEntry()
                    .message("Failed to read file group")
                    .field("path", path.toString())
                    .exception(e)
                    .log();
            return false;
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    @FunctionalInterface
    private interface IORunnable {
        void run() throws IOException;
    }

}
