package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsModelPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the command template used to run a local Coqui XTTS wrapper from DocuPodcast jobs.
 *
 * <p>DocuPodcast stores one text file per segment and expects one WAV per segment. Coqui/XTTS is
 * treated as the high-quality TTS path. The desktop app owns a local Python runtime under the
 * repository/app folder; it must never fall back to a global Python installation or the system
 * {@code PATH}. This template points to a local wrapper under {@code tools/xtts-wrapper/}. The
 * wrapper receives a text file, a speaker WAV and an output path.</p>
 */
public final class XttsTtsCommandTemplate {
    public static final String ENGINE_MODE_XTTS = "xtts";
    public static final String ENGINE_MODE_COQUI = "coqui";
    public static final String DEFAULT_SCRIPT = "scripts/tts/xtts-file-to-wav.ps1";
    public static final String DEFAULT_WRAPPER = "tools/xtts-wrapper/synthesize_xtts.py";
    public static final String DEFAULT_PYTHON = "tools/xtts-wrapper/.venv/Scripts/python.exe";
    public static final String DEFAULT_MODEL_DIR = XttsModelPathPolicy.XTTS_RELATIVE_MODEL_DIRECTORY;
    public static final String DEFAULT_SPEAKER_WAV = "tts/xtts/speakers/voz-por-defecto.wav";

    private XttsTtsCommandTemplate() {
    }

    /**
     * Resolves the managed command template for the advanced voice engine. Managed engine modes
     * derive paths from the current application root so copied ZIP/tanda folders do not keep using
     * stale absolute commands from a previous folder. Custom raw commands belong to external mode.
     */
    public static Optional<String> resolve(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        if (!selected(current)) {
            return Optional.empty();
        }
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        RuntimeArtifactPaths runtime = RuntimeArtifactPaths.fromRoot(root);
        return Optional.of(build(
                runtime.xttsPowerShellScript(),
                runtime.xttsPythonExecutable(),
                runtime.xttsWrapperScript(),
                modelDirectory(root, current),
                speakerWavPath(root, current)));
    }

    public static boolean selected(OperationalSettings settings) {
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        String mode = normalize(current.tts().engineMode()).toLowerCase(Locale.ROOT);
        if (ENGINE_MODE_XTTS.equals(mode) || ENGINE_MODE_COQUI.equals(mode)) {
            return true;
        }
        return looksLikeLegacyAdvancedVoiceCommand(current.tts().commandTemplate());
    }

    /**
     * Older settings can keep a localized/custom command such as "Voz IA avanzada-file-to-wav.ps1"
     * or paths like "componentes locales IA avanzada-wrapper". When that command clearly belongs
     * to the managed advanced voice engine, DocuPodcast must rebuild the current portable command
     * from the active application root instead of executing stale paths copied from another ZIP.
     */
    public static boolean looksLikeLegacyAdvancedVoiceCommand(String commandTemplate) {
        String command = normalize(commandTemplate).toLowerCase(Locale.ROOT);
        return command.contains("xtts-file-to-wav")
                || command.contains("synthesize_xtts")
                || command.contains("xtts-wrapper")
                || command.contains("voz ia avanzada-file-to-wav")
                || command.contains("componentes locales ia avanzada-wrapper")
                || command.contains("recursos locales ia avanzada");
    }

    public static Path modelDirectory(Path applicationRoot, OperationalSettings settings) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot.toAbsolutePath().normalize();
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        Path portableRoot = XttsModelPathPolicy.portableModelDirectory(root);
        if (usableXttsModelFolder(portableRoot)) {
            return portableRoot;
        }
        return XttsModelPathPolicy.modelDirectoryFromSettings(current, root);
    }

    private static boolean usableXttsModelFolder(Path folder) {
        return XttsModelPathPolicy.usableXttsModelFolder(folder);
    }

    public static Path speakerWavPath(Path applicationRoot, OperationalSettings settings) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        String voice = advancedSpeakerVoiceId(current);
        Path voicePath = Path.of(voice);
        if (voicePath.isAbsolute() || voice.contains("/") || voice.contains("\\")) {
            return voicePath.normalize();
        }
        if (!voice.toLowerCase(Locale.ROOT).endsWith(".wav")) {
            voice = voice + ".wav";
        }
        return modelDirectory(root, current).resolve("speakers").resolve(voice).normalize();
    }

    private static String advancedSpeakerVoiceId(OperationalSettings settings) {
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        String mode = normalize(current.tts().engineMode()).toLowerCase(Locale.ROOT);
        String voice = normalize(current.tts().voiceProfileId());
        if (!ENGINE_MODE_XTTS.equals(mode) && !ENGINE_MODE_COQUI.equals(mode)) {
            return "voz-por-defecto.wav";
        }
        if (voice.isBlank()
                || "VOC-NARRATOR".equalsIgnoreCase(voice)
                || "VOC-OWN-PLACEHOLDER".equalsIgnoreCase(voice)
                || "voz-local-simple".equalsIgnoreCase(voice)
                || OfficialAdvancedVoicePresetCatalog.voiceIds().stream().anyMatch(id -> id.equalsIgnoreCase(voice))) {
            return "voz-por-defecto.wav";
        }
        return voice;
    }

    public static Path pythonPath(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        return RuntimeArtifactPaths.fromRoot(root).xttsPythonExecutable();
    }

    public static boolean looksReady(OperationalSettings settings, Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        RuntimeArtifactPaths runtime = RuntimeArtifactPaths.fromRoot(root);
        return Files.isRegularFile(runtime.xttsPowerShellScript())
                && Files.isRegularFile(pythonPath(root))
                && Files.isRegularFile(runtime.xttsWrapperScript())
                && Files.isRegularFile(speakerWavPath(root, settings));
    }

    private static String build(Path script, Path python, Path wrapper, Path modelDir, Path speakerWav) {
        return "powershell -NoProfile -ExecutionPolicy Bypass -File "
                + quote(script)
                + " -Python " + quote(python)
                + " -Wrapper " + quote(wrapper)
                + " -ModelDir " + quote(modelDir)
                + " -SpeakerWav " + quote(speakerWav)
                + " -Text {textFile} -Output {outputFile} -Language {language} -Device {device}";
    }

    private static String quote(Path path) {
        String text = Objects.toString(path, "").replace("\\", "/");
        return '"' + text.replace("\"", "") + '"';
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
