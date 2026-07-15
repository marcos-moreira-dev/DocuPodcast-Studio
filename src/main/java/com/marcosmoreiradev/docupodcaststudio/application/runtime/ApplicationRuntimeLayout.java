package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Central description of the folder layout used by DocuPodcast Studio at runtime.
 *
 * <p>The layout is intentionally small: it does not install tools or models, but
 * it gives every adapter a single source of truth for repo, portable and future
 * installer executions.</p>
 */
public record ApplicationRuntimeLayout(Path applicationRoot) {
    public static final String TOOLS = "tools";
    public static final String MODELS = "models";
    public static final String SCRIPTS = "scripts";
    public static final String EXAMPLES = "examples";

    public ApplicationRuntimeLayout {
        Objects.requireNonNull(applicationRoot, "applicationRoot");
        applicationRoot = applicationRoot.toAbsolutePath().normalize();
    }

    public Path toolsRoot() {
        return applicationRoot.resolve(TOOLS).normalize();
    }

    public Path modelsRoot() {
        return applicationRoot.resolve(MODELS).normalize();
    }

    public Path scriptsRoot() {
        return applicationRoot.resolve(SCRIPTS).normalize();
    }

    public Path examplesRoot() {
        return applicationRoot.resolve(EXAMPLES).normalize();
    }

    public Path runtimeRoot() {
        return applicationRoot.resolve("runtime").normalize();
    }

    public Path xttsSmokeRoot() {
        return runtimeRoot().resolve("tts/xtts-smoke").normalize();
    }

    public Path voiceTempRecordingsRoot() {
        return voiceLibraryRoot().resolve("tmp-recordings").normalize();
    }

    /** Application-level voice library root. User reference samples live here, not inside each project folder. */
    public Path voiceLibraryRoot() {
        return applicationRoot.resolve("voice-library").normalize();
    }

    /** Application-level voice sample storage used by Voz IA avanzada reference samples. */
    public Path voiceSamplesRoot() {
        return voiceLibraryRoot().resolve("samples").normalize();
    }

    public Path ffmpegExecutable() {
        return applicationRoot.resolve("tools/ffmpeg/bin/ffmpeg.exe").normalize();
    }

    public Path ffprobeExecutable() {
        return applicationRoot.resolve("tools/ffmpeg/bin/ffprobe.exe").normalize();
    }

    public Path piperExecutable() {
        return applicationRoot.resolve("tools/piper/piper.exe").normalize();
    }

    public Path xttsPython() {
        return applicationRoot.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe").normalize();
    }

    public Path xttsWrapperScript() {
        return applicationRoot.resolve("tools/xtts-wrapper/synthesize_xtts.py").normalize();
    }

    public Path resolveBundled(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return applicationRoot;
        }
        return applicationRoot.resolve(relativePath.replace('\\', '/')).normalize();
    }

    public Path resolveConfiguredPath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        Path path = Path.of(configuredPath.trim());
        return path.isAbsolute() ? path.normalize() : applicationRoot.resolve(path).normalize();
    }

    public Path resolveConfiguredPathOrBundled(String configuredPath, String bundledRelativePath) {
        Path configured = resolveConfiguredPath(configuredPath);
        return configured == null ? resolveBundled(bundledRelativePath) : configured;
    }
}
