package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ModelSetupProgressListener;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Executes and persists a CUDA smoke inside the self-contained Python runtime used by XTTS. */
public final class RunXttsCudaSmokeUseCase {
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final XttsCudaRuntimeProbeGateway probeGateway;

    public RunXttsCudaSmokeUseCase() {
        this(new ProcessXttsCudaRuntimeProbeGateway());
    }

    public RunXttsCudaSmokeUseCase(XttsCudaRuntimeProbeGateway probeGateway) {
        this.probeGateway = Objects.requireNonNull(probeGateway, "probeGateway");
    }

    public XttsCudaSmokeReport run(OperationalSettings settings, ComputeEnvironmentReport environmentReport, Path applicationRoot) {
        return run(settings, environmentReport, applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsCudaSmokeReport run(OperationalSettings settings, ComputeEnvironmentReport environmentReport, Path applicationRoot,
                                   ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        RuntimeArtifactPaths runtimePaths = RuntimeArtifactPaths.fromRoot(root);
        Path dir = runtimePaths.xttsSmokeDirectory();
        Path manifest = runtimePaths.xttsCudaSmokeManifest();
        ComputeEnvironmentReport environment = environmentReport == null
                ? new InspectComputeEnvironmentUseCase().inspect(current)
                : environmentReport;
        ArrayList<String> issues = new ArrayList<>();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            return reportAndMaybePersist(dir, manifest, null, false, false, false, false,
                    "", "", "", "", "", 0, List.of("No se pudo crear carpeta de smoke CUDA: " + ex.getMessage()),
                    "No se pudo preparar la prueba CUDA de Voz IA avanzada.");
        }

        OperationalSettings.ComputeSettings compute = current.compute();
        if (!compute.allowGpuForTts() || compute.policy() == ComputeDevicePolicy.CPU_ONLY) {
            issues.add("GPU para Voz IA avanzada está desactivada o la política actual es Solo CPU.");
            return persist(dir, manifest, false, true, false, false, "", "",
                    selectedDeviceId(environment, compute), "cpu", "", 0, issues,
                    "Voz IA avanzada seguirá en CPU porque GPU está desactivada para voz.");
        }
        ComputeDeviceDescriptor gpu = nvidiaGpu(environment, compute);
        if (gpu == null) {
            issues.add("No se detectó una GPU NVIDIA candidata para Voz IA avanzada.");
            return persist(dir, manifest, false, true, false, false, "", "",
                    selectedDeviceId(environment, compute), "cpu", "", 0, issues,
                    "GPU no confirmada: no hay GPU NVIDIA candidata para Voz IA avanzada.");
        }
        String selectedDeviceId = gpu.id();
        String deviceArgument = ComputeDeviceArgumentMapper.toCudaDeviceArgument(selectedDeviceId);
        if (!deviceArgument.startsWith("cuda:")) {
            issues.add("El dispositivo seleccionado no se puede mapear a un argumento CUDA válido: " + selectedDeviceId + ".");
            return persist(dir, manifest, false, true, false, false, "", "",
                    selectedDeviceId, deviceArgument, "", 0, issues,
                    "GPU no confirmada: el dispositivo no se puede usar como CUDA para Voz IA avanzada.");
        }
        Path python = runtimePaths.xttsPythonExecutable();
        if (!Files.isRegularFile(python)) {
            issues.add("No existe el Python local de Voz IA avanzada: " + python + ".");
            return persist(dir, manifest, false, false, false, false, "", "",
                    selectedDeviceId, deviceArgument, "", 0, issues,
                    "GPU no confirmada: falta el Python local de Voz IA avanzada.");
        }

        progress.onProgress("Ejecutando prueba CUDA dentro del Python local de Voz IA avanzada...");
        XttsCudaRuntimeProbeResult raw = probeGateway.probe(python, deviceArgument, timeoutSeconds());
        String json = firstJson(raw.stdout());
        boolean torchImportable = booleanValue(json, "torch_importable");
        boolean cudaAvailable = booleanValue(json, "cuda_available");
        String torchVersion = stringValue(json, "torch_version");
        String torchCudaVersion = stringValue(json, "torch_cuda_version");
        String deviceName = stringValue(json, "device_name");
        int deviceCount = intValue(json, "device_count");
        String error = stringValue(json, "error");
        if (raw.timedOut()) {
            issues.add("La prueba CUDA excedió el tiempo máximo.");
        }
        if (!torchImportable) {
            issues.add("PyTorch no está importable dentro del Python local de Voz IA avanzada.");
        }
        if (torchImportable && !cudaAvailable) {
            issues.add("PyTorch existe, pero CUDA no está disponible para este Python local.");
        }
        if (!error.isBlank()) {
            issues.add(error);
        }
        if (!raw.stderr().isBlank()) {
            issues.add("stderr: " + abbreviate(raw.stderr()));
        }
        String message = torchImportable && cudaAvailable
                ? "GPU confirmada para Voz IA avanzada dentro del Python local: " + (deviceName.isBlank() ? deviceArgument : deviceName) + "."
                : "GPU detectada por Windows, pero no disponible para Voz IA avanzada en este runtime Python.";
        return persist(dir, manifest, true, true, torchImportable, cudaAvailable, torchVersion, torchCudaVersion,
                selectedDeviceId, deviceArgument, deviceName, deviceCount, issues, message);
    }

    private static ComputeDeviceDescriptor nvidiaGpu(ComputeEnvironmentReport environment,
                                                     OperationalSettings.ComputeSettings compute) {
        String selected = selectedDeviceId(environment, compute);
        if (!selected.isBlank() && selected.toLowerCase(Locale.ROOT).startsWith("gpu-nvidia")) {
            return environment.devices().stream()
                    .filter(ComputeDeviceDescriptor::gpu)
                    .filter(device -> device.id().equalsIgnoreCase(selected))
                    .findFirst()
                    .orElse(null);
        }
        return environment.devices().stream()
                .filter(ComputeDeviceDescriptor::gpu)
                .filter(device -> device.vendor().toLowerCase(Locale.ROOT).contains("nvidia")
                        || device.id().toLowerCase(Locale.ROOT).startsWith("gpu-nvidia"))
                .findFirst()
                .orElse(null);
    }

    private static String selectedDeviceId(ComputeEnvironmentReport environment, OperationalSettings.ComputeSettings compute) {
        String selected = compute.selectedDeviceId() == null ? "" : compute.selectedDeviceId().strip();
        if (!selected.isBlank() && !"auto".equalsIgnoreCase(selected) && !"cpu".equalsIgnoreCase(selected)) {
            return selected;
        }
        return environment.selectedDevice()
                .filter(ComputeDeviceDescriptor::gpu)
                .map(ComputeDeviceDescriptor::id)
                .orElse("");
    }

    private XttsCudaSmokeReport persist(Path dir, Path manifest, boolean attempted, boolean pythonFound,
                                        boolean torchImportable, boolean cudaAvailable, String torchVersion,
                                        String torchCudaVersion, String selectedDeviceId, String deviceArgument,
                                        String deviceName, int deviceCount, List<String> issues, String message) {
        return reportAndMaybePersist(dir, manifest, Instant.now(), attempted, pythonFound, torchImportable,
                cudaAvailable, torchVersion, torchCudaVersion, selectedDeviceId, deviceArgument, deviceName,
                deviceCount, issues, message);
    }

    private XttsCudaSmokeReport reportAndMaybePersist(Path dir, Path manifest, Instant checkedAt, boolean attempted,
                                                      boolean pythonFound, boolean torchImportable, boolean cudaAvailable,
                                                      String torchVersion, String torchCudaVersion, String selectedDeviceId,
                                                      String deviceArgument, String deviceName, int deviceCount,
                                                      List<String> issues, String message) {
        XttsCudaSmokeReport report = new XttsCudaSmokeReport(dir, manifest, checkedAt, attempted, pythonFound,
                torchImportable, cudaAvailable, torchVersion, torchCudaVersion, selectedDeviceId, deviceArgument,
                deviceName, deviceCount, issues, message);
        try {
            Files.createDirectories(dir);
            Files.writeString(manifest, manifestJson(report), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // The returned report is still useful even if the manifest could not be persisted.
        }
        return report;
    }

    private static String manifestJson(XttsCudaSmokeReport report) {
        return "{\n"
                + "  \"schema\": \"docupodcast-xtts-cuda-smoke-v1\",\n"
                + "  \"checkedAt\": \"" + escape(report.checkedAt() == null ? "" : report.checkedAt().toString()) + "\",\n"
                + "  \"attempted\": " + report.attempted() + ",\n"
                + "  \"pythonFound\": " + report.pythonFound() + ",\n"
                + "  \"torchImportable\": " + report.torchImportable() + ",\n"
                + "  \"cudaAvailable\": " + report.cudaAvailable() + ",\n"
                + "  \"torchVersion\": \"" + escape(report.torchVersion()) + "\",\n"
                + "  \"torchCudaVersion\": \"" + escape(report.torchCudaVersion()) + "\",\n"
                + "  \"selectedDeviceId\": \"" + escape(report.selectedDeviceId()) + "\",\n"
                + "  \"deviceArgument\": \"" + escape(report.deviceArgument()) + "\",\n"
                + "  \"deviceName\": \"" + escape(report.deviceName()) + "\",\n"
                + "  \"deviceCount\": " + report.deviceCount() + ",\n"
                + "  \"gpuUsableForXtts\": " + report.gpuUsableForXtts() + ",\n"
                + "  \"issueSummary\": \"" + escape(String.join(" | ", report.issues())) + "\",\n"
                + "  \"userMessage\": \"" + escape(report.userMessage()) + "\"\n"
                + "}\n";
    }

    private static int timeoutSeconds() {
        String value = System.getProperty("docupodcast.xttsCudaSmoke.timeoutSeconds", "");
        if (value.isBlank()) {
            value = System.getenv("DOCUPODCAST_XTTS_CUDA_SMOKE_TIMEOUT_SECONDS");
        }
        try {
            return Math.max(5, Math.min(Integer.parseInt(value), 600));
        } catch (RuntimeException ex) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }

    private static String firstJson(String stdout) {
        if (stdout == null) {
            return "";
        }
        int start = stdout.indexOf('{');
        int end = stdout.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return stdout.substring(start, end + 1);
    }

    private static boolean booleanValue(String json, String key) {
        return json != null && json.contains("\"" + key + "\": true");
    }

    private static int intValue(String json, String key) {
        String marker = "\"" + key + "\": ";
        int start = json.indexOf(marker);
        if (start < 0) {
            return 0;
        }
        int from = start + marker.length();
        int to = from;
        while (to < json.length() && Character.isDigit(json.charAt(to))) {
            to++;
        }
        try {
            return Integer.parseInt(json.substring(from, to));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private static String stringValue(String json, String key) {
        if (json == null) {
            return "";
        }
        String marker = "\"" + key + "\": \"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int from = start + marker.length();
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                out.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString().strip();
    }

    private static String abbreviate(String text) {
        String normalized = text == null ? "" : text.replace('\r', ' ').replace('\n', ' ').strip();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
