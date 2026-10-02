package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceArgumentMapper;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Resolves the configured priority to one exact ComfyUI backend without hiding the result. */
public final class VisualComputeBackendResolver {
    public VisualComputeBinding resolve(OperationalSettings.ComputeSettings settings,
                                        List<ComputeDeviceDescriptor> detectedDevices,
                                        ComfyUiRuntimeCapabilities capabilities) {
        OperationalSettings.ComputeSettings current = settings == null
                ? OperationalSettings.ComputeSettings.defaults()
                : settings;
        List<ComputeDeviceDescriptor> devices = detectedDevices == null ? List.of() : detectedDevices;
        if (current.policy() == ComputeDevicePolicy.CPU_ONLY) {
            return resolveDevice(requireDetected(devices, "cpu"), capabilities);
        }

        String selectedId = clean(current.selectedDeviceId()).toLowerCase(Locale.ROOT);
        boolean explicit = !selectedId.isBlank() && !"auto".equals(selectedId);
        if (explicit) {
            ComputeDeviceDescriptor selected = devices.stream()
                    .filter(device -> device.detected() && device.id().equalsIgnoreCase(selectedId))
                    .findFirst()
                    .orElse(null);
            if (selected != null) {
                try {
                    return resolveDevice(selected, capabilities);
                } catch (IllegalStateException unsupported) {
                    if (current.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE) throw unsupported;
                }
            } else if (current.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE) {
                throw new IllegalStateException(
                        "El dispositivo seleccionado '" + selectedId + "' no está disponible. "
                                + "Vuelve a seleccionarlo en Configuración.");
            }
        } else if (current.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE) {
            throw new IllegalStateException(
                    "La política exige un dispositivo concreto en Configuración > Rendimiento.");
        }

        for (ComputeDeviceDescriptor candidate : devices.stream()
                .filter(ComputeDeviceDescriptor::detected)
                .filter(ComputeDeviceDescriptor::gpu)
                .sorted(Comparator.comparingInt(VisualComputeBackendResolver::gpuPriority))
                .toList()) {
            try {
                return resolveDevice(candidate, capabilities);
            } catch (IllegalStateException unsupported) {
                // AUTO/PREFER_GPU may try the next detected compatible GPU.
            }
        }
        return resolveDevice(requireDetected(devices, "cpu"), capabilities);
    }

    private static VisualComputeBinding resolveDevice(
            ComputeDeviceDescriptor selected, ComfyUiRuntimeCapabilities capabilities) {
        if ("cpu".equalsIgnoreCase(selected.id()) || !selected.gpu()) {
            requireFlag(capabilities, "--cpu");
            return new VisualComputeBinding(selected.id(), selected.displayName(),
                    VisualComputeBackend.CPU, -1, List.of("--cpu"));
        }

        int index = parseIndex(selected.id());
        String vendor = (selected.vendor() + " " + selected.displayName()).toLowerCase(Locale.ROOT);
        if (vendor.contains("nvidia") || vendor.contains("geforce")
                || vendor.contains("gtx") || vendor.contains("rtx")) {
            requireFlag(capabilities, "--cuda-device");
            return new VisualComputeBinding(selected.id(), selected.displayName(),
                    VisualComputeBackend.CUDA, index,
                    List.of("--cuda-device", Integer.toString(index)));
        }
        if (vendor.contains("intel")) {
            if (supports(capabilities, "--oneapi-device-selector")) {
                return new VisualComputeBinding(selected.id(), selected.displayName(),
                        VisualComputeBackend.XPU, index,
                        List.of("--oneapi-device-selector", "level_zero:" + index));
            }
            requireFlag(capabilities, "--directml");
            return new VisualComputeBinding(selected.id(), selected.displayName(),
                    VisualComputeBackend.DIRECT_ML, index,
                    List.of("--directml", Integer.toString(index)));
        }
        if (vendor.contains("amd") || vendor.contains("radeon")) {
            requireFlag(capabilities, "--directml");
            return new VisualComputeBinding(selected.id(), selected.displayName(),
                    VisualComputeBackend.DIRECT_ML, index,
                    List.of("--directml", Integer.toString(index)));
        }
        throw new IllegalStateException("El dispositivo '" + selected.displayName()
                + "' no tiene un backend visual local compatible configurado.");
    }

    private static ComputeDeviceDescriptor requireDetected(
            List<ComputeDeviceDescriptor> devices, String id) {
        return devices.stream()
                .filter(ComputeDeviceDescriptor::detected)
                .filter(device -> device.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No se detectó el dispositivo requerido: " + id + "."));
    }

    private static int gpuPriority(ComputeDeviceDescriptor device) {
        String identity = (device.vendor() + " " + device.displayName()).toLowerCase(Locale.ROOT);
        if (identity.contains("nvidia") || identity.contains("geforce")
                || identity.contains("gtx") || identity.contains("rtx")) return 0;
        if (identity.contains("amd") || identity.contains("radeon")) return 1;
        if (identity.contains("intel") || identity.contains("arc")
                || identity.contains("iris") || identity.contains("uhd")) return 2;
        return 3;
    }

    private static void requireFlag(ComfyUiRuntimeCapabilities capabilities, String flag) {
        if (capabilities != null && capabilities.probed() && !capabilities.supports(flag)) {
            throw new IllegalStateException("El runtime ComfyUI seleccionado no admite " + flag
                    + ". Instala el runtime administrado compatible con el dispositivo elegido.");
        }
    }

    private static boolean supports(ComfyUiRuntimeCapabilities capabilities, String flag) {
        return capabilities != null && capabilities.supports(flag);
    }

    private static int parseIndex(String selectedDeviceId) {
        String value = ComputeDeviceArgumentMapper.gpuIndex(selectedDeviceId);
        try {
            int index = Integer.parseInt(value);
            if (index >= 0) return index;
        } catch (NumberFormatException ignored) {
            // The diagnostic below names the exact invalid configured value.
        }
        throw new IllegalStateException(
                "No se pudo resolver el índice del dispositivo '" + selectedDeviceId + "'.");
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
