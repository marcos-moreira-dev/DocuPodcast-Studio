package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemSourceDraft;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfExporter;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.StudyProblemPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudyProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.study.StudySourceReference;
import com.marcosmoreiradev.docupodcaststudio.domain.study.TechnicalProblem;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import com.marcosmoreiradev.docupodcaststudio.ink.canvas.InkImageFileStore;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Saves project-side study problems and optional canvas exports. */
public final class StudyProblemWorkflow {
    private final StudyProblemPdfExporter pdfExporter;

    public StudyProblemWorkflow() {
        this(StudyProblemPdfExporter.unavailable());
    }

    public StudyProblemWorkflow(StudyProblemPdfExporter pdfExporter) {
        this.pdfExporter = Objects.requireNonNull(pdfExporter, "pdfExporter");
    }

    public TechnicalProblem save(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                 List<DocumentBlock> sourceBlocks, String requestedTitle,
                                 String solutionText, WritableImage solutionImage, Map<String, Path> sourceCropPaths) throws IOException {
        return save(session, projectDirectory, sourceBlocks, requestedTitle, solutionText, solutionImage, sourceCropPaths, null);
    }

    public TechnicalProblem save(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                 List<DocumentBlock> sourceBlocks, String requestedTitle,
                                 String solutionText, WritableImage solutionImage, Map<String, Path> sourceCropPaths,
                                 String canvasStateJson) throws IOException {
        List<DocumentBlock> blocks = sourceBlocks == null ? List.of() : sourceBlocks.stream()
                .filter(block -> block != null && !block.text().isBlank())
                .toList();
        if (blocks.isEmpty()) {
            throw new IOException("Selecciona al menos un bloque del documento para crear el problema.");
        }
        Map<String, Path> crops = sourceCropPaths == null ? Map.of() : sourceCropPaths;
        return saveFromSources(session, projectDirectory, blocks.stream()
                        .map(block -> StudyProblemSourceDraft.fromBlock(block, crops.get(block.id())))
                        .toList(),
                requestedTitle, solutionText, solutionImage, canvasStateJson);
    }

    public TechnicalProblem saveFromSources(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                            List<StudyProblemSourceDraft> sourceDrafts, String requestedTitle,
                                            String solutionText, WritableImage solutionImage) throws IOException {
        return saveFromSources(session, projectDirectory, sourceDrafts, requestedTitle, solutionText, solutionImage, null);
    }

