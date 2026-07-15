package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Single policy for resolving the local XTTS model folder used by Voz IA avanzada.
 *
 * <p>The application stores the configured models directory as a user-facing setting, but users and
 * legacy builds may point that setting to several valid shapes: the models root, the concrete
 * {@code models/tts/xtts} folder, a transplanted folder that directly contains {@code model.pth}, or
 * even the {@code model.pth} file itself. Every Java-side readiness check and command builder must
 * normalize those shapes before PowerShell/Python receives a path, so the runtime never builds a
 * nested path such as {@code model.pth/model.pth}.</p>
 */
public final class XttsModelPathPolicy {
    public static final String DEFAULT_MODELS_ROOT = "models";
    public static final String XTTS_RELATIVE_MODEL_DIRECTORY = "tts/xtts";
    public static final String MODEL_FILE_NAME = "model.pth";

    private XttsModelPathPolicy() {
    }

    /** Returns the portable XTTS folder inside the application/repository root. */
    public static Path portableModelDirectory(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        return root.resolve(DEFAULT_MODELS_ROOT).resolve(XTTS_RELATIVE_MODEL_DIRECTORY).normalize();
    }

    /**
     * Resolves the effective XTTS folder from persisted settings.
     *
     * <p>If a portable transplanted model exists in {@code models/tts/xtts}, it wins over stale
     * absolute settings from another extracted ZIP/tanda folder. Otherwise the configured setting is
     * normalized defensively.</p>
     */
    public static Path modelDirectoryFromSettings(OperationalSettings settings, Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path portable = portableModelDirectory(root);
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path configured = configuredModelDirectory(current, root);
        if (looksLikeLegacyAdvancedVoiceFolder(configured)) {
            return portable;
        }
        if (!portable.equals(configured) && usableXttsModelFolder(portable)) {
            return portable;
        }
        return configured;
    }

    /** Normalizes a user-selected folder/file into the folder that should contain XTTS artifacts. */
    public static Path normalizeUserSelectedModelPath(Path selectedPath, Path applicationRoot) {
        if (selectedPath == null) {
            return null;
        }
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path candidate = selectedPath;
        if (!candidate.isAbsolute()) {
            candidate = root.resolve(candidate);
        }
        return normalizeModelDirectory(candidate.normalize());
    }

    /** Normalizes a raw path without applying portable-folder preference. */
    public static Path normalizeModelDirectory(Path rawPath) {
        if (rawPath == null) {
            return null;
        }
        Path candidate = rawPath.toAbsolutePath().normalize();
        while (pointsToModelPth(candidate)) {
            Path parent = candidate.getParent();
            if (parent == null || parent.equals(candidate)) {
                return candidate;
            }
            candidate = parent.normalize();
        }
        if (looksLikeConcreteXttsFolder(candidate)) {
            return candidate;
        }
        Path nested = candidate.resolve(XTTS_RELATIVE_MODEL_DIRECTORY).normalize();
        if (likelyModelsRoot(candidate)) {
            return nested;
        }
        return candidate;
    }

    /** True when a command/readiness path contains the duplicated legacy shape model.pth/model.pth. */
    public static boolean containsDuplicatedModelPth(Path path) {
        if (path == null) {
            return false;
        }
        String normalized = path.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/model.pth/model.pth") || normalized.endsWith("model.pth/model.pth");
    }

    public static String explain(Path rawPath, Path normalizedPath) {
        if (rawPath == null) {
            return "No se recibió ruta de modelo.";
        }
        if (normalizedPath == null) {
            return "No se pudo normalizar la ruta de modelo.";
        }
        Path raw = rawPath.toAbsolutePath().normalize();
        Path normalized = normalizedPath.toAbsolutePath().normalize();
        if (raw.equals(normalized)) {
            return "La ruta de Voz IA avanzada ya apunta a la carpeta del modelo: " + normalized + ".";
        }
        if (pointsToModelPth(raw)) {
            return "La ruta de Voz IA avanzada apuntaba al archivo model.pth; se usará su carpeta padre: " + normalized + ".";
        }
        return "La ruta de Voz IA avanzada se normalizó a: " + normalized + ".";
    }

    private static Path configuredModelDirectory(OperationalSettings settings, Path applicationRoot) {
        String configured = settings == null || settings.storage() == null ? "" : Objects.toString(settings.storage().modelsDirectory(), "").strip();
        if (configured.isBlank()) {
            configured = DEFAULT_MODELS_ROOT;
        }
        Path raw = Path.of(configured);
        if (!raw.isAbsolute()) {
            raw = applicationRoot.resolve(raw);
        }
        return normalizeModelDirectory(raw.normalize());
    }

    private static boolean pointsToModelPth(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return MODEL_FILE_NAME.equalsIgnoreCase(path.getFileName().toString());
    }

    private static boolean looksLikeConcreteXttsFolder(Path path) {
        if (path == null) {
            return false;
        }
        if (hasCoreXttsFiles(path)) {
            return true;
        }
        String text = path.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return text.endsWith("/tts/xtts") || text.equals("tts/xtts") || text.endsWith("/xtts");
    }

    private static boolean likelyModelsRoot(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String leaf = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return "models".equals(leaf) || Files.isDirectory(path.resolve("tts"));
    }

    private static boolean looksLikeLegacyAdvancedVoiceFolder(Path path) {
        if (path == null) {
            return false;
        }
        String text = path.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return text.contains("recursos locales ia avanzada")
                || text.contains("componentes locales ia avanzada-wrapper")
                || text.contains("/model.pth/model.pth");
    }

    private static boolean hasCoreXttsFiles(Path folder) {
        return folder != null
                && Files.isRegularFile(folder.resolve("config.json"))
                && Files.isRegularFile(folder.resolve(MODEL_FILE_NAME))
                && Files.isRegularFile(folder.resolve("vocab.json"));
    }

    /** True when the folder contains the concrete files required by the local XTTS runtime. */
    public static boolean usableXttsModelFolder(Path folder) {
        return hasCoreXttsFiles(folder)
                && Files.isRegularFile(folder.resolve("speakers_xtts.pth"))
                && Files.isRegularFile(folder.resolve("dvae.pth"))
                && Files.isRegularFile(folder.resolve("mel_stats.pth"));
    }
}
