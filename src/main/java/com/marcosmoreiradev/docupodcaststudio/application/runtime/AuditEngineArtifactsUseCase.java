package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Audits local engine files, licenses and checksums from the resolved runtime/app-image root. */
public final class AuditEngineArtifactsUseCase {
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final BuildEngineArtifactManifestUseCase manifestUseCase;

    public AuditEngineArtifactsUseCase() {
        this(new BuildEngineArtifactManifestUseCase());
    }

    public AuditEngineArtifactsUseCase(BuildEngineArtifactManifestUseCase manifestUseCase) {
        this.manifestUseCase = Objects.requireNonNull(manifestUseCase, "manifestUseCase");
    }

    public EngineArtifactAuditReport audit(ApplicationRuntimeLayout layout) {
        return audit(layout, message -> { });
    }

    public EngineArtifactAuditReport audit(ApplicationRuntimeLayout layout, Consumer<String> progress) {
        ApplicationRuntimeLayout runtimeLayout = layout == null
                ? new ApplicationRuntimeLayout(Path.of("."))
                : layout;
        Consumer<String> listener = progress == null ? message -> { } : progress;
        EngineArtifactManifest manifest = manifestUseCase.execute();
        ArrayList<EngineArtifactFileStatus> statuses = new ArrayList<>();
        listener.accept("Auditando artefactos de motores en " + runtimeLayout.applicationRoot() + ".");
        for (EngineArtifactDescriptor descriptor : manifest.artifacts()) {
            listener.accept("Revisando " + descriptor.displayName() + "...");
            List<Path> matches = findMatches(runtimeLayout.applicationRoot(), descriptor.expectedPath());
            ArrayList<String> hashes = new ArrayList<>();
            for (Path match : matches) {
                if (Files.isRegularFile(match)) {
                    listener.accept("Calculando SHA-256 de " + runtimeLayout.applicationRoot().relativize(match).toString() + "...");
                    hashes.add(sha256(match));
                }
            }
            boolean present = !matches.isEmpty();
            statuses.add(new EngineArtifactFileStatus(descriptor, present, matches, hashes, actionFor(descriptor, present)));
        }
        EngineArtifactAuditReport report = new EngineArtifactAuditReport(runtimeLayout.applicationRoot(), statuses);
        listener.accept(report.compactSummary());
        return report;
    }

    public Path writeMarkdown(EngineArtifactAuditReport report, Path outputPath) throws IOException {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(outputPath, "outputPath");
        Path target = outputPath.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.writeString(target, report.toMarkdown(), StandardCharsets.UTF_8);
        return target;
    }

    private static List<Path> findMatches(Path root, String expectedPath) {
        Path applicationRoot = root == null ? Path.of(".").toAbsolutePath().normalize() : root.toAbsolutePath().normalize();
        String normalized = expectedPath == null ? "" : expectedPath.replace('\\', '/').trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        if (!normalized.contains("*") && !normalized.contains("|")) {
            Path candidate = applicationRoot.resolve(normalized).normalize();
            return Files.exists(candidate) ? List.of(candidate) : List.of();
        }
        int slash = normalized.lastIndexOf('/');
        String directoryPart = slash >= 0 ? normalized.substring(0, slash) : "";
        String filePatternPart = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        Path directory = directoryPart.isBlank() ? applicationRoot : applicationRoot.resolve(directoryPart).normalize();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        String[] alternatives = filePatternPart.split("\\|");
        ArrayList<Path> matches = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            stream.filter(path -> matchesAny(path.getFileName().toString(), alternatives))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(matches::add);
        } catch (IOException ex) {
            return List.of();
        }
        return List.copyOf(matches);
    }

    private static boolean matchesAny(String fileName, String[] alternatives) {
        for (String raw : alternatives) {
            String pattern = raw == null ? "" : raw.trim();
            if (pattern.isBlank()) {
                continue;
            }
            if (pattern.equals(fileName)) {
                return true;
            }
            if (pattern.startsWith("*") && fileName.endsWith(pattern.substring(1))) {
                return true;
            }
            if (pattern.endsWith("*") && fileName.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private static String actionFor(EngineArtifactDescriptor descriptor, boolean present) {
        if (present) {
            if (descriptor.hasConcreteSha256()) {
                return "Presente; comparar con SHA-256 fijado en el manifiesto.";
            }
            return "Presente; fijar SHA-256 y licencia antes de RC final.";
        }
        if (descriptor.userProvidedAllowed()) {
            return "Importar desde Configuración o colocar archivo autorizado en la ruta esperada.";
        }
        if (descriptor.redistributedByDefault()) {
            return "Preparar automáticamente desde Configuración o reparar el runtime portable.";
        }
        return "Opcional; solo necesario si se habilita este motor.";
    }

    private static String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file); DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (digestInput.read(buffer) >= 0) {
                    // DigestInputStream updates the digest.
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (IOException | NoSuchAlgorithmException ex) {
            return "ERROR_SHA256:" + ex.getMessage();
        }
    }
}
