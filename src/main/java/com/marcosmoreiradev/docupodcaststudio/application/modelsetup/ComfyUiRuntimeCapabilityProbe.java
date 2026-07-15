package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Probes local ComfyUI help output without assuming every runtime supports the same flags. */
public final class ComfyUiRuntimeCapabilityProbe {
    public ComfyUiRuntimeCapabilities probe(Path runtime, ExternalProcessRunner runner) {
        if (runtime == null || runner == null) {
            return ComfyUiRuntimeCapabilities.unknown("Runtime o runner no disponible para probe ComfyUI.");
        }
        ProbeCommand command = probeCommand(runtime);
        if (command == null) {
            return ComfyUiRuntimeCapabilities.unknown("No se encontro main.py con Python local para probe ComfyUI.");
        }
        try {
            ExternalProcessResult result = runner.run(ExternalProcessRequest.of(
                            command.command(),
                            "comfyui-help-probe",
                            Duration.ofSeconds(8))
                    .withWorkingDirectory(command.workingDirectory())
                    .redirectingErrorStream());
            if (!result.succeeded()) {
                return ComfyUiRuntimeCapabilities.unknown("Probe ComfyUI fallo: " + result.combinedOutputTail());
            }
            return ComfyUiRuntimeCapabilities.fromHelpText(result.stdout() + "\n" + result.stderr());
        } catch (Exception ex) {
            return ComfyUiRuntimeCapabilities.unknown("Probe ComfyUI no ejecutado: " + ex.getMessage());
        }
    }

    private static ProbeCommand probeCommand(Path runtime) {
        Path root = runtime.toAbsolutePath().normalize();
        Path main = Files.isRegularFile(root.resolve("main.py"))
                ? root.resolve("main.py")
                : root.resolve("ComfyUI/main.py");
        Path python = firstExisting(root,
                "python_embeded/python.exe", "python_embedded/python.exe", "venv/Scripts/python.exe", ".venv/Scripts/python.exe");
        if (!Files.isRegularFile(main) || python == null) {
            return null;
        }
        Path workingDirectory = main.getParent() == null ? root : main.getParent();
        return new ProbeCommand(List.of(python.toString(), main.toString(), "--help"), workingDirectory);
    }

    private static Path firstExisting(Path root, String... names) {
        for (String name : names) {
            Path candidate = root.resolve(name).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private record ProbeCommand(List<String> command, Path workingDirectory) {
    }
}
