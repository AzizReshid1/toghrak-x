package com.toghrakx.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadPathResolver {

    private UploadPathResolver() {
    }

    // Absolute paths are used as-is. Relative paths are resolved against the project root,
    // so the same folder is used whether the app is started from the project root or from "backend".
    public static Path resolve(String uploadDir) {
        Path path = Paths.get(uploadDir);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path workingDir = Paths.get("").toAbsolutePath();
        Path projectRoot = "backend".equals(String.valueOf(workingDir.getFileName()))
                ? workingDir.getParent()
                : workingDir;
        return projectRoot.resolve(path).normalize();
    }
}
