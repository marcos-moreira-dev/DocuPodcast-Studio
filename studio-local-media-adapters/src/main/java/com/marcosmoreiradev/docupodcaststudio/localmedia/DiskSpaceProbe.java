package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Injectable disk-space boundary used before large managed downloads. */
@FunctionalInterface
interface DiskSpaceProbe {
    long usableBytes(Path target) throws IOException;

    static DiskSpaceProbe system() {
        return target -> {
            Path existing = target.toAbsolutePath().normalize();
            while (existing != null && !Files.exists(existing)) {
                existing = existing.getParent();
            }
            if (existing == null) throw new IOException("No existe una raíz para comprobar espacio.");
            return Files.getFileStore(existing).getUsableSpace();
        };
    }
}
