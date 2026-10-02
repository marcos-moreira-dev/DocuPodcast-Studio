package com.marcosmoreiradev.docupodcaststudio.launcher;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Map;

final class LauncherLayoutResolver {
    static final String APP_ROOT_PROPERTY = "docupodcast.app.root";
    static final String RUNTIME_ROOT_PROPERTY = "docupodcast.runtime.root";
    static final String APP_ROOT_ENV = "DOCUPODCAST_APP_ROOT";
    static final String RUNTIME_ROOT_ENV = "DOCUPODCAST_RUNTIME_ROOT";

    private LauncherLayoutResolver() { }

    static LauncherLayout resolve() {
        return resolve(System.getProperties(), System.getenv(), Path.of("."), codeSource());
    }

    static LauncherLayout resolve(java.util.Properties properties, Map<String, String> environment,
                                  Path workingDirectory, Path codeSource) {
        String explicitInstallation = first(properties.getProperty(APP_ROOT_PROPERTY), environment.get(APP_ROOT_ENV));
        Path detected = findRepositoryRoot(workingDirectory);
        if (detected == null) detected = findRepositoryRoot(codeSource);
        Path installation = explicitInstallation == null
                ? (detected == null ? packagedRoot(codeSource, workingDirectory) : detected)
                : Path.of(explicitInstallation);
        String installationSource = explicitInstallation != null ? "explicit"
                : detected != null ? "development-layout" : "code-source";

        String explicitRuntime = first(properties.getProperty(RUNTIME_ROOT_PROPERTY), environment.get(RUNTIME_ROOT_ENV));
        boolean developmentRuntime = detected != null && Files.isDirectory(detected.resolve("models"))
                && Files.isDirectory(detected.resolve("tools"));
        Path runtime = explicitRuntime != null ? Path.of(explicitRuntime)
                : developmentRuntime ? detected : localRuntime(environment, properties);
        String runtimeSource = explicitRuntime != null ? "explicit"
                : developmentRuntime ? "development-layout" : "local-app-data";
        return new LauncherLayout(installation, runtime, installationSource, runtimeSource);
    }

    private static String first(String property, String environment) {
        if (property != null && !property.isBlank()) return property.strip();
        if (environment != null && !environment.isBlank()) return environment.strip();
        return null;
    }

    private static Path findRepositoryRoot(Path start) {
        if (start == null) return null;
        Path current = Files.isRegularFile(start) ? start.getParent() : start;
        current = current == null ? null : current.toAbsolutePath().normalize();
        for (int depth = 0; current != null && depth < 8; depth++, current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("studio-launcher"))
                    && Files.isDirectory(current.resolve("scripts/tts"))) return current;
        }
        return null;
    }

    private static Path packagedRoot(Path codeSource, Path fallback) {
        Path source = codeSource == null ? fallback : codeSource;
        if (source == null) return Path.of(".").toAbsolutePath().normalize();
        Path root = Files.isRegularFile(source) ? source.getParent() : source;
        return root == null ? source : root;
    }

    private static Path localRuntime(Map<String, String> environment, java.util.Properties properties) {
        String localAppData = environment.get("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData).resolve("DocuPodcastStudio/runtime");
        }
        String userHome = properties.getProperty("user.home", ".");
        return Path.of(userHome).resolve(".docupodcast-studio/runtime");
    }

    private static Path codeSource() {
        try {
            CodeSource source = StudioLauncher.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) return null;
            URI location = source.getLocation().toURI();
            return Path.of(location).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
        }
    }
}
