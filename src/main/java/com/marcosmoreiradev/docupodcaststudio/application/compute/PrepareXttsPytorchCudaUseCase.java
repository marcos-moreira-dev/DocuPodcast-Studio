package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Installs CUDA-enabled PyTorch into the local XTTS venv without touching global Python. */
public final class PrepareXttsPytorchCudaUseCase {
    private static final long TIMEOUT_MINUTES = 90;
    private final ExternalProcessRunner runner;

    public PrepareXttsPytorchCudaUseCase() {
        this(ExternalProcessRunner.unavailable("PrepareXttsPytorchCudaUseCase"));
    }

    public PrepareXttsPytorchCudaUseCase(ExternalProcessRunner runner) {
        this.runner = runner == null ? ExternalProcessRunner.unavailable("PrepareXttsPytorchCudaUseCase") : runner;
    }

    public XttsCudaPreparationReport prepare(Path applicationRoot) {
        return prepare(applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsCudaPreparationReport prepare(Path applicationRoot, ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths paths = RuntimeArtifactPaths.fromRoot(root);
        Path script = paths.scriptsRoot().resolve("tts/setup-xtts-pytorch-cuda.ps1").normalize();
        Path logFile = paths.xttsSmokeDirectory().resolve("xtts-pytorch-cuda-install.log").normalize();
        Path python = paths.xttsPythonExecutable();
        if (!Files.isRegularFile(python)) {
            return XttsCudaPreparationReport.failed(script, logFile,
                    "No existe el Python local de Voz IA avanzada. Prepara primero el runtime XTTS portable.", List.of());
        }
        if (!Files.isRegularFile(script)) {
            return XttsCudaPreparationReport.failed(script, logFile,
                    "No se encontro el instalador local de PyTorch CUDA para Voz IA avanzada.", List.of());
        }
        try {
            Files.createDirectories(logFile.getParent());
        } catch (IOException ex) {
            return XttsCudaPreparationReport.failed(script, logFile,
                    "No se pudo crear la carpeta de logs CUDA: " + ex.getMessage(), List.of());
        }

        try {
            progress.onProgress("Instalando PyTorch CUDA dentro del Python local de Voz IA avanzada. No se usa Python global.");
            ArrayList<String> lines = new ArrayList<>();
            ExternalProcessRequest request = ExternalProcessRequest.of(commandFor(script), "xtts-pytorch-cuda-install",
                            Duration.ofMinutes(TIMEOUT_MINUTES))
                    .withWorkingDirectory(root)
                    .redirectingErrorStream();
            ExternalProcessResult result = runner.run(request, outputObserver(lines, progress));
            if (lines.isEmpty()) {
                lines.addAll(linesFrom(result.combinedOutputTail()));
            }
            if (result.timedOut()) {
                return new XttsCudaPreparationReport(false, -1, true, script, logFile, List.copyOf(lines),
                        "La preparacion CUDA tardo demasiado y fue detenida. Revisa la conexion y vuelve a intentar.");
            }
            boolean success = result.exitCode() == 0;
            String message = success
                    ? "PyTorch CUDA quedo instalado en el Python local de Voz IA avanzada. Ejecuta la prueba GPU para confirmar CUDA."
                    : "La instalacion local de PyTorch CUDA fallo. Revisa el log y el detalle tecnico.";
            progress.onProgress(message);
            return new XttsCudaPreparationReport(success, result.exitCode(), false, script, logFile, List.copyOf(lines), message);
        } catch (IOException ex) {
            return XttsCudaPreparationReport.failed(script, logFile,
                    "No se pudo iniciar la preparacion CUDA: " + ex.getMessage(), List.of());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return XttsCudaPreparationReport.failed(script, logFile,
                    "La preparacion CUDA fue interrumpida antes de terminar.", List.of(Objects.toString(ex.getMessage(), "")));
        }
    }

    private static List<String> commandFor(Path script) {
        return List.of(resolvePowerShellCommand(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), "-Force");
    }

    private static String resolvePowerShellCommand() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("win") ? "powershell.exe" : "pwsh";
    }

    private static ExternalProcessObserver outputObserver(ArrayList<String> lines, ModelSetupProgressListener progress) {
        return new ExternalProcessObserver() {
            @Override
            public void onOutputLine(String line) {
                String normalized = line == null ? "" : line.strip();
                if (normalized.isBlank()) {
                    return;
                }
                lines.add(normalized);
                progress.onProgress(normalized);
                trim(lines);
            }
        };
    }

    private static void trim(ArrayList<String> lines) {
        while (lines.size() > 160) {
            lines.remove(0);
        }
    }

    private static List<String> linesFrom(String output) {
        String text = output == null ? "" : output.strip();
        if (text.isBlank()) {
            return List.of();
        }
        return text.lines().map(String::strip).filter(line -> !line.isBlank()).toList();
    }
}