    public TechnicalProblem saveFromSources(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                            List<StudyProblemSourceDraft> sourceDrafts, String requestedTitle,
                                            String solutionText, WritableImage solutionImage,
                                            String canvasStateJson) throws IOException {
        List<StudyProblemSourceDraft> sources = sourceDrafts == null ? List.of() : sourceDrafts.stream()
                .filter(source -> source != null && source.hasUsableContent())
                .toList();
        if (sources.isEmpty()) {
            throw new IOException("Selecciona al menos una fuente del documento para crear el problema.");
        }
        DocuPodcastProject project = session.project();
        int nextIndex = project.study().technicalProblems().size() + 1;
        String problemId = "PROB-" + String.format(java.util.Locale.ROOT, "%03d", nextIndex);
        String title = requestedTitle == null || requestedTitle.isBlank() ? "Problema " + nextIndex : requestedTitle.strip();
        String solutionAssetId = "";
        DocuPodcastProject updatedProject = project;
        Map<String, String> cropAssetIds = new LinkedHashMap<>();
        if (sources.stream().anyMatch(StudyProblemSourceDraft::hasCrop)) {
            Path root = projectDirectory.orElseThrow(() -> new IOException("Guarda el proyecto antes de guardar crops fuente del problema."));
            int sourceIndex = 1;
            for (StudyProblemSourceDraft source : sources) {
                Path crop = source.sourceCropPath();
                if (crop == null || !Files.isRegularFile(crop)) {
                    continue;
                }
                boolean externalImage = source.externalImage();
                String cropAssetId = (externalImage ? "STUDY-IMAGE-" : "STUDY-CROP-")
                        + String.format(java.util.Locale.ROOT, "%03d-%02d", nextIndex, sourceIndex);
                Path relative = Path.of("study", "problems", problemId.toLowerCase(java.util.Locale.ROOT), "source", safeFileToken(source.sourceId()) + ".png");
                Path output = safeProjectPath(root, relative);
                Files.createDirectories(output.getParent());
                Files.copy(crop, output, StandardCopyOption.REPLACE_EXISTING);
                updatedProject = updatedProject.withAsset(new ProjectAssetReference(
                        cropAssetId,
                        externalImage ? ProjectAssetKind.STUDY_PROBLEM_IMAGE : ProjectAssetKind.STUDY_SOURCE_CROP,
                        title + " - fuente " + sourceIndex,
                        relative.toString().replace('\\', '/'), "image/png",
                        externalImage ? "Imagen externa asociada a problema tecnico" : "Recorte fiel del PDF fuente para problema tecnico",
                        "",
                        externalImage ? "Importada por el usuario como fuente visual del problema" : "Generado desde bbox PDF"));
                cropAssetIds.put(source.sourceId(), cropAssetId);
                sourceIndex++;
            }
        }
        if (solutionImage != null && solutionImage.getWidth() > 0 && solutionImage.getHeight() > 0) {
            Path root = projectDirectory.orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar el lienzo del problema."));
            Path relative = Path.of("study", "problems", problemId.toLowerCase(java.util.Locale.ROOT), "solution.png");
            Path output = safeProjectPath(root, relative);
            writePng(solutionImage, output);
            solutionAssetId = "STUDY-SOLUTION-" + String.format(java.util.Locale.ROOT, "%03d", nextIndex);
            updatedProject = updatedProject.withAsset(new ProjectAssetReference(
                    solutionAssetId, ProjectAssetKind.STUDY_SOLUTION_IMAGE, title + " - solucion",
                    relative.toString().replace('\\', '/'), "image/png",
                    "Solucion manuscrita de problema tecnico", "", "Exportado desde el lienzo de estudio documental"));
        }
        if (hasCanvasState(canvasStateJson)) {
            writeCanvasState(projectDirectory, problemId, canvasStateJson);
        }
        Instant now = Instant.now();
        TechnicalProblem problem = new TechnicalProblem(problemId, title, studySources(sources, cropAssetIds), problemText(sources),
                solutionText == null ? "" : solutionText, solutionAssetId, now, now, "");
        StudyProjectLayer study = updatedProject.study().withTechnicalProblem(problem);
        session.replaceProject(updatedProject.withStudy(study), true);
        return problem;
    }

    public TechnicalProblem updateSolution(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                           String problemId, String solutionText, WritableImage solutionImage,
                                           String notes) throws IOException {
        return updateSolution(session, projectDirectory, problemId, solutionText, solutionImage, notes, null);
    }

    public TechnicalProblem updateSolution(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                           String problemId, String solutionText, WritableImage solutionImage,
                                           String notes, String canvasStateJson) throws IOException {
        DocuPodcastProject project = session.project();
        TechnicalProblem current = findProblem(project, problemId);
        DocuPodcastProject updatedProject = project;
        String solutionAssetId = current.solutionImageAssetId();
        if (solutionImage != null && solutionImage.getWidth() > 0 && solutionImage.getHeight() > 0) {
            Path root = projectDirectory.orElseThrow(() -> new IOException("Guarda el proyecto antes de exportar el lienzo del problema."));
            SolutionAssetTarget target = solutionAssetTarget(updatedProject, current);
            Path output = safeProjectPath(root, target.relativePath());
            writePng(solutionImage, output);
            solutionAssetId = target.assetId();
            updatedProject = updatedProject.withAsset(new ProjectAssetReference(
                    solutionAssetId, ProjectAssetKind.STUDY_SOLUTION_IMAGE, current.title() + " - solucion",
                    target.relativePath().toString().replace('\\', '/'), "image/png",
                    "Solucion manuscrita de problema tecnico", "", "Exportado desde el lienzo de estudio documental"));
        }
        if (hasCanvasState(canvasStateJson)) {
            writeCanvasState(projectDirectory, current.id(), canvasStateJson);
        }
        TechnicalProblem updated = current.withUpdatedSolution(solutionText == null ? "" : solutionText, solutionAssetId, notes == null ? "" : notes);
        session.replaceProject(updatedProject.withStudy(updatedProject.study().withTechnicalProblem(updated)), true);
        return updated;
    }

