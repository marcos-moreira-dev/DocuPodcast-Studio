package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.Locale;

/** Confirms that a running ComfyUI endpoint is using the configured device. */
public final class VisualComputeBindingVerifier {
    public Verification verify(VisualComputeBinding expected, ComfyUiSystemStats stats) {
        if (expected == null) {
            return Verification.failed("No hay binding de dispositivo visual para verificar.");
        }
        if (stats == null || stats.devices().isEmpty()) {
            return Verification.failed("ComfyUI respondio, pero /system_stats no identifico ningun dispositivo.");
        }
        ComfyUiSystemStats.Device actual = stats.devices().stream()
                .filter(device -> device.index() == Math.max(0, expected.deviceIndex()))
                .filter(device -> backendMatches(expected.backend(), device))
                .findFirst()
                .orElse(null);
        if (actual == null && expected.backend() == VisualComputeBackend.CPU) {
            actual = stats.devices().stream()
                    .filter(device -> backendMatches(expected.backend(), device))
                    .findFirst().orElse(null);
        }
        if (actual == null) {
            return Verification.failed("ComfyUI esta usando un backend o indice distinto. Esperado: "
                    + expected.backend() + " " + expected.deviceIndex()
                    + "; detectado: " + stats.devices() + ".");
        }
        if (!nameMatches(expected, actual)) {
            return Verification.failed("ComfyUI no coincide con el dispositivo seleccionado. Esperado: "
                    + expected.displayName() + "; detectado: " + actual.name() + ".");
        }
        return new Verification(true, actual,
                "Dispositivo visual verificado: " + actual.name() + " (" + actual.type() + ").");
    }

    private static boolean backendMatches(VisualComputeBackend backend, ComfyUiSystemStats.Device device) {
        String declaredType = normalize(device.type());
        if (declaredType.contains("directml")
                || declaredType.contains("dml")
                || declaredType.contains("privateuseone")) {
            return backend == VisualComputeBackend.DIRECT_ML;
        }
        if (declaredType.contains("xpu")
                || declaredType.contains("oneapi")
                || declaredType.contains("level_zero")) {
            return backend == VisualComputeBackend.XPU;
        }
        if (declaredType.contains("cuda")) {
            return backend == VisualComputeBackend.CUDA;
        }
        if (declaredType.contains("cpu")) {
            return backend == VisualComputeBackend.CPU;
        }
        String type = (declaredType + " " + normalize(device.name())).strip();
        return switch (backend) {
            case CUDA -> type.contains("cuda") || type.contains("nvidia");
            case XPU -> type.contains("xpu") || type.contains("oneapi") || type.contains("level_zero");
            case DIRECT_ML -> type.contains("directml") || type.contains("dml") || type.contains("privateuseone");
            case CPU -> type.contains("cpu");
        };
    }

    private static boolean nameMatches(VisualComputeBinding expected, ComfyUiSystemStats.Device actual) {
        if (expected.backend() == VisualComputeBackend.CPU) {
            return true;
        }
        String expectedName = normalize(expected.displayName());
        String actualName = normalize(actual.name());
        if (expectedName.isBlank() || actualName.isBlank()) {
            return false;
        }
        if (expectedName.contains(actualName) || actualName.contains(expectedName)) {
            return true;
        }
        java.util.Set<String> ignored = java.util.Set.of(
                "gpu", "device", "local", "nvidia", "intel", "amd", "graphics", "adapter",
                "vram", "ram", "memory");
        java.util.List<String> identity = java.util.Arrays.stream(expectedName.split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !ignored.contains(token))
                .toList();
        return !identity.isEmpty() && identity.stream().allMatch(actualName::contains);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    public record Verification(boolean matches,
                               ComfyUiSystemStats.Device actualDevice,
                               String message) {
        public Verification {
            message = message == null ? "" : message.strip();
        }

        public static Verification failed(String message) {
            return new Verification(false, null, message);
        }
    }
}
