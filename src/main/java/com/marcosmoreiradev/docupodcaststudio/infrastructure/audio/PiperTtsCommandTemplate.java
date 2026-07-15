package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.PiperVoiceModelPathPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the Windows command template used to run Piper from a text-file based DocuPodcast job.
 *
 * <p>Piper normally receives text through standard input and writes a WAV with {@code --output_file}.
 * DocuPodcast jobs, however, persist one text file per segment so jobs can be resumed and audited.
 * This factory bridges both worlds by generating a command that calls the small repository script
 * {@code scripts/tts/piper-file-to-wav.ps1}: the script reads {@code {textFile}}, pipes the text into
 * Piper and asks Piper to write {@code {outputFile}}.</p>
 *
 * <p>The command also forwards the selected compute policy/device to the wrapper. Piper builds may
 * ignore or reject these hints, but DocuPodcast still propagates the user's CPU/GPU choice so a
 * capable local runtime can honor the selected device instead of silently choosing another adapter.</p>
 */
public final class PiperTtsCommandTemplate {
    public static final String ENGINE_MODE = "piper";
    public static final String DEFAULT_PIPER_EXE = "tools/piper/piper.exe";
    public static final String DEFAULT_SCRIPT = "scripts/tts/piper-file-to-wav.ps1";

    private PiperTtsCommandTemplate() {
    }

    /**
     * Resolves the effective command template for TTS settings.
     *
     * <p>When the user selects the managed simple voice mode, DocuPodcast derives the template from
     * the current application folder. Custom raw commands belong to external mode so old absolute
     * paths cannot contaminate this managed engine.</p>
     */
    public static Optional<String> resolve(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        if (!ENGINE_MODE.equalsIgnoreCase(current.tts().engineMode())) {
            return Optional.empty();
        }
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        Path script = root.resolve(DEFAULT_SCRIPT).normalize();
        Path piper = root.resolve(DEFAULT_PIPER_EXE).normalize();
        Path voiceModel = voiceModelPath(root, current);
        return Optional.of(build(script, piper, voiceModel));
    }

    public static Path voiceModelPath(Path applicationRoot, OperationalSettings settings) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        return PiperVoiceModelPathPolicy.voiceModelPath(root, settings);
    }

    public static boolean looksReady(OperationalSettings settings, Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".") : applicationRoot;
        return Files.isRegularFile(root.resolve(DEFAULT_SCRIPT).normalize())
                && Files.isRegularFile(root.resolve(DEFAULT_PIPER_EXE).normalize())
                && Files.isRegularFile(voiceModelPath(root, settings));
    }

    private static String build(Path script, Path piper, Path voiceModel) {
        return "powershell -NoProfile -ExecutionPolicy Bypass -File "
                + quote(script)
                + " -Piper " + quote(piper)
                + " -Model " + quote(voiceModel)
                + " -Text {textFile} -Output {outputFile}"
                + " -ComputePolicy {computePolicy} -Device {computeDevice} -GpuIndex {gpuIndex}";
    }

    private static String quote(Path path) {
        String text = Objects.toString(path, "").replace("\\", "/");
        return '"' + text.replace("\"", "") + '"';
    }

}
