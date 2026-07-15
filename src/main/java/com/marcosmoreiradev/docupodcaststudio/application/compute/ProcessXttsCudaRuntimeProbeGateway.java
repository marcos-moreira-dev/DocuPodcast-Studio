package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Process-based implementation of the CUDA smoke for the local Python runtime. */
public final class ProcessXttsCudaRuntimeProbeGateway implements XttsCudaRuntimeProbeGateway {
    private final ExternalProcessRunner runner;

    public ProcessXttsCudaRuntimeProbeGateway() {
        this(ExternalProcessRunner.unavailable("ProcessXttsCudaRuntimeProbeGateway"));
    }

    public ProcessXttsCudaRuntimeProbeGateway(ExternalProcessRunner runner) {
        this.runner = runner == null
                ? ExternalProcessRunner.unavailable("ProcessXttsCudaRuntimeProbeGateway")
                : runner;
    }

    private static final String PYTHON_PROBE = """
            import json, sys
            info = {
                "torch_importable": False,
                "cuda_available": False,
                "torch_version": "",
                "torch_cuda_version": "",
                "device_argument": sys.argv[1] if len(sys.argv) > 1 else "",
                "device_name": "",
                "device_count": 0,
                "error": ""
            }
            try:
                import torch
                info["torch_importable"] = True
                info["torch_version"] = str(getattr(torch, "__version__", ""))
                info["torch_cuda_version"] = str(getattr(torch.version, "cuda", "") or "")
                info["cuda_available"] = bool(torch.cuda.is_available())
                if info["cuda_available"]:
                    info["device_count"] = int(torch.cuda.device_count())
                    if info["device_argument"].startswith("cuda:"):
                        try:
                            index = int(info["device_argument"].split(":", 1)[1])
                            if index < 0 or index >= info["device_count"]:
                                info["cuda_available"] = False
                                info["error"] = "El indice CUDA solicitado no existe en este runtime."
                            else:
                                info["device_name"] = str(torch.cuda.get_device_name(index))
                        except Exception as exc:
                            info["cuda_available"] = False
                            info["error"] = str(exc)
            except Exception as exc:
                info["error"] = str(exc)
            print(json.dumps(info, ensure_ascii=False))
            sys.exit(0 if info.get("torch_importable") and info.get("cuda_available") else 2)
            """;

    @Override
    public XttsCudaRuntimeProbeResult probe(Path pythonExecutable, String deviceArgument, int timeoutSeconds) {
        if (pythonExecutable == null) {
            return new XttsCudaRuntimeProbeResult(1, "", "Python local no configurado.", false);
        }
        int timeout = Math.max(5, Math.min(timeoutSeconds <= 0 ? 30 : timeoutSeconds, 600));
        Path probeScript = null;
        try {
            probeScript = Files.createTempFile("docupodcast-cuda-probe-", ".py");
            Files.writeString(probeScript, PYTHON_PROBE, StandardCharsets.UTF_8);
            ExternalProcessRequest request = new ExternalProcessRequest(
                    List.of(pythonExecutable.toString(), probeScript.toString(), deviceArgument == null ? "" : deviceArgument),
                    null,
                    Duration.ofSeconds(timeout),
                    Map.of("PYTHONUNBUFFERED", "1", "PYTHONIOENCODING", "utf-8"),
                    "xtts-cuda-smoke",
                    false
            );
            ExternalProcessResult result = runner.run(request);
            return new XttsCudaRuntimeProbeResult(result.exitCode(), result.stdout(), result.stderr(), result.timedOut());
        } catch (IOException ex) {
            return new XttsCudaRuntimeProbeResult(1, "", ex.getMessage(), false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new XttsCudaRuntimeProbeResult(1, "", "Prueba CUDA interrumpida.", false);
        } finally {
            if (probeScript != null) {
                try {
                    Files.deleteIfExists(probeScript);
                } catch (IOException ignored) {
                    // Temporal probe scripts are best-effort cleanup.
                }
            }
        }
    }
}
