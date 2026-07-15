package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Temporary storage for image-engine smoke PNGs; project images must not use this store. */
public final class ImageEngineSmokeImageStore {
    private static final Path SESSION_ROOT = Path.of(System.getProperty("java.io.tmpdir"))
            .resolve("docupodcast-studio")
            .resolve("image-smoke-" + ProcessHandle.current().pid() + "-" + System.currentTimeMillis())
            .toAbsolutePath()
            .normalize();
    private static final Set<Path> REGISTERED_ROOTS = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(
                ImageEngineSmokeImageStore::cleanupAll,
                "docupodcast-image-smoke-cleanup"));
    }

    private ImageEngineSmokeImageStore() {
    }

    public static Path outputDirectory() {
        Path directory = SESSION_ROOT.resolve("png").normalize();
        register(directory);
        try {
            Files.createDirectories(directory);
        } catch (IOException ignored) {
            // The caller will surface the write failure when the smoke test tries to save the PNG.
        }
        directory.toFile().deleteOnExit();
        SESSION_ROOT.toFile().deleteOnExit();
        return directory;
    }

    public static void register(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(SESSION_ROOT)) {
            REGISTERED_ROOTS.add(normalized);
        }
    }

    public static void cleanupAll() {
        REGISTERED_ROOTS.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(ImageEngineSmokeImageStore::deleteRecursively);
        REGISTERED_ROOTS.clear();
        deleteRecursively(SESSION_ROOT);
    }

    private static void deleteRecursively(Path path) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(SESSION_ROOT) || !Files.exists(normalized)) {
            return;
        }
        try (var stream = Files.walk(normalized)) {
            stream.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // Best-effort cleanup; locked preview files are retried by deleteOnExit.
                    candidate.toFile().deleteOnExit();
                }
            });
        } catch (IOException ignored) {
            normalized.toFile().deleteOnExit();
        }
    }
}
