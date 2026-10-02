package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;

/** Evidence collected locally before a managed dependency may use the network. */
public record ManagedDownloadPreflight(
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
    public ManagedDownloadPreflight {
        resourceId = resourceId == null ? "" : resourceId.strip();
        state = state == null ? ManagedDownloadState.MISSING : state;
        location = location == null ? null : location.toAbsolutePath().normalize();
        expectedBytes = Math.max(0L, expectedBytes);
        actualBytes = Math.max(0L, actualBytes);
        expectedSha256 = expectedSha256 == null ? "" : expectedSha256.strip();
        actualSha256 = actualSha256 == null ? "" : actualSha256.strip();
        license = license == null ? "" : license.strip();
        source = source == null ? "" : source.strip();
        diagnosis = diagnosis == null ? "" : diagnosis.strip();
    }

    public boolean valid() {
        return state == ManagedDownloadState.VALID;
    }

    public String technicalDetails() {
        return String.join(System.lineSeparator(),
                "Recurso: " + resourceId,
                "Estado: " + state,
                "Ubicación: " + (location == null ? "" : location),
                "Tamaño local: " + actualBytes + " bytes",
                "Tamaño esperado: " + expectedBytes + " bytes",
                "SHA-256 local: " + actualSha256,
                "SHA-256 esperado: " + expectedSha256,
                "Licencia: " + license,
                "Fuente: " + source,
                "Diagnóstico: " + diagnosis);
    }
}
