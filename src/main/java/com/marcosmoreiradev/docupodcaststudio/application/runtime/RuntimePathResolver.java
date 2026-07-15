package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.net.URI;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Resolves the app root without depending on the current working directory only.
 *
 * <p>Priority: system property, environment variable, code source, fallback.
 * This is the first productization step before packaging tools/models/scripts.</p>
 */
public final class RuntimePathResolver {
    public static final String APP_ROOT_PROPERTY = "docupodcast.app.root";
    public static final String APP_ROOT_ENV = "DOCUPODCAST_APP_ROOT";

    public static RuntimePathResolver defaultResolver() {
        return new RuntimePathResolver();
    }

    public RuntimePathResolution resolve() {
        return resolve(Path.of("."));
    }

    public RuntimePathResolution resolve(Path fallbackRoot) {
        String property = System.getProperty(APP_ROOT_PROPERTY);
        if (property != null && !property.isBlank()) {
            return explicit(property, "system-property:" + APP_ROOT_PROPERTY);
        }
        String env = System.getenv(APP_ROOT_ENV);
        if (env != null && !env.isBlank()) {
            return explicit(env, "environment:" + APP_ROOT_ENV);
        }
        Path codeSource = codeSourceRoot();
        if (codeSource != null) {
            return new RuntimePathResolution(new ApplicationRuntimeLayout(codeSource), "code-source", false);
        }
        Path fallback = fallbackRoot == null ? Path.of(".") : fallbackRoot;
        return new RuntimePathResolution(new ApplicationRuntimeLayout(fallback), "fallback-working-directory", false);
    }

    private static RuntimePathResolution explicit(String rawPath, String source) {
        return new RuntimePathResolution(new ApplicationRuntimeLayout(Path.of(rawPath)), source, true);
    }

    private static Path codeSourceRoot() {
        try {
            CodeSource codeSource = RuntimePathResolver.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            URI uri = codeSource.getLocation().toURI();
            Path location = Path.of(uri).toAbsolutePath().normalize();
            String fileName = location.getFileName() == null ? "" : location.getFileName().toString();
            if (fileName.endsWith(".jar")) {
                Path parent = location.getParent();
                return parent == null ? location : parent;
            }
            if (fileName.equals("classes") && location.getParent() != null && location.getParent().getFileName() != null
                    && location.getParent().getFileName().toString().equals("target")) {
                Path projectRoot = location.getParent().getParent();
                return projectRoot == null ? location : projectRoot;
            }
            return location;
        } catch (Exception ex) {
            return null;
        }
    }
}