    public void delete(ProjectSession session, java.util.Optional<Path> projectDirectory, String problemId) throws IOException {
        DocuPodcastProject project = session.project();
        TechnicalProblem problem = findProblem(project, problemId);
        DocuPodcastProject updated = project;
        for (String assetId : studyAssetIds(problem)) {
            Optional<ProjectAssetReference> asset = updated.assets().byId(assetId);
            if (asset.isPresent()) {
                deleteProjectAssetFileIfPresent(projectDirectory, asset.get());
                updated = updated.withoutAsset(assetId);
            }
        }
        deleteCanvasStateIfPresent(projectDirectory, problem.id());
        session.replaceProject(updated.withStudy(updated.study().withoutTechnicalProblem(problem.id())), true);
    }

    public Path exportSolutionImage(ProjectSession session, java.util.Optional<Path> projectDirectory, String problemId, Path target) throws IOException {
        TechnicalProblem problem = findProblem(session.project(), problemId);
        if (problem.solutionImageAssetId().isBlank()) {
            throw new IOException("El problema no tiene solucion de lienzo para exportar.");
        }
        ProjectAssetReference asset = session.project().assets().byId(problem.solutionImageAssetId())
                .orElseThrow(() -> new IOException("No se encontro el asset PNG de solucion."));
        Path source = resolveProjectAsset(projectDirectory, asset)
                .orElseThrow(() -> new IOException("Guarda el proyecto o revisa el archivo PNG de solucion antes de exportar."));
        Path output = requireTarget(target);
        Files.createDirectories(output.getParent());
        Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
        return output;
    }

