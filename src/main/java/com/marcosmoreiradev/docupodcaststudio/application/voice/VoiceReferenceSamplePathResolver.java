package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves project-managed and app-bundled voice reference samples. */
public final class VoiceReferenceSamplePathResolver {
    private final RuntimeArtifactPaths runtimePaths;

    public VoiceReferenceSamplePathResolver(RuntimeArtifactPaths runtimePaths) {
        this.runtimePaths = Objects.requireNonNull(runtimePaths, "runtimePaths");
    }

    public static VoiceReferenceSamplePathResolver fromCurrentApplicationRoot() {
        return new VoiceReferenceSamplePathResolver(RuntimeArtifactPaths.fromRoot(Path.of("").toAbsolutePath().normalize()));
    }

    public Path resolve(Path projectRoot, VoiceReferenceSample sample, String contextLabel) throws IOException {
        Objects.requireNonNull(sample, "sample");
        String label = contextLabel == null || contextLabel.isBlank() ? "muestra de voz" : contextLabel.strip();
        String uri = sample.fileUri();
        Path raw = Path.of(uri);
        if (raw.isAbsolute()) {
            return requireRegularFile(raw.normalize(), uri);
        }
        if (appBundled(sample)) {
            Path resolved = runtimePaths.resolveBundled(uri);
            if (!resolved.startsWith(runtimePaths.applicationRoot())) {
                throw new IOException("La " + label + " embebida apunta fuera de la carpeta de la app: " + uri);
            }
            return requireRegularFile(resolved, uri);
        }
        Path root = Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize();
        Path resolved = root.resolve(raw).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("La " + label + " apunta fuera de la carpeta del proyecto: " + uri);
        }
        return requireRegularFile(resolved, uri);
    }

    public static boolean appBundled(VoiceReferenceSample sample) {
        return sample != null && (sample.origin() == VoiceSampleOrigin.APP_DEFAULT
                || sample.ownership() == VoiceFileOwnership.APP_RESOURCE);
    }

    private static Path requireRegularFile(Path resolved, String originalUri) throws IOException {
        if (!Files.isRegularFile(resolved)) {
            throw new IOException("La muestra de voz registrada no existe: " + resolved + " (" + originalUri + ")");
        }
        return resolved;
    }
}
