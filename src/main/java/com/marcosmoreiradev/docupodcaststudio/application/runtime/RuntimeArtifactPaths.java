package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Single source of truth for local runtime artifacts used by engines, smoke tests and packaging.
 *
 * <p>This policy does not install anything. It only resolves paths in a portable and auditable way
 * so Java, PowerShell, Python wrappers and diagnostic scripts converge on the same layout.</p>
 */
public final class RuntimeArtifactPaths {
    private final ApplicationRuntimeLayout layout;

    public static RuntimeArtifactPaths fromRoot(Path applicationRoot) {
        return new RuntimeArtifactPaths(new ApplicationRuntimeLayout(applicationRoot == null ? Path.of(".") : applicationRoot));
    }

    public static RuntimeArtifactPaths fromLayout(ApplicationRuntimeLayout layout) {
        return new RuntimeArtifactPaths(layout);
    }

    public RuntimeArtifactPaths(ApplicationRuntimeLayout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public ApplicationRuntimeLayout layout() {
        return layout;
    }

    public Path applicationRoot() {
        return layout.applicationRoot();
    }

    public Path toolsRoot() {
        return layout.toolsRoot();
    }

    public Path modelsRoot() {
        return layout.modelsRoot();
    }

    public Path scriptsRoot() {
        return layout.scriptsRoot();
    }

    public Path runtimeRoot() {
        return applicationRoot().resolve("runtime").normalize();
    }

    public Path xttsWrapperDirectory() {
        return toolsRoot().resolve("xtts-wrapper").normalize();
    }

    public Path xttsVirtualEnvironment() {
        return xttsWrapperDirectory().resolve(".venv").normalize();
    }

    public Path xttsPythonExecutable() {
        return layout.xttsPython();
    }

    public Path xttsSynthesizeScript() {
        return layout.xttsWrapperScript();
    }

    public Path xttsWrapperScript() {
        return layout.xttsWrapperScript();
    }

    public Path xttsPowerShellScript() {
        return scriptsRoot().resolve("tts/xtts-file-to-wav.ps1").normalize();
    }

    public Path xttsPortableSetupScript() {
        return scriptsRoot().resolve("tts/setup-xtts-portable-python.ps1").normalize();
    }

    public Path xttsModelDirectory() {
        return modelsRoot().resolve("tts/xtts").normalize();
    }

    public Path xttsDefaultSpeakerWav() {
        return xttsModelDirectory().resolve("speakers/voz-por-defecto.wav").normalize();
    }

    public Path xttsSmokeDirectory() {
        return runtimeRoot().resolve("tts/xtts-smoke").normalize();
    }

    public Path xttsReadinessSmokeManifest() {
        return xttsSmokeDirectory().resolve("xtts-readiness-smoke.json").normalize();
    }

    public Path xttsCudaSmokeManifest() {
        return xttsSmokeDirectory().resolve("xtts-cuda-smoke.json").normalize();
    }

    public Path piperRoot() {
        return toolsRoot().resolve("piper").normalize();
    }

    public Path piperExecutable() {
        return layout.piperExecutable();
    }

    public Path piperPowerShellScript() {
        return scriptsRoot().resolve("tts/piper-file-to-wav.ps1").normalize();
    }

    public Path piperVoicesDirectory() {
        return modelsRoot().resolve("tts/piper/voices").normalize();
    }

    public Path piperDefaultVoiceModel() {
        return piperVoicesDirectory().resolve(PiperVoiceModelPathPolicy.DEFAULT_PIPER_VOICE).normalize();
    }

    public Path piperDefaultVoiceMetadata() {
        return Path.of(piperDefaultVoiceModel().toString() + ".json").normalize();
    }

    public Path ffmpegRoot() {
        return toolsRoot().resolve("ffmpeg").normalize();
    }

    public Path ffmpegBinDirectory() {
        return ffmpegRoot().resolve("bin").normalize();
    }

    public Path ffmpegExecutable() {
        return layout.ffmpegExecutable();
    }

    public Path ffprobeExecutable() {
        return layout.ffprobeExecutable();
    }

    public Path tesseractRoot() {
        return toolsRoot().resolve("tesseract").normalize();
    }

    public Path tesseractBinDirectory() {
        return tesseractRoot().resolve("bin").normalize();
    }

    public Path tesseractExecutable() {
        return tesseractBinDirectory().resolve("tesseract.exe").normalize();
    }

    public Path voiceLibraryRoot() {
        return layout.voiceLibraryRoot();
    }

    public Path voiceSamplesDirectory() {
        return layout.voiceSamplesRoot();
    }

    public Path voiceTempRecordingsDirectory() {
        return voiceLibraryRoot().resolve("tmp-recordings").normalize();
    }

    public Path resolveBundled(String relativePath) {
        return layout.resolveBundled(relativePath);
    }

    public Path resolveConfiguredPath(String configuredPath) {
        return layout.resolveConfiguredPath(configuredPath);
    }
}
