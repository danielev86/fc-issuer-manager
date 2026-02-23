package com.redcatdev86.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private AppPaths() {}

    public static Path defaultDataFile() {
        // Prefer local ./data next to where the app is started
        Path local = Paths.get("data", "fc-issuer-manager.json").toAbsolutePath();

        if (isWritableParent(local)) {
            return local;
        }

        // Fallback: user home
        Path home = Paths.get(System.getProperty("user.home"), ".fc-issuer-manager", "fc-issuer-manager.json");
        return home;
    }

    private static boolean isWritableParent(Path file) {
        try {
            Path parent = file.getParent();
            if (parent == null) return false;
            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            return Files.isWritable(parent);
        } catch (Exception e) {
            return false;
        }
    }
}