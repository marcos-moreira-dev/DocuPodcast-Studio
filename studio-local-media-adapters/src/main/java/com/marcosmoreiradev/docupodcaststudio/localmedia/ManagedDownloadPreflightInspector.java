package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadPreflight;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HexFormat;

/** Strict local-only inspection shared by managed adapter downloads. */
final class ManagedDownloadPreflightInspector {
    private static final int MAX_CACHED_HASHES = 128;
    private static final ConcurrentHashMap<FileFingerprint, String> SHA256_CACHE =
            new ConcurrentHashMap<>();

    private ManagedDownloadPreflightInspector() {
    }

    static ManagedDownloadPreflight inspect(
            String resourceId,
            Path target,
            Path partial,
            long expectedBytes,
            String expectedSha256,
            String license,
            String source) throws IOException {
        Path location = target.toAbsolutePath().normalize();
        if (Files.isRegularFile(location)) {
            long actualBytes = Files.size(location);
            String actualSha = sha256(location);
            boolean valid = actualBytes == expectedBytes && expectedSha256.equalsIgnoreCase(actualSha);
            return result(resourceId, valid ? ManagedDownloadState.VALID : ManagedDownloadState.INVALID,
                    location, expectedBytes, actualBytes, expectedSha256, actualSha, license, source,
                    valid ? "El recurso instalado coincide con el tamaño y SHA-256 esperados."
                            : "El archivo local no coincide con el tamaño o SHA-256 esperado.");
        }
        if (Files.exists(location)) {
            return result(resourceId, ManagedDownloadState.INVALID, location, expectedBytes,
                    0L, expectedSha256, "", license, source,
                    "La ubicación administrada no contiene un archivo regular.");
        }
        if (partial != null && Files.isRegularFile(partial)) {
            return result(resourceId, ManagedDownloadState.PARTIAL, location, expectedBytes,
                    Files.size(partial), expectedSha256, sha256(partial), license, source,
                    "Hay una descarga incompleta en staging; el recurso publicado sigue ausente.");
        }
        return result(resourceId, ManagedDownloadState.MISSING, location, expectedBytes,
                0L, expectedSha256, "", license, source,
                "El recurso todavía no está instalado.");
    }

    static String sha256(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        BasicFileAttributes attributes = Files.readAttributes(normalized, BasicFileAttributes.class);
        FileFingerprint fingerprint = new FileFingerprint(normalized, attributes.size(),
                attributes.lastModifiedTime().toMillis(), String.valueOf(attributes.fileKey()));
        String cached = SHA256_CACHE.get(fingerprint);
        if (cached != null) return cached;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(normalized)) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            if (SHA256_CACHE.size() >= MAX_CACHED_HASHES) SHA256_CACHE.clear();
            SHA256_CACHE.put(fingerprint, hash);
            return hash;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 no está disponible.", impossible);
        }
    }

    private record FileFingerprint(Path path, long size, long lastModifiedMillis, String fileKey) {
    }

    private static ManagedDownloadPreflight result(
            String resourceId,
            ManagedDownloadState state,
            Path location,
            long expectedBytes,
            long actualBytes,
            String expectedSha256,
            String actualSha256,
            String license,
            String source,
            String diagnosis) {
        return new ManagedDownloadPreflight(resourceId, state, location, expectedBytes, actualBytes,
                expectedSha256, actualSha256, license, source, diagnosis);
    }
}
