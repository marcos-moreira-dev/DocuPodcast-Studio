package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Stable-enough local hardware signature used to expire physical certifications. */
final class EngineHardwareFingerprint {
    private EngineHardwareFingerprint() { }

    static String current(ComputePreference preference) {
        String source = System.getProperty("os.name", "") + "|"
                + System.getProperty("os.arch", "") + "|"
                + Runtime.getRuntime().availableProcessors() + "|"
                + totalPhysicalBytes();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static long totalPhysicalBytes() {
        var bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean extended) {
            return Math.max(0L, extended.getTotalMemorySize());
        }
        return Runtime.getRuntime().maxMemory();
    }
}
