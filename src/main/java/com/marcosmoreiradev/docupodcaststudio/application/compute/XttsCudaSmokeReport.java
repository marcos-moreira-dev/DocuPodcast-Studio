package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Result of checking CUDA inside the self-contained Python runtime used by Voz IA avanzada. */
public record XttsCudaSmokeReport(
        Path smokeDirectory,
        Path manifestFile,
        Instant checkedAt,
        boolean attempted,
        boolean pythonFound,
        boolean torchImportable,
        boolean cudaAvailable,
        String torchVersion,
        String torchCudaVersion,
        String selectedDeviceId,
        String deviceArgument,
        String deviceName,
        int deviceCount,
        List<String> issues,
        String userMessage
) {
    public static final String MANIFEST_NAME = "xtts-cuda-smoke.json";

    public XttsCudaSmokeReport {
        smokeDirectory = smokeDirectory == null ? Path.of("runtime/tts/xtts-smoke") : smokeDirectory;
        manifestFile = manifestFile == null ? smokeDirectory.resolve(MANIFEST_NAME) : manifestFile;
        torchVersion = normalize(torchVersion);
        torchCudaVersion = normalize(torchCudaVersion);
        selectedDeviceId = normalize(selectedDeviceId);
        deviceArgument = normalize(deviceArgument);
        deviceName = normalize(deviceName);
        deviceCount = Math.max(0, deviceCount);
        issues = List.copyOf(issues == null ? List.of() : issues);
        userMessage = normalize(userMessage);
    }

    public boolean gpuUsableForXtts() {
        return attempted && pythonFound && torchImportable && cudaAvailable && deviceArgument.startsWith("cuda:");
    }

    public boolean needsSmoke() {
        return !gpuUsableForXtts();
    }

    public String statusLabel() {
        if (gpuUsableForXtts()) {
            return "GPU confirmada para Voz IA avanzada";
        }
        if (!pythonFound) {
            return "Python local pendiente";
        }
        if (!attempted) {
            return "Prueba CUDA pendiente";
        }
        if (!torchImportable) {
            return "PyTorch no disponible en este Python";
        }
        if (!cudaAvailable) {
            return "CUDA no disponible en este Python";
        }
        return "GPU no confirmada";
    }

    public static XttsCudaSmokeReport pending(Path smokeDirectory, String message) {
        Path dir = smokeDirectory == null ? Path.of("runtime/tts/xtts-smoke") : smokeDirectory;
        return new XttsCudaSmokeReport(dir, dir.resolve(MANIFEST_NAME), null, false, false, false, false,
                "", "", "", "", "", 0, List.of(message), message);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
