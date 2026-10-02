package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeRoots;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Resolves project-managed and app-bundled voice reference samples. */
public final class VoiceReferenceSamplePathResolver {
    private final RuntimeArtifactPaths installationPaths;
    private final RuntimeArtifactPaths runtimePaths;

    public VoiceReferenceSamplePathResolver(RuntimeArtifactPaths runtimePaths) {
        this(runtimePaths, runtimePaths);
    }

    public VoiceReferenceSamplePathResolver(Path installationRoot, Path runtimeRoot) {
        this(RuntimeArtifactPaths.fromRoot(installationRoot), RuntimeArtifactPaths.fromRoot(runtimeRoot));
    }

    public VoiceReferenceSamplePathResolver(RuntimeArtifactPaths installationPaths,
                                            RuntimeArtifactPaths runtimePaths) {
        this.installationPaths = Objects.requireNonNull(installationPaths, "installationPaths");
        this.runtimePaths = Objects.requireNonNull(runtimePaths, "runtimePaths");
    }

    /** @deprecated use injected roots; retained for legacy entry points without a composition context. */
    @Deprecated(forRemoval = false)
    public static VoiceReferenceSamplePathResolver fromCurrentApplicationRoot() {
        ApplicationRuntimeRoots roots = ApplicationRuntimeRoots.configured();
        return new VoiceReferenceSamplePathResolver(roots.installationRoot(), roots.runtimeRoot());
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
            Path resolved = installationPaths.resolveBundled(uri);
            if (!resolved.startsWith(installationPaths.applicationRoot())) {
                throw new IOException("La " + label + " embebida apunta fuera de la carpeta de la app: " + uri);
            }
            return requireRegularFile(resolved, uri);
        }
        if (managedLibrary(sample)) {
            Path resolved = runtimePaths.resolveBundled(uri);
            if (!resolved.startsWith(runtimePaths.applicationRoot())) {
                throw new IOException("La " + label + " administrada apunta fuera del runtime: " + uri);
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

    /** Resolves the legacy advanced narrator sample from installation or runtime layout. */
    public Optional<Path> resolveDefaultAdvancedReference() {
        Path installed = installationPaths.xttsDefaultSpeakerWav();
        if (Files.isRegularFile(installed)) {
            return Optional.of(installed);
        }
        Path runtime = runtimePaths.xttsDefaultSpeakerWav();
        return Files.isRegularFile(runtime) ? Optional.of(runtime) : Optional.empty();
    }

    public static boolean appBundled(VoiceReferenceSample sample) {
        return sample != null && (sample.origin() == VoiceSampleOrigin.APP_DEFAULT
                || sample.ownership() == VoiceFileOwnership.APP_RESOURCE);
    }

    private static boolean managedLibrary(VoiceReferenceSample sample) {
        return sample != null && sample.ownership() == VoiceFileOwnership.USER_APPDATA;
    }

    private static Path requireRegularFile(Path resolved, String originalUri) throws IOException {
        if (!Files.isRegularFile(resolved)) {
            throw new IOException("La muestra de voz registrada no existe: " + resolved + " (" + originalUri + ")");
        }
        return resolved;
    }
}
