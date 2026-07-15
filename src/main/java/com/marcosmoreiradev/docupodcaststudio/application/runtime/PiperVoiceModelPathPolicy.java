package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves the managed Piper voice model independently from advanced XTTS voice selections.
 *
 * <p>The global voice setting can contain an advanced voice id, a user sample path or a preset id.
 * Piper cannot use those values as ONNX models, so the managed lightweight engine falls back to its
 * embedded Spanish model unless the setting explicitly points to a Piper ONNX voice.</p>
 */
public final class PiperVoiceModelPathPolicy {
    public static final String DEFAULT_PIPER_VOICE = "es_ES-default-medium.onnx";
    public static final String PIPER_VOICES_RELATIVE = "tts/piper/voices";

    private PiperVoiceModelPathPolicy() {
    }

    public static Path voiceDirectory(Path applicationRoot, OperationalSettings settings) {
        Path root = root(applicationRoot);
        return modelsRoot(root, current(settings)).resolve(PIPER_VOICES_RELATIVE).normalize();
    }

    public static Path voiceModelPath(Path applicationRoot, OperationalSettings settings) {
        Path root = root(applicationRoot);
        OperationalSettings current = current(settings);
        String voice = normalize(current.tts().voiceProfileId());
        if (usesManagedDefaultVoice(voice)) {
            return voiceDirectory(root, current).resolve(DEFAULT_PIPER_VOICE).normalize();
        }
        Path voicePath = Path.of(voice);
        if (isPathLike(voice)) {
            return voicePath.isAbsolute() ? voicePath.normalize() : root.resolve(voicePath).normalize();
        }
        if (!voice.toLowerCase(Locale.ROOT).endsWith(".onnx")) {
            voice = voice + ".onnx";
        }
        return voiceDirectory(root, current).resolve(voice).normalize();
    }

    public static Path voiceMetadataPath(Path applicationRoot, OperationalSettings settings) {
        return Path.of(voiceModelPath(applicationRoot, settings).toString() + ".json").normalize();
    }

    private static OperationalSettings current(OperationalSettings settings) {
        return settings == null ? OperationalSettings.defaults() : settings;
    }

    private static Path root(Path applicationRoot) {
        return applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
    }

    private static Path modelsRoot(Path applicationRoot, OperationalSettings settings) {
        String configured = normalize(settings.storage().modelsDirectory());
        if (configured.isBlank() || isLegacySimpleVoiceStorage(configured)) {
            return RuntimeArtifactPaths.fromRoot(applicationRoot).modelsRoot();
        }
        Path configuredPath = Path.of(configured);
        return configuredPath.isAbsolute() ? configuredPath.normalize() : applicationRoot.resolve(configuredPath).normalize();
    }

    private static boolean usesManagedDefaultVoice(String voice) {
        String normalized = normalize(voice);
        if (normalized.isBlank()
                || "VOC-NARRATOR".equalsIgnoreCase(normalized)
                || "voz-local-simple".equalsIgnoreCase(normalized)) {
            return true;
        }
        String lower = normalized.replace('\\', '/').toLowerCase(Locale.ROOT);
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("VOC-")) {
            return true;
        }
        if (lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".flac")) {
            return true;
        }
        if (lower.contains("samples/voices")
                || lower.contains("voice-library")
                || lower.contains("advanced-presets")) {
            return true;
        }
        return isPathLike(normalized) && !lower.endsWith(".onnx");
    }

    private static boolean isLegacySimpleVoiceStorage(String configured) {
        String lower = normalize(configured).replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.contains("recursos locales local simple")
                || lower.contains("componentes locales local simple")
                || lower.endsWith("/recursos locales")
                || lower.endsWith("/componentes locales");
    }

    private static boolean isPathLike(String value) {
        return value.contains("/") || value.contains("\\") || Path.of(value).isAbsolute();
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").strip();
    }
}
