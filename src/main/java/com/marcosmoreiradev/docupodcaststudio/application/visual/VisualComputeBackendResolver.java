package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceArgumentMapper;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.List;
import java.util.Locale;

/** Resolves the configured device to one exact ComfyUI backend without fallback. */
public final class VisualComputeBackendResolver {
    public VisualComputeBinding resolve(OperationalSettings.ComputeSettings settings,
                                        List<ComputeDeviceDescriptor> detectedDevices,
                                        ComfyUiRuntimeCapabilities capabilities) {
        OperationalSettings.ComputeSettings current = settings == null
                ? OperationalSettings.ComputeSettings.defaults()
                : settings;
        String selectedId = clean(current.selectedDeviceId()).toLowerCase(Locale.ROOT);
        if (current.policy() == ComputeDevicePolicy.CPU_ONLY) {
            selectedId = "cpu";
        }
        if (selectedId.isBlank() || "auto".equals(selectedId)) {
            throw new IllegalStateException(
                    "Selecciona un dispositivo concreto en Configuracion > Generacion visual. "
                            + "La generacion no usa fallback automatico.");
        }
        String requestedId = selectedId;
        List<ComputeDeviceDescriptor> devices = detectedDevices == null ? List.of() : detectedDevices;
        ComputeDeviceDescriptor selected = devices.stream()
                .filter(device -> device.detected() && device.id().equalsIgnoreCase(requestedId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El dispositivo seleccionado '" + requestedId + "' no esta disponible. "
                                + "Vuelve a seleccionarlo en Configuracion."));
        if ("cpu".equals(selected.id()) || !selected.gpu()) {
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
            if (index >= 0) {
                return index;
            }
        } catch (NumberFormatException ignored) {
            // The diagnostic below names the exact invalid configured value.
        }
        throw new IllegalStateException("No se pudo resolver el indice del dispositivo '" + selectedDeviceId + "'.");
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
