package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Conservative host-memory check before loading the 20+ GB FLUX bundle. */
public final class FluxMemoryPreflight {
    public static final long MIN_COMMITTABLE_BYTES = 24L * 1024L * 1024L * 1024L;

    public Report inspect(Path applicationRoot) {
        long freePhysical = 0L;
        long freeSwap = 0L;
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean os) {
                freePhysical = Math.max(0L, os.getFreeMemorySize());
                freeSwap = Math.max(0L, os.getFreeSwapSpaceSize());
            }
        } catch (RuntimeException ignored) {
            // Unknown values are reported instead of pretending the host is ready.
        }
        long disk = 0L;
        try {
            Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize()
                    : applicationRoot.toAbsolutePath().normalize();
            disk = Files.getFileStore(root).getUsableSpace();
        } catch (Exception ignored) {
            disk = 0L;
        }
        long committable = freePhysical + freeSwap;
        boolean measurable = freePhysical > 0 || freeSwap > 0;
        boolean ready = measurable && committable >= MIN_COMMITTABLE_BYTES;
        String message = ready
                ? "Memoria disponible para iniciar FLUX con offload."
                : "FLUX requiere al menos 24 GB de RAM + memoria virtual libres. Cierra aplicaciones o amplia el archivo de paginacion antes de generar.";
        return new Report(ready, freePhysical, freeSwap, committable, disk, message);
    }

    public record Report(boolean ready, long freePhysicalBytes, long freeSwapBytes,
                         long committableBytes, long usableDiskBytes, String userMessage) {
        public Report {
            userMessage = userMessage == null ? "" : userMessage.strip();
        }

        public String diagnostic() {
            return "freePhysicalGB=" + gb(freePhysicalBytes)
                    + "\nfreeSwapGB=" + gb(freeSwapBytes)
                    + "\ncommittableGB=" + gb(committableBytes)
                    + "\nusableDiskGB=" + gb(usableDiskBytes);
        }

        private static String gb(long bytes) {
            return String.format(java.util.Locale.ROOT, "%.2f", bytes / 1024.0 / 1024.0 / 1024.0);
        }
    }
}
