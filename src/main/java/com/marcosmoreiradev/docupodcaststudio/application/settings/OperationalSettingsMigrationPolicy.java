package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Repairs persistent settings created by older DocuPodcast builds before the managed runtime layout
 * became the canonical path for XTTS, Piper and FFmpeg.
 *
 * <p>This policy is intentionally in application: loading settings is not just deserialization, it is a
 * product boundary. A user can carry an old {@code operational-settings.properties} between ZIP/tanda
 * folders. If that file still contains a localized command such as {@code Voz IA avanzada-file-to-wav.ps1}
 * or a model folder ending in {@code model.pth}, the current app must not execute the stale path. It
 * must repair the intent to the managed engine mode and let the current runtime resolver rebuild paths
 * from the active application root.</p>
 */
public final class OperationalSettingsMigrationPolicy {
    private static final String MODEL_FILE = "model.pth";
    private static final String PORTABLE_XTTS_MODEL_DIRECTORY = "models/tts/xtts";

    private OperationalSettingsMigrationPolicy() {
    }

    public static OperationalSettings repair(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        OperationalSettings.TtsEngineSettings tts = repairTts(current.tts());
        OperationalSettings.StorageSettings storage = repairStorage(current.storage());
        if (tts == current.tts() && storage == current.storage()) {
            return current;
        }
        return new OperationalSettings(
                current.readingDocument(),
                current.playbackBuffer(),
                tts,
                current.video(),
                current.imageGeneration(),
                current.frameGeneration(),
                current.compute(),
                current.ocr(),
                storage,
                current.diagnostics());
    }

    public static boolean hasLegacyAdvancedVoiceCommand(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return looksLikeLegacyAdvancedVoiceCommand(current.tts().commandTemplate());
    }

    public static boolean looksLikeLegacyAdvancedVoiceCommand(String commandTemplate) {
        String command = normalize(commandTemplate).toLowerCase(Locale.ROOT)
                .replace('\\', '/')
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
        return command.contains("xtts-file-to-wav")
                || command.contains("synthesize_xtts")
                || command.contains("xtts-wrapper")
                || command.contains("voz ia avanzada-file-to-wav")
                || command.contains("componentes locales ia avanzada-wrapper")
                || command.contains("recursos locales ia avanzada")
                || command.contains("model.pth/model.pth")
                || command.contains("coqui") && command.contains("model.pth");
    }

    public static boolean modelsDirectoryPointsToModelPth(OperationalSettings settings) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        return pointsToModelPth(current.storage().modelsDirectory());
    }

    private static OperationalSettings.TtsEngineSettings repairTts(OperationalSettings.TtsEngineSettings tts) {
        OperationalSettings.TtsEngineSettings current = tts == null ? OperationalSettings.TtsEngineSettings.defaults() : tts;
        if (!looksLikeLegacyAdvancedVoiceCommand(current.commandTemplate())) {
            return current;
        }
        return new OperationalSettings.TtsEngineSettings(
                "xtts",
                "",
                "Voz IA avanzada — lectura de calidad alta",
                current.language(),
                current.voiceProfileId(),
                Math.max(current.timeoutSeconds(), 900),
                current.maxRetries(),
                current.xttsDownloadBaseUrl(),
                current.piperRuntimeZipUrl(),
                current.piperDefaultVoiceUrl(),
                current.piperDefaultVoiceMetadataUrl());
    }

    private static OperationalSettings.StorageSettings repairStorage(OperationalSettings.StorageSettings storage) {
        OperationalSettings.StorageSettings current = storage == null ? OperationalSettings.StorageSettings.defaults() : storage;
        String models = normalize(current.modelsDirectory());
        if (looksLikeLegacyAdvancedVoiceModelDirectory(models)) {
            return new OperationalSettings.StorageSettings(PORTABLE_XTTS_MODEL_DIRECTORY, current.exportsDirectory());
        }
        if (!pointsToModelPth(models)) {
            return current;
        }
        return new OperationalSettings.StorageSettings(parentOfModelPth(models), current.exportsDirectory());
    }

    private static boolean looksLikeLegacyAdvancedVoiceModelDirectory(String value) {
        String text = normalize(value).replace('\\', '/').toLowerCase(Locale.ROOT);
        return text.contains("recursos locales ia avanzada")
                || text.contains("componentes locales ia avanzada-wrapper")
                || text.contains("/model.pth/model.pth");
    }

    private static boolean pointsToModelPth(String value) {
        String text = normalize(value).replace('\\', '/');
        return text.toLowerCase(Locale.ROOT).endsWith("/" + MODEL_FILE) || text.equalsIgnoreCase(MODEL_FILE);
    }

    private static String parentOfModelPth(String value) {
        String text = normalize(value);
        if (text.isBlank()) {
            return "models";
        }
        try {
            Path path = Path.of(text);
            Path parent = path.getParent();
            if (parent != null) {
                return parent.toString();
            }
        } catch (RuntimeException ignored) {
            // Fall through to string handling; invalid paths are still user input we can repair best-effort.
        }
        String unix = text.replace('\\', '/');
        int slash = unix.lastIndexOf('/');
        return slash > 0 ? text.substring(0, slash) : "models";
    }

    private static String normalize(String value) {
        return Objects.toString(value, "").strip();
    }
}
