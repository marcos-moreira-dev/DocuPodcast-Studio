package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Explicit roots for immutable application resources and mutable managed runtimes.
 *
 * <p>The installation root owns packaged resources such as official voice samples. The runtime
 * root owns downloaded engines, models and the managed voice library. Keeping both paths explicit
 * prevents launchers started from a module directory from resolving resources through
 * {@code user.dir}.</p>
 */
public record ApplicationRuntimeRoots(Path installationRoot, Path runtimeRoot) {
    public static final String RUNTIME_ROOT_PROPERTY = "docupodcast.runtime.root";
    public static final String RUNTIME_ROOT_ENV = "DOCUPODCAST_RUNTIME_ROOT";

    public ApplicationRuntimeRoots {
        installationRoot = normalize(installationRoot, "installation root");
        runtimeRoot = normalize(runtimeRoot, "runtime root");
    }

    public static ApplicationRuntimeRoots unified(Path root) {
        Path normalized = normalize(root, "application root");
        return new ApplicationRuntimeRoots(normalized, normalized);
    }

    /**
     * Compatibility resolver for entry points that have not yet received the injected context.
     * It deliberately avoids the working directory whenever a configured application root exists.
     */
    public static ApplicationRuntimeRoots configured() {
        Path installation = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        String runtimeProperty = System.getProperty(RUNTIME_ROOT_PROPERTY);
        String runtimeEnvironment = System.getenv(RUNTIME_ROOT_ENV);
        String configuredRuntime = runtimeProperty != null && !runtimeProperty.isBlank()
                ? runtimeProperty : runtimeEnvironment;
        Path runtime = configuredRuntime == null || configuredRuntime.isBlank()
                ? installation : Path.of(configuredRuntime);
        return new ApplicationRuntimeRoots(installation, runtime);
    }

    private static Path normalize(Path path, String label) {
        return Objects.requireNonNull(path, label).toAbsolutePath().normalize();
    }
}