    public Path exportSolutionImage(WritableImage image, Path target) throws IOException {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IOException("El lienzo no produjo una imagen valida para exportar.");
        }
        Path output = requireTarget(target);
        writePng(image, output);
        return output;
    }

    public BatchExportReport exportAllSolutionImages(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                                     Path targetDirectory) throws IOException {
        if (targetDirectory == null) {
            throw new IOException("Selecciona una carpeta destino.");
        }
        Path outputRoot = targetDirectory.toAbsolutePath().normalize();
        Files.createDirectories(outputRoot);
        java.util.List<String> manifest = new java.util.ArrayList<>();
        manifest.add("Exportacion masiva de problemas tecnicos");
        manifest.add("Fecha: " + Instant.now());
        manifest.add("");
        int exported = 0;
        int skipped = 0;
        int failed = 0;
        int index = 1;
        for (TechnicalProblem problem : session.project().study().technicalProblems()) {
            if (problem.solutionImageAssetId().isBlank()) {
                skipped++;
                manifest.add("OMITIDO " + problem.id() + " - sin PNG final");
                continue;
            }
            try {
                ProjectAssetReference asset = session.project().assets().byId(problem.solutionImageAssetId())
                        .orElseThrow(() -> new IOException("Asset de solucion no encontrado."));
                Path source = resolveProjectAsset(projectDirectory, asset)
                        .orElseThrow(() -> new IOException("No se pudo resolver el PNG del proyecto."));
                if (!Files.isRegularFile(source)) {
                    throw new IOException("Archivo PNG no existe: " + asset.relativePath());
                }
                String fileName = String.format(java.util.Locale.ROOT, "%03d-%s-%s.png",
                        index,
                        safeFileToken(problem.id()).toLowerCase(java.util.Locale.ROOT),
                        safeFileToken(problem.title()).toLowerCase(java.util.Locale.ROOT));
                Path target = outputRoot.resolve(fileName).normalize();
                if (!target.startsWith(outputRoot)) {
                    throw new IOException("Nombre de archivo invalido.");
                }
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                exported++;
                manifest.add("EXPORTADO " + problem.id() + " -> " + fileName);
            } catch (IOException ex) {
                failed++;
                manifest.add("ERROR " + problem.id() + " - " + ex.getMessage());
            }
            index++;
        }
        manifest.add("");
        manifest.add("Exportados: " + exported);
        manifest.add("Omitidos: " + skipped);
        manifest.add("Errores: " + failed);
        Path manifestPath = outputRoot.resolve("manifest.txt");
        Files.write(manifestPath, manifest, StandardCharsets.UTF_8);
        return new BatchExportReport(outputRoot, manifestPath, exported, skipped, failed);
    }

    public PdfExportReport exportAllSolutionImagesAsPdf(ProjectSession session, java.util.Optional<Path> projectDirectory,
                                                        Path targetPdf) throws IOException {
        Path output = withPdfExtension(requireTarget(targetPdf));
        Files.createDirectories(output.getParent());
        java.util.List<String> manifest = new java.util.ArrayList<>();
        manifest.add("Exportacion PDF de problemas tecnicos");
        manifest.add("Fecha: " + Instant.now());
        manifest.add("PDF: " + output.getFileName());
        manifest.add("");
        int exported = 0;
        int skipped = 0;
        int failed = 0;
        java.util.List<StudyProblemPdfPage> pages = new java.util.ArrayList<>();
        for (TechnicalProblem problem : session.project().study().technicalProblems()) {
            if (problem.solutionImageAssetId().isBlank()) {
                skipped++;
                manifest.add("OMITIDO " + problem.id() + " - sin PNG final");
                continue;
            }
            try {
                ProjectAssetReference asset = session.project().assets().byId(problem.solutionImageAssetId())
                        .orElseThrow(() -> new IOException("Asset de solucion no encontrado."));
                Path source = resolveProjectAsset(projectDirectory, asset)
                        .orElseThrow(() -> new IOException("No se pudo resolver el PNG del proyecto."));
                if (!Files.isRegularFile(source)) {
                    throw new IOException("Archivo PNG no existe: " + asset.relativePath());
                }
                Path editable = projectDirectory.isPresent() ? canvasStatePath(projectDirectory.get(), problem.id()) : null;
                if (editable != null && Files.isRegularFile(editable)) {
                    String json = Files.readString(editable);
                    var state = com.marcosmoreiradev.docupodcaststudio.ink.model.InkWorkspaceStateSerializer.fromJson(json);
                    int count = Integer.parseInt(state.metadata().getOrDefault("notebook.count", "1"));
                    int current = Integer.parseInt(state.metadata().getOrDefault("notebook.current", "0"));
                    if (count < 1 || count > 10000 || current < 0 || current >= count) throw new IOException("Cuaderno inválido");
                    java.util.List<StudyProblemPdfPage> notebook = new java.util.ArrayList<>();
                    for (int i=0; i<count; i++) {
                        String pageState = i == current ? json : state.metadata().get("notebook.page."+i);
                        if (pageState == null || pageState.isBlank()) throw new IOException("Falta el estado de la página " + (i+1));
                        notebook.add(new StudyProblemPdfPage(problem.title(), source, pageState));
                    }
                    pages.addAll(notebook);
                    exported += count;
                } else {
                    pages.add(new StudyProblemPdfPage(problem.title(), source));
                    exported++;
                }
                manifest.add("EXPORTADO " + problem.id() + " - " + problem.title());
            } catch (IOException | IllegalArgumentException ex) {
                failed++;
                manifest.add("ERROR " + problem.id() + " - " + ex.getMessage());
            }
        }
        if (exported == 0) {
            throw new IOException("No hay problemas con PNG final para exportar a PDF.");
        }
        pdfExporter.export(pages, output);
        manifest.add("");
        manifest.add("Exportados: " + exported);
        manifest.add("Omitidos: " + skipped);
        manifest.add("Errores: " + failed);
        Path manifestPath = output.resolveSibling(stripExtension(output.getFileName().toString()) + "-manifest.txt");
        Files.write(manifestPath, manifest, StandardCharsets.UTF_8);
        return new PdfExportReport(output, manifestPath, exported, skipped, failed);
    }

    public Path exportSolutionText(ProjectSession session, String problemId, Path target) throws IOException {
        TechnicalProblem problem = findProblem(session.project(), problemId);
        if (problem.solutionText().isBlank()) {
            throw new IOException("El problema no tiene solucion textual para exportar.");
        }
        Path output = requireTarget(target);
        Files.createDirectories(output.getParent());
        Files.writeString(output, problem.solutionText(), StandardCharsets.UTF_8);
        return output;
    }

    private static List<StudySourceReference> sources(List<DocumentBlock> blocks, Map<String, String> cropAssetIds) {
        return blocks.stream()
                .map(block -> StudySourceReference.fullBlock(block.id(), block.text(),
                        block.metadata().getOrDefault("sourcePage", ""),
                        block.metadata().getOrDefault("bbox", ""),
                        cropAssetIds.getOrDefault(block.id(), "")))
                .toList();
    }

    private static List<StudySourceReference> studySources(List<StudyProblemSourceDraft> sources, Map<String, String> cropAssetIds) {
        return sources.stream()
                .map(source -> source.visualRegion()
                        ? StudySourceReference.visualRegion(source.sourceId(), source.selectedText(), source.sourcePage(),
                        source.bbox(), cropAssetIds.getOrDefault(source.sourceId(), ""))
                        : StudySourceReference.fullBlock(source.sourceId(), source.selectedText(), source.sourcePage(),
                        source.bbox(), cropAssetIds.getOrDefault(source.sourceId(), "")))
                .toList();
    }

    private static String problemText(List<StudyProblemSourceDraft> sources) {
        return sources.stream()
                .map(StudyProblemSourceDraft::selectedText)
                .filter(text -> text != null && !text.isBlank())
                .collect(java.util.stream.Collectors.joining("\n\n"));
    }

    private static String safeFileToken(String value) {
        String normalized = value == null ? "" : value.strip()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "source" : normalized;
    }

    private static void writePng(WritableImage image, Path target) throws IOException {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        PixelReader reader = image.getPixelReader();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        InkImageFileStore.writePng(output, target);
    }

    private static Path safeProjectPath(Path root, Path relative) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path output = normalizedRoot.resolve(relative).normalize();
        if (!output.startsWith(normalizedRoot)) {
            throw new IOException("Ruta de asset de estudio fuera de la carpeta del proyecto.");
        }
        return output;
    }

    private static boolean hasCanvasState(String canvasStateJson) {
        return canvasStateJson != null && !canvasStateJson.isBlank();
    }

    private static void writeCanvasState(Optional<Path> projectDirectory, String problemId, String canvasStateJson) throws IOException {
        if (!hasCanvasState(canvasStateJson)) {
            return;
        }
        Path root = projectDirectory
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de guardar el estado editable del lienzo."));
        Path output = canvasStatePath(root, problemId);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, canvasStateJson, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void deleteCanvasStateIfPresent(Optional<Path> projectDirectory, String problemId) throws IOException {
        if (projectDirectory.isEmpty() || problemId == null || problemId.isBlank()) {
            return;
        }
        Path output = canvasStatePath(projectDirectory.get(), problemId);
        if (Files.isRegularFile(output)) {
            Files.delete(output);
        }
    }

    private static Path canvasStatePath(Path root, String problemId) throws IOException {
        String safeProblemId = problemId == null ? "" : problemId.strip().toLowerCase(java.util.Locale.ROOT);
        if (safeProblemId.isBlank()) {
            throw new IOException("No se puede guardar el estado editable sin identificador de problema.");
        }
        return safeProjectPath(root, Path.of("study", "problems", safeProblemId, "solution", "canvas-state.json"));
    }

    private static TechnicalProblem findProblem(DocuPodcastProject project, String problemId) throws IOException {
        String normalized = problemId == null ? "" : problemId.strip();
        if (normalized.isBlank()) {
            throw new IOException("Selecciona un problema tecnico guardado.");
        }
        return project.study().technicalProblems().stream()
                .filter(problem -> problem.id().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IOException("No se encontro el problema tecnico " + normalized + "."));
    }

    private static List<String> studyAssetIds(TechnicalProblem problem) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (!problem.solutionImageAssetId().isBlank()) {
            ids.add(problem.solutionImageAssetId());
        }
        for (StudySourceReference source : problem.sources()) {
            if (!source.sourceCropAssetId().isBlank()) {
                ids.add(source.sourceCropAssetId());
            }
        }
        return List.copyOf(ids);
    }

    private static SolutionAssetTarget solutionAssetTarget(DocuPodcastProject project, TechnicalProblem problem) {
        if (!problem.solutionImageAssetId().isBlank()) {
            Optional<ProjectAssetReference> existing = project.assets().byId(problem.solutionImageAssetId());
            if (existing.isPresent()) {
                return new SolutionAssetTarget(problem.solutionImageAssetId(), Path.of(existing.get().relativePath()));
            }
        }
        String suffix = numericSuffix(problem.id());
        String assetId = uniqueAssetId(project, "STUDY-SOLUTION-" + suffix);
        Path relative = Path.of("study", "problems", problem.id().toLowerCase(java.util.Locale.ROOT), "solution.png");
        return new SolutionAssetTarget(assetId, relative);
    }

    private static String numericSuffix(String problemId) {
        String normalized = problemId == null ? "" : problemId.strip();
        int dash = normalized.lastIndexOf('-');
        String suffix = dash >= 0 ? normalized.substring(dash + 1) : normalized;
        return suffix.matches("\\d+") ? suffix : "001";
    }

    private static String uniqueAssetId(DocuPodcastProject project, String base) {
        if (project.assets().byId(base).isEmpty()) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (project.assets().byId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo crear un id unico para asset de solucion.");
    }

    private static void deleteProjectAssetFileIfPresent(java.util.Optional<Path> projectDirectory, ProjectAssetReference asset) throws IOException {
        Optional<Path> path = resolveProjectAsset(projectDirectory, asset);
        if (path.isPresent() && Files.isRegularFile(path.get())) {
            Files.delete(path.get());
        }
    }

    private static Optional<Path> resolveProjectAsset(java.util.Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) {
            return Optional.empty();
        }
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    private static Path requireTarget(Path target) throws IOException {
        if (target == null) {
            throw new IOException("Selecciona un archivo destino.");
        }
        Path output = target.toAbsolutePath().normalize();
        if (Files.isDirectory(output)) {
            throw new IOException("El destino debe ser un archivo.");
        }
        if (output.getParent() == null) {
            throw new IOException("El destino debe tener carpeta padre.");
        }
        return output;
    }

    private static Path withPdfExtension(Path target) {
        String name = target.getFileName().toString();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            return target;
        }
        return target.resolveSibling(name + ".pdf");
    }

    private static String stripExtension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot <= 0 ? (fileName == null || fileName.isBlank() ? "ejercicios" : fileName) : fileName.substring(0, dot);
    }

    private record SolutionAssetTarget(String assetId, Path relativePath) {
    }

    public record BatchExportReport(Path directory, Path manifestPath, int exported, int skipped, int failed) {
    }

    public record PdfExportReport(Path pdfPath, Path manifestPath, int exported, int skipped, int failed) {
    }
}
