package com.marcosmoreiradev.docupodcaststudio.infrastructure.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.BundleArtifactMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.export.BuildVoiceReferenceSamplesExportReportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportPodcastWavUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.InspectExportReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.export.VoiceReferenceSamplesExportReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExporter;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/** Filesystem exporter for a portable and auditable DocuPodcast project bundle. */
public final class FileSystemProjectBundleExporter implements ProjectBundleExporter {
    @Override
    public ProjectBundleExportResult export(ProjectBundleExportRequest request) throws IOException {
        Path root = request.targetDirectory().toAbsolutePath().normalize()
                .resolve(ExportFolderNamePolicy.folderName(request.project().metadata().title()));
        Path input = root.resolve("input");
        Path editable = root.resolve("editable");
        Path output = root.resolve("output");
        Path assets = root.resolve("assets");
        Path jobs = root.resolve("jobs");
        Path reports = root.resolve("reports");
        Files.createDirectories(input);
        Files.createDirectories(editable);
        Files.createDirectories(output);
        Files.createDirectories(assets);
        Files.createDirectories(jobs);
        Files.createDirectories(reports);

        int copiedInputs = copyInputAssets(request, input);
        copyEditableProject(request, editable);
        copyEditableDirectories(request, editable.resolve("workspace"));
        int copiedAssets = copyRegisteredAssets(request, assets.resolve("registered"));
        int copiedOutputs = copyOutputs(request, output);
        int copiedJobs = copyJobs(request, jobs);

        Path readme = root.resolve("README_EXPORTACION.md");
        Path manifest = root.resolve("MANIFEST_EXPORTACION.md");
        Path index = reports.resolve("BUNDLE_FILE_INDEX.tsv");
        Path readinessFile = reports.resolve("EXPORT_READINESS.md");
        Path voiceSamplesReportFile = reports.resolve("VOICE_REFERENCE_SAMPLES.md");
        Path voiceSamplesIndexFile = reports.resolve("VOICE_REFERENCE_SAMPLES.tsv");
        ExportReadinessReport readiness = new InspectExportReadinessUseCase()
                .inspect(request.project(), request.projectFile(), request.script(), request.storyboard(), request.audioJobs());
        VoiceReferenceSamplesExportReport voiceSamplesReport = new BuildVoiceReferenceSamplesExportReportUseCase()
                .build(request.project(), request.projectFile());
        Files.writeString(readme, readme(request), StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("README_EXPORTACION.md"), readme(request), StandardCharsets.UTF_8);
        Files.writeString(readinessFile, readiness.toMarkdown(), StandardCharsets.UTF_8);
        Files.writeString(voiceSamplesReportFile, voiceSamplesReport.markdown(), StandardCharsets.UTF_8);
        Files.writeString(voiceSamplesIndexFile, voiceSamplesReport.tsv(), StandardCharsets.UTF_8);

        List<BundleArtifactMetadata> artifactsBeforeManifest = collectArtifacts(root, List.of(manifest, index));
        Files.writeString(index, fileIndex(artifactsBeforeManifest), StandardCharsets.UTF_8);
        List<BundleArtifactMetadata> artifactsBeforeFinalManifest = collectArtifacts(root, List.of(manifest));
        Files.writeString(manifest, manifest(request, copiedInputs, copiedOutputs, copiedJobs, copiedAssets, readiness,
                voiceSamplesReport, artifactsBeforeFinalManifest), StandardCharsets.UTF_8);
        Files.writeString(reports.resolve("MANIFEST_EXPORTACION.md"), Files.readString(manifest, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        List<BundleArtifactMetadata> finalArtifacts = collectArtifacts(root, List.of());

        return new ProjectBundleExportResult(root, manifest, readme, index, readinessFile, voiceSamplesReportFile,
                voiceSamplesIndexFile, copiedInputs, copiedOutputs, copiedJobs, copiedAssets,
                voiceSamplesReport.sampleCount(), finalArtifacts);
    }

    private int copyInputAssets(ProjectBundleExportRequest request, Path input) throws IOException {
        int copied = 0;
        for (ProjectAssetReference asset : request.project().assets().references()) {
            if (asset.kind() == ProjectAssetKind.SOURCE_DOCUMENT) {
                Path source = safeResolve(request.projectDirectory(), asset.relativePath());
                if (Files.exists(source) && Files.isRegularFile(source)) {
                    Files.copy(source, input.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    copied++;
                }
            }
        }
        if (copied == 0) {
            Files.writeString(input.resolve("README_INPUT.md"), "No se encontró documento fuente copiable en el catálogo de assets.\n", StandardCharsets.UTF_8);
        }
        return copied;
    }

    private void copyEditableProject(ProjectBundleExportRequest request, Path editable) throws IOException {
        Files.copy(request.projectFile(), editable.resolve(request.projectFile().getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyEditableDirectories(ProjectBundleExportRequest request, Path target) throws IOException {
        Files.createDirectories(target);
        for (String name : List.of("document", "script", "voices", "storyboard", "media", "recordings", "study")) {
            Path source = request.projectDirectory().resolve(name);
            if (Files.exists(source)) {
                copyRecursively(source, target.resolve(name));
            }
        }
    }

    private int copyRegisteredAssets(ProjectBundleExportRequest request, Path target) throws IOException {
        Files.createDirectories(target);
        int copied = 0;
        for (ProjectAssetReference asset : request.project().assets().references()) {
            Path source = safeResolve(request.projectDirectory(), asset.relativePath());
            if (!Files.exists(source) || !Files.isRegularFile(source)) {
                continue;
            }
            Path destination = target.resolve(asset.kind().name().toLowerCase(Locale.ROOT)).resolve(asset.relativePath()).normalize();
            if (!destination.startsWith(target.toAbsolutePath().normalize())) {
                throw new IOException("Destino de asset fuera del paquete: " + asset.relativePath());
            }
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            copied++;
        }
        if (copied == 0) {
            Files.writeString(target.resolve("README_ASSETS.md"), "No hay assets registrados copiables.\n", StandardCharsets.UTF_8);
        }
        return copied;
    }

    private int copyOutputs(ProjectBundleExportRequest request, Path output) throws IOException {
        int copied = 0;
        if (request.script() != null && !request.script().empty()) {
            Files.writeString(output.resolve("lectura_preparada.md"), preparedReadingMarkdown(request), StandardCharsets.UTF_8);
            copied++;
        }
        ExportPodcastWavUseCase podcastExporter = new ExportPodcastWavUseCase();
        if (podcastExporter.canExport(request.audioJobs())) {
            podcastExporter.exportLatest(request.audioJobs(), request.projectDirectory(), output.resolve("podcast.wav"));
            copied++;
        }
        if (request.storyboard() != null) {
            Files.writeString(output.resolve("resumen_visual.md"), storyboardSummary(request), StandardCharsets.UTF_8);
            copied++;
        }
        if (copied == 0) {
            Files.writeString(output.resolve("README_OUTPUT.md"), "No hay salidas exportables todavía.\n", StandardCharsets.UTF_8);
        }
        return copied;
    }

    private int copyJobs(ProjectBundleExportRequest request, Path jobsTarget) throws IOException {
        Path jobsSource = request.projectDirectory().resolve("jobs");
        if (!Files.exists(jobsSource)) {
            Files.writeString(jobsTarget.resolve("README_JOBS.md"), "No hay carpeta jobs/ persistida.\n", StandardCharsets.UTF_8);
            return 0;
        }
        copyRecursively(jobsSource, jobsTarget.resolve("jobs"));
        return request.audioJobs().size();
    }


    private String readme(ProjectBundleExportRequest request) {
        return "# Exportación DocuPodcast\n\n"
                + "Este paquete contiene entrada, proyecto editable, salidas, assets, jobs y reportes auditables.\n\n"
                + "- Proyecto: " + request.project().metadata().title() + "\n"
                + "- Fecha UTC: " + Instant.now() + "\n\n"
                + "Carpetas principales:\n\n"
                + "- `input/`: Word/DOCX u otra fuente original cuando esté disponible.\n"
                + "- `editable/`: `.docupodcast.json` y snapshots editables/materializados.\n"
                + "- `output/`: lectura preparada, audio final y resúmenes exportados.\n"
                + "- `assets/`: copia auditable de assets registrados por tipo.\n"
                + "- `jobs/`: jobs persistidos para auditoría o recuperación.\n"
                + "- `reports/`: índice de archivos y copia del manifiesto.\n\n"
                + "Archivos raíz:\n\n"
                + "- `MANIFEST_EXPORTACION.md`: resumen del paquete y SHA-256 de artefactos.\n"
                + "- `reports/BUNDLE_FILE_INDEX.tsv`: índice tabular de archivos, tamaños y SHA-256.\n"
                + "- `reports/EXPORT_READINESS.md`: matriz de salidas exportables, faltantes y limitaciones honestas.\n"
                + "- `reports/VOICE_REFERENCE_SAMPLES.md`: matriz auditable de muestras de voz por tono.\n";
    }

    private String manifest(ProjectBundleExportRequest request, int copiedInputs, int copiedOutputs, int copiedJobs,
                            int copiedAssets, ExportReadinessReport readiness,
                            VoiceReferenceSamplesExportReport voiceSamplesReport,
                            List<BundleArtifactMetadata> artifacts) {
        StringBuilder out = new StringBuilder();
        out.append("# Manifiesto de exportación\n\n");
        out.append("- Proyecto: ").append(request.project().metadata().title()).append("\n");
        out.append("- Tipo: ").append(request.project().metadata().mode().displayName()).append("\n");
        out.append("- Estado tecnico: ").append(request.project().metadata().kind().displayName()).append("\n");
        out.append("- Estado: ").append(request.project().metadata().status().displayName()).append("\n");
        out.append("- Fecha UTC: ").append(Instant.now()).append("\n");
        out.append("- Assets registrados: ").append(request.project().assets().size()).append("\n");
        out.append("- Inputs copiados: ").append(copiedInputs).append("\n");
        out.append("- Outputs generados/copiados: ").append(copiedOutputs).append("\n");
        out.append("- Jobs referenciados: ").append(copiedJobs).append("\n");
        out.append("- Assets copiados: ").append(copiedAssets).append("\n");
        out.append("- Muestras de voz por tono: ").append(voiceSamplesReport.sampleCount()).append("\n");
        out.append("- Muestras con asset registrado: ").append(voiceSamplesReport.registeredAssetCount()).append("\n");
        out.append("- Muestras con archivo faltante: ").append(voiceSamplesReport.missingFileCount()).append("\n");
        out.append("- Pruebas de voz generadas: ").append(voiceSamplesReport.generatedTestFileCount()).append("\n");
        out.append("- Archivos auditados: ").append(artifacts.size()).append("\n");
        out.append("- Estado exportaciones: ").append(readiness.status().displayName()).append("\n");
        out.append("- Salidas exportables: ").append(readiness.exportableCount()).append("\n");
        out.append("- Salidas bloqueadas: ").append(readiness.blockedCount()).append("\n\n");
        out.append("## Preparación de exportaciones\n\n");
        out.append("Ver `reports/EXPORT_READINESS.md` para salidas exportables, faltantes y limitaciones honestas.\n\n");
        out.append("| Salida | Estado |\n");
        out.append("|---|---|\n");
        for (var item : readiness.items()) {
            out.append("| ").append(item.kind().displayName()).append(" | ").append(item.status().displayName()).append(" |\n");
        }
        out.append("\n## Lectura preparada\n\n");
        out.append(request.script() == null ? "Sin lectura preparada cargada.\n" : "Segmentos: " + request.script().segmentCount() + " · palabras: " + request.script().wordCount() + "\n");
        out.append("\n## Visuales\n\n");
        out.append(request.storyboard() == null ? "Sin visuales cargados.\n" : "Asociaciones: " + request.storyboard().bindingCount() + "\n");
        out.append("\n## Voces y muestras\n\n");
        out.append("Ver `reports/VOICE_REFERENCE_SAMPLES.md` para el detalle por tono, rutas y disponibilidad de archivos.\n\n");
        out.append("- Voces: ").append(voiceSamplesReport.voiceCount()).append("\n");
        out.append("- Sets de muestras por tono: ").append(voiceSamplesReport.sampleSetCount()).append("\n");
        out.append("- Muestras registradas: ").append(voiceSamplesReport.sampleCount()).append("\n");
        out.append("- Muestras faltantes: ").append(voiceSamplesReport.missingFileCount()).append("\n");
        out.append("\n## Jobs de audio\n\n");
        if (request.audioJobs().isEmpty()) {
            out.append("Sin jobs persistidos.\n");
        } else {
            for (AudioJobSnapshot job : request.audioJobs()) {
                out.append("- ").append(job.jobId()).append(" · ").append(job.state().displayName())
                        .append(" · ").append(job.completedSegments()).append("/").append(job.totalSegments())
                        .append(" · ").append(job.recoveryLabel()).append("\n");
            }
        }
        out.append("\n## Índice SHA-256\n\n");
        out.append("| Archivo | Bytes | SHA-256 |\n");
        out.append("|---|---:|---|\n");
        for (BundleArtifactMetadata artifact : artifacts) {
            out.append("| `").append(artifact.relativePath().toString().replace('\\', '/')).append("` | ")
                    .append(artifact.sizeBytes()).append(" | `").append(artifact.sha256()).append("` |\n");
        }
        return out.toString();
    }

    private String fileIndex(List<BundleArtifactMetadata> artifacts) {
        StringBuilder out = new StringBuilder("relative_path\tsize_bytes\tsha256\n");
        for (BundleArtifactMetadata artifact : artifacts) {
            out.append(artifact.relativePath().toString().replace('\\', '/'))
                    .append('\t').append(artifact.sizeBytes())
                    .append('\t').append(artifact.sha256()).append('\n');
        }
        return out.toString();
    }

    private String preparedReadingMarkdown(ProjectBundleExportRequest request) {
        StringBuilder out = new StringBuilder("# ").append(request.script().title()).append("\n\n");
        for (NarrationSegment segment : request.script().segments()) {
            out.append("## ").append(segment.id()).append(" — ").append(segment.title().isBlank() ? segment.type().displayName() : segment.title()).append("\n\n");
            out.append(segment.narrationText()).append("\n\n");
        }
        return out.toString();
    }

    private String storyboardSummary(ProjectBundleExportRequest request) {
        return "# Secuencia visual\n\nAsociaciones de imagen: " + request.storyboard().bindingCount() + "\n";
    }

    private static List<BundleArtifactMetadata> collectArtifacts(Path root, List<Path> excluded) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        List<Path> normalizedExcluded = excluded.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .toList();
        try (var stream = Files.walk(normalizedRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .filter(path -> !normalizedExcluded.contains(path))
                    .sorted()
                    .map(path -> toMetadata(normalizedRoot, path))
                    .toList();
        }
    }

    private static BundleArtifactMetadata toMetadata(Path root, Path file) {
        try {
            return new BundleArtifactMetadata(root.relativize(file), Files.size(file), sha256(file));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo auditar archivo: " + file, ex);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream input = Files.newInputStream(file)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 no disponible", ex);
        }
    }

    private static Path safeResolve(Path root, String relativePath) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Ruta fuera del proyecto: " + relativePath);
        }
        return resolved;
    }

    private static void copyRecursively(Path source, Path target) throws IOException {
        if (Files.isRegularFile(source)) {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (var stream = Files.walk(source)) {
            for (Path item : stream.toList()) {
                Path relative = source.relativize(item);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(item)) {
                    Files.createDirectories(destination);
                } else {
                    if (destination.getParent() != null) {
                        Files.createDirectories(destination.getParent());
                    }
                    Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
