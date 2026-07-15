package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSmokeTestUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reads the last CUDA smoke result produced for the local XTTS Python runtime. */
public final class InspectXttsCudaSmokeUseCase {
    public XttsCudaSmokeReport inspect(Path applicationRoot) {
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path dir = smokeDirectory(root);
        Path manifest = RuntimeArtifactPaths.fromRoot(root).xttsCudaSmokeManifest();
        if (!manifest.getFileName().toString().equals(XttsCudaSmokeReport.MANIFEST_NAME)) {
            manifest = dir.resolve(XttsCudaSmokeReport.MANIFEST_NAME);
        }
        if (!Files.isRegularFile(manifest)) {
            return XttsCudaSmokeReport.pending(dir,
                    "Falta ejecutar la prueba CUDA de Voz IA avanzada dentro del Python local.");
        }
        try {
            String json = Files.readString(manifest);
            boolean attempted = booleanValue(json, "attempted");
            boolean pythonFound = booleanValue(json, "pythonFound");
            boolean torchImportable = booleanValue(json, "torchImportable");
            boolean cudaAvailable = booleanValue(json, "cudaAvailable");
            String selectedDeviceId = stringValue(json, "selectedDeviceId");
            String deviceArgument = stringValue(json, "deviceArgument");
            String torchVersion = stringValue(json, "torchVersion");
            String torchCudaVersion = stringValue(json, "torchCudaVersion");
            String deviceName = stringValue(json, "deviceName");
            int deviceCount = intValue(json, "deviceCount");
            String message = stringValue(json, "userMessage");
            Instant checkedAt = instantValue(json, "checkedAt");
            List<String> issues = issues(json);
            return new XttsCudaSmokeReport(dir, manifest, checkedAt, attempted, pythonFound, torchImportable,
                    cudaAvailable, torchVersion, torchCudaVersion, selectedDeviceId, deviceArgument, deviceName,
                    deviceCount, issues, message);
        } catch (IOException ex) {
            return new XttsCudaSmokeReport(dir, manifest, null, false, false, false, false,
                    "", "", "", "", "", 0, List.of("No se pudo leer el manifiesto CUDA: " + ex.getMessage()),
                    "No se pudo leer la prueba CUDA de Voz IA avanzada.");
        }
    }

    public static Path smokeDirectory(Path applicationRoot) {
        return RuntimeArtifactPaths.fromRoot(applicationRoot).xttsSmokeDirectory();
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

    private static Instant instantValue(String json, String key) {
        String value = stringValue(json, key);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
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

    private static List<String> issues(String json) {
        String value = stringValue(json, "issueSummary");
        if (value.isBlank()) {
            return List.of();
        }
        ArrayList<String> issues = new ArrayList<>();
        for (String token : value.split("\\|")) {
            String normalized = token.strip();
            if (!normalized.isBlank()) {
                issues.add(normalized);
            }
        }
        return issues;
    }
}
