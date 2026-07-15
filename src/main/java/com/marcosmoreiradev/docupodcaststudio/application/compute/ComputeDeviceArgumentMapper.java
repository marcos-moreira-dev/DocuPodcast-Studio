package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.Locale;

/** Maps the user's processing-device intent to process arguments without hiding manual choices. */
public final class ComputeDeviceArgumentMapper {
    private ComputeDeviceArgumentMapper() {
    }

    public static String toProcessDeviceArgument(ComputeDevicePolicy policy, String selectedDeviceId) {
        ComputeDevicePolicy normalizedPolicy = policy == null ? ComputeDevicePolicy.AUTO : policy;
        String selected = normalize(selectedDeviceId);
        if (normalizedPolicy == ComputeDevicePolicy.CPU_ONLY || "cpu".equals(selected)) {
            return "cpu";
        }
        if (selected.isBlank()) {
            return normalizedPolicy == ComputeDevicePolicy.SPECIFIC_DEVICE ? "" : "auto";
        }
        if ("auto".equals(selected)) {
            return normalizedPolicy == ComputeDevicePolicy.SPECIFIC_DEVICE ? "" : "auto";
        }
        return selected;
    }

    public static String toCudaDeviceArgument(String selectedDeviceId) {
        String selected = normalize(selectedDeviceId);
        if (selected.startsWith("cuda")) {
            return selected;
        }
        if (selected.startsWith("gpu-nvidia")) {
            return "cuda:" + gpuIndex(selected);
        }
        if ("nvidia".equals(selected) || "gpu".equals(selected)) {
            return "cuda:0";
        }
        return selected;
    }

    public static String gpuIndex(String selectedDeviceId) {
        String selected = normalize(selectedDeviceId);
        int lastDash = selected.lastIndexOf('-');
        if (lastDash >= 0 && lastDash + 1 < selected.length()) {
            String suffix = selected.substring(lastDash + 1);
            if (suffix.chars().allMatch(Character::isDigit)) {
                return suffix;
            }
        }
        int colon = selected.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < selected.length()) {
            String suffix = selected.substring(colon + 1);
            if (suffix.chars().allMatch(Character::isDigit)) {
                return suffix;
            }
        }
        return selected.startsWith("gpu") || selected.startsWith("cuda") ? "0" : "-1";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
