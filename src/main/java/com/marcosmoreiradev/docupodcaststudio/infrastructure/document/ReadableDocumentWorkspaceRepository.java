package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.ImportedDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocument;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Writes the imported document snapshot and source copy into a project folder. */
public final class ReadableDocumentWorkspaceRepository implements ImportedDocumentWorkspaceRepository {
    private final PreparedPdfDocumentRepository preparedPdfRepository;

    public ReadableDocumentWorkspaceRepository() {
        this(new JsonPreparedPdfDocumentRepository());
    }

    public ReadableDocumentWorkspaceRepository(PreparedPdfDocumentRepository preparedPdfRepository) {
        this.preparedPdfRepository = Objects.requireNonNull(preparedPdfRepository, "preparedPdfRepository");
    }

    @Override
    public MaterializedImportedDocument materialize(ReadableDocument document, ReadingProfile activeReadingProfile, Path projectFile) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(activeReadingProfile, "activeReadingProfile");
        Objects.requireNonNull(projectFile, "projectFile");
        if (document.format() == SourceDocumentFormat.PDF) {
            throw new IOException("PDF debe materializarse mediante PreparedPdfSource.");
        }
        Path root = projectFile.toAbsolutePath().getParent();
        if (root == null) {
            root = Path.of(".").toAbsolutePath();
        }
        Files.createDirectories(root.resolve("source"));
        Files.createDirectories(root.resolve("document"));

        String sourceName = safeFileName(document.sourcePath().getFileName().toString());
        Path copiedSource = root.resolve("source").resolve(sourceName).toAbsolutePath().normalize();
        Path originalSource = document.sourcePath().toAbsolutePath().normalize();
        if (Files.isRegularFile(originalSource) && !originalSource.equals(copiedSource)) {
            Files.copy(originalSource, copiedSource, StandardCopyOption.REPLACE_EXISTING);
        }
        ReadableDocument projectSourceDocument = rebaseToProjectSource(document, copiedSource);
        removePdfStorage(root);
        Path documentJson = root.resolve("document").resolve("document.json");
        new AtomicUtf8JsonFileWriter().write(documentJson, toJson(projectSourceDocument, activeReadingProfile));

        ProjectAssetReference sourceAsset = ProjectAssetReference.sourceDocument(
                "SRC-001",
                sourceName,
                "source/" + sourceName,
                mimeForSource(document.sourcePath())
        );
        ProjectAssetReference importedAsset = new ProjectAssetReference(
                "DOC-001",
                ProjectAssetKind.IMPORTED_DOCUMENT,
                "Documento importado",
                "document/document.json",
                "application/json",
                "Representación interna normalizada del documento fuente",
                "",
                "Generado por DocuPodcast Studio al guardar el proyecto"
        );
        return new MaterializedImportedDocument(sourceAsset, importedAsset,
                new BlockDocumentSource(projectSourceDocument));
    }

    @Override
    public MaterializedImportedDocument materialize(ProjectDocumentSource source,
                                                    ReadingProfile activeReadingProfile,
                                                    Path projectFile) throws IOException {
        Objects.requireNonNull(source, "source");
        if (source instanceof BlockDocumentSource block) {
            return materialize(block.document(), activeReadingProfile, projectFile);
        }
        return materializePdf((PreparedPdfSource) source, projectFile);
    }

    private MaterializedImportedDocument materializePdf(PreparedPdfSource pdf,
                                                        Path projectFile) throws IOException {
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) root = Path.of(".").toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("source"));
        Files.createDirectories(root.resolve("document"));

        Path previousRoot = pdf.workspace().projectRoot();
        PdfDocumentManifest previousManifest = preparedPdfRepository.loadManifest(previousRoot)
                .orElseThrow(() -> new IOException("Falta el manifest PDF V2 en " + previousRoot));
        List<PreparedPdfPage> carriedPages = previousRoot.equals(root)
                ? List.of() : preparedPdfRepository.loadPages(previousRoot);
        String sourceName = safeFileName(pdf.sourcePath().getFileName().toString());
        Path copiedSource = root.resolve("source").resolve(sourceName).toAbsolutePath().normalize();
        if (!pdf.sourcePath().toAbsolutePath().normalize().equals(copiedSource)) {
            copyAtomically(pdf.sourcePath(), copiedSource);
        }
        String hash = sha256(copiedSource);
        if (!hash.equalsIgnoreCase(pdf.workspace().sourceSha256())) {
            throw new IOException("El hash del PDF fuente no coincide con el workspace preparado.");
        }
        Instant now = Instant.now();
        preparedPdfRepository.initialize(root, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                pdf.title(), sourceName, hash, previousManifest.pageCount(),
                previousManifest.preparationSignature(),
                previousManifest.analysisSummary(), previousManifest.createdAt(), now,
                previousManifest.readingStrategy(), previousManifest.nativeTextProvider()));
        for (PreparedPdfPage page : carriedPages) {
            preparedPdfRepository.savePage(root, page);
        }
        if (!previousRoot.equals(root)) {
            copyPageMapSidecars(previousRoot, root);
            copyPdfOperationSidecars(previousRoot, root);
        }
        Files.deleteIfExists(root.resolve("document").resolve("document.json"));

        ProjectAssetReference sourceAsset = ProjectAssetReference.sourceDocument(
                "SRC-001", sourceName, "source/" + sourceName, "application/pdf");
        ProjectAssetReference importedAsset = new ProjectAssetReference(
                "DOC-001", ProjectAssetKind.IMPORTED_DOCUMENT, "Documento PDF preparado",
                JsonPreparedPdfDocumentRepository.MANIFEST_RELATIVE_PATH,
                "application/json", "Manifest canónico PDF V2", "",
                "Generado por DocuPodcast Studio al guardar el proyecto");
        return new MaterializedImportedDocument(sourceAsset, importedAsset,
                new PreparedPdfSource(new PreparedPdfWorkspaceRef(root, copiedSource, hash), pdf.title()));
    }

    private static void copyAtomically(Path source, Path target) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyPageMapSidecars(Path sourceRoot, Path targetRoot) throws IOException {
        Path sourceDirectory = sourceRoot.resolve("document/page-maps").normalize();
        if (!Files.isDirectory(sourceDirectory)) return;
        Path targetDirectory = targetRoot.resolve("document/page-maps").normalize();
        Files.createDirectories(targetDirectory);
        try (java.util.stream.Stream<Path> stream = Files.list(sourceDirectory)) {
            for (Path source : stream.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .matches("(?:manifest|page-\\d{6})\\.json"))
                    .toList()) {
                copyAtomically(source, targetDirectory.resolve(source.getFileName()));
            }
        }
    }

    private static void copyPdfOperationSidecars(
            Path sourceRoot, Path targetRoot) throws IOException {
        Path sourceDirectory = sourceRoot.resolve(
                JsonPdfOperationAttemptRepository.RELATIVE_DIRECTORY).normalize();
        if (!Files.isDirectory(sourceDirectory)) return;
        Path targetDirectory = targetRoot.resolve(
                JsonPdfOperationAttemptRepository.RELATIVE_DIRECTORY).normalize();
        Files.createDirectories(targetDirectory);
        try (java.util.stream.Stream<Path> stream = Files.list(sourceDirectory)) {
            for (Path source : stream.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .matches("PDF-ATTEMPT-[a-fA-F0-9]+\\.json"))
                    .toList()) {
                copyAtomically(source,
                        targetDirectory.resolve(source.getFileName()));
            }
        }
    }

    private static ReadableDocument rebaseToProjectSource(ReadableDocument document, Path copiedSource) {
        return new ReadableDocument(document.title(), document.format(), copiedSource, document.blocks(), document.importReport());
    }

    private static void removePdfStorage(Path root) throws IOException {
        Path documentDirectory = root.toAbsolutePath().normalize().resolve("document");
        Files.deleteIfExists(documentDirectory.resolve("manifest.json"));
        Path pages = documentDirectory.resolve("pages");
        if (Files.isDirectory(pages)) {
            try (java.util.stream.Stream<Path> stream = Files.list(pages)) {
                for (Path path : stream.filter(Files::isRegularFile)
                        .filter(candidate -> candidate.getFileName().toString()
                                .matches("page-\\d{6}\\.json(?:\\.tmp)?"))
                        .toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Path pageMaps = documentDirectory.resolve("page-maps");
        if (Files.isDirectory(pageMaps)) {
            try (java.util.stream.Stream<Path> stream = Files.list(pageMaps)) {
                for (Path path : stream.filter(Files::isRegularFile)
                        .filter(candidate -> candidate.getFileName().toString()
                                .matches("(?:manifest|page-\\d{6})\\.json(?:\\.tmp)?"))
                        .toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Path operations = documentDirectory.resolve("pdf-operations");
        if (Files.isDirectory(operations)) {
            try (java.util.stream.Stream<Path> stream = Files.list(operations)) {
                for (Path path : stream.filter(Files::isRegularFile)
                        .filter(candidate -> candidate.getFileName().toString()
                                .matches("PDF-ATTEMPT-[a-fA-F0-9]+\\.json(?:\\.tmp)?"))
                        .toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ProjectDocumentSource> load(Path projectFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) {
            root = Path.of(".").toAbsolutePath().normalize();
        }
        Optional<PdfDocumentManifest> pdfManifest = preparedPdfRepository.loadManifest(root);
        if (pdfManifest.isPresent()) {
            PdfDocumentManifest manifest = pdfManifest.get();
            Path sourcePath = root.resolve("source").resolve(safeFileName(manifest.sourceFile())).normalize();
            return Optional.of(new PreparedPdfSource(
                    new PreparedPdfWorkspaceRef(root, sourcePath, manifest.sourceSha256()),
                    manifest.title()));
        }
        Path documentJson = root.resolve("document").resolve("document.json").normalize();
        if (!Files.isRegularFile(documentJson)) {
            return Optional.empty();
        }
        Object parsed = SimpleJsonParser.parse(Files.readString(documentJson, StandardCharsets.UTF_8));
        Map<String, Object> map = object(parsed, "document/document.json");
        String title = stringOrDefault(map.get("title"), "Documento importado");
        SourceDocumentFormat format = enumOrDefault(SourceDocumentFormat.class, stringOrDefault(map.get("format"), SourceDocumentFormat.UNKNOWN.name()), SourceDocumentFormat.UNKNOWN);
        if (format == SourceDocumentFormat.PDF) {
            throw new IOException("Este proyecto usa el almacenamiento PDF preliminar. "
                    + "Vuelve a importar el PDF para crear su espacio documental V2.");
        }
        String sourceFileName = stringOrDefault(map.get("sourcePath"), title + ".docx");
        Path sourcePath = root.resolve("source").resolve(safeFileName(sourceFileName)).normalize();
        List<DocumentBlock> blocks = enrichEmbeddedImagesFromSource(sourcePath, readBlocks(map.get("blocks")));
        DocumentImportReport report = readImportReport(optionalObject(map.get("importReport")));
        return Optional.of(new BlockDocumentSource(
                new ReadableDocument(title, format, sourcePath, blocks, report)));
    }


    private static List<DocumentBlock> enrichEmbeddedImagesFromSource(Path sourcePath, List<DocumentBlock> blocks) {
        if (sourcePath == null || blocks == null || blocks.isEmpty() || !Files.isRegularFile(sourcePath)) {
            return blocks;
        }
        String fileName = sourcePath.getFileName() == null ? "" : sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".docx")) {
            return blocks;
        }
        List<EmbeddedDocumentImage> images = readEmbeddedDocumentImages(sourcePath);
        if (images.isEmpty()) {
            return blocks;
        }
        ArrayList<DocumentBlock> next = new ArrayList<>(blocks.size());
        int imageIndex = 0;
        boolean changed = false;
        for (DocumentBlock block : blocks) {
            if (block.type() == DocumentBlockType.IMAGE_NOTICE) {
                Map<String, String> metadata = new LinkedHashMap<>(block.metadata());
                if (metadata.getOrDefault("embeddedImageBase64", "").isBlank() && imageIndex < images.size()) {
                    EmbeddedDocumentImage image = images.get(imageIndex);
                    metadata.putIfAbsent("embeddedImagePath", image.packagePath());
                    metadata.putIfAbsent("embeddedImageMimeType", image.mimeType());
                    metadata.put("embeddedImageBase64", image.base64());
                    metadata.putIfAbsent("visualBlock", "true");
                    metadata.putIfAbsent("storyboardAssignment", "user-controlled");
                    next.add(new DocumentBlock(block.id(), block.type(), block.text(), block.originalStyle(), metadata));
                    changed = true;
                } else {
                    next.add(block);
                }
                imageIndex++;
            } else {
                next.add(block);
            }
        }
        return changed ? List.copyOf(next) : blocks;
    }

    private static List<EmbeddedDocumentImage> readEmbeddedDocumentImages(Path docx) {
        ArrayList<EmbeddedDocumentImage> images = new ArrayList<>();
        try (ZipFile zip = new ZipFile(docx.toFile())) {
            ArrayList<? extends ZipEntry> entries = java.util.Collections.list(zip.entries()).stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith("word/media/"))
                    .filter(entry -> isSupportedImagePath(entry.getName()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            entries.sort(java.util.Comparator.comparing(ZipEntry::getName));
            for (ZipEntry entry : entries) {
                try (java.io.InputStream input = zip.getInputStream(entry)) {
                    byte[] bytes = input.readAllBytes();
                    if (bytes.length > 0) {
                        images.add(new EmbeddedDocumentImage(entry.getName(), mimeTypeForImage(entry.getName()), Base64.getEncoder().encodeToString(bytes)));
                    }
                }
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return List.copyOf(images);
    }

    private static boolean isSupportedImagePath(String path) {
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    private static String mimeTypeForImage(String path) {
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }

    private record EmbeddedDocumentImage(String packagePath, String mimeType, String base64) {
    }

    @SuppressWarnings("unchecked")
    private static List<DocumentBlock> readBlocks(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("document.blocks must be an array");
        }
        ArrayList<DocumentBlock> blocks = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> block = object(item, "document.blocks[]");
            blocks.add(new DocumentBlock(
                    string(block.get("id"), "block.id"),
                    enumOrDefault(DocumentBlockType.class, stringOrDefault(block.get("type"), DocumentBlockType.PARAGRAPH.name()), DocumentBlockType.PARAGRAPH),
                    stringOrDefault(block.get("text"), ""),
                    stringOrDefault(block.get("originalStyle"), ""),
                    readStringMap(optionalObject(block.get("metadata")))
            ));
        }
        return List.copyOf(blocks);
    }

    private static DocumentImportReport readImportReport(Map<String, Object> report) throws IOException {
        Object rawIssues = report.getOrDefault("issues", List.of());
        if (!(rawIssues instanceof List<?> list)) {
            throw new IOException("document.importReport.issues must be an array");
        }
        ArrayList<DocumentImportIssue> issues = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> issue = object(item, "document.importReport.issues[]");
            issues.add(new DocumentImportIssue(
                    enumOrDefault(DocumentImportIssueLevel.class, stringOrDefault(issue.get("level"), DocumentImportIssueLevel.INFO.name()), DocumentImportIssueLevel.INFO),
                    stringOrDefault(issue.get("code"), "DOC_IMPORT_INFO"),
                    stringOrDefault(issue.get("message"), "Diagnóstico importado"),
                    stringOrDefault(issue.get("blockId"), "")
            ));
        }
        return new DocumentImportReport(issues);
    }

    private static Map<String, String> readStringMap(Map<String, Object> raw) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String field) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException(field + " must be an object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) throws IOException {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("Expected object");
        }
        return (Map<String, Object>) map;
    }

    private static String string(Object value, String field) throws IOException {
        if (!(value instanceof String text)) {
            throw new IOException(field + " must be a string");
        }
        return text;
    }

    private static String stringOrDefault(Object value, String defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String text)) {
            throw new IOException("Expected string value");
        }
        return text;
    }

    private static <E extends Enum<E>> E enumOrDefault(Class<E> enumType, String value, E fallback) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String toJson(ReadableDocument document, ReadingProfile activeReadingProfile) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        field(out, 1, "title", quote(document.title()), true);
        field(out, 1, "format", quote(document.format().name()), true);
        field(out, 1, "sourcePath", quote(document.sourcePath().getFileName().toString()), true);
        writeReadingProfile(out, activeReadingProfile);
        out.append(",\n");
        field(out, 1, "narratableBlockCount", Long.toString(document.narratableBlockCount()), true);
        field(out, 1, "imageNoticeCount", Long.toString(document.imageNoticeCount()), true);
        field(out, 1, "tableNoticeCount", Long.toString(document.tableNoticeCount()), true);
        field(out, 1, "ignoredCount", Long.toString(document.ignoredCount()), true);
        writeSummary(out, document);
        out.append(",\n");
        writeIssues(out, document);
        out.append(",\n");
        writeBlocks(out, document);
        out.append("}\n");
        return out.toString();
    }



    private static void writeReadingProfile(StringBuilder out, ReadingProfile profile) {
        indent(out, 1).append("\"readingProfile\": {\n");
        field(out, 2, "id", quote(profile.id()), true);
        field(out, 2, "name", quote(profile.name()), true);
        field(out, 2, "imagePolicy", quote(profile.imagePolicy().name()), true);
        field(out, 2, "tablePolicy", quote(profile.tablePolicy().name()), true);
        field(out, 2, "maxShortBoldWords", Integer.toString(profile.headingRules().maxShortBoldWords()), true);
        field(out, 2, "treatShortBoldParagraphAsSubheading", Boolean.toString(profile.headingRules().treatShortBoldParagraphAsSubheading()), false);
        indent(out, 1).append("}");
    }

    private static void writeSummary(StringBuilder out, ReadableDocument document) {
        indent(out, 1).append("\"summary\": {\n");
        field(out, 2, "blocks", Long.toString(document.blocks().size()), true);
        field(out, 2, "narratableBlocks", Long.toString(document.narratableBlockCount()), true);
        field(out, 2, "titleBlocks", Long.toString(document.titleCount()), true);
        field(out, 2, "headingBlocks", Long.toString(document.headingCount()), true);
        field(out, 2, "subheadingBlocks", Long.toString(document.subheadingCount()), true);
        field(out, 2, "listItems", Long.toString(document.listItemCount()), true);
        field(out, 2, "images", Long.toString(document.imageNoticeCount()), true);
        field(out, 2, "tables", Long.toString(document.tableNoticeCount()), true);
        field(out, 2, "ignored", Long.toString(document.ignoredCount()), true);
        field(out, 2, "warnings", Long.toString(document.warningCount()), true);
        field(out, 2, "errors", Long.toString(document.errorCount()), false);
        indent(out, 1).append("}");
    }

    private static void writeIssues(StringBuilder out, ReadableDocument document) {
        indent(out, 1).append("\"importReport\": {\n");
        field(out, 2, "warningCount", Long.toString(document.importReport().warningCount()), true);
        field(out, 2, "errorCount", Long.toString(document.importReport().errorCount()), true);
        indent(out, 2).append("\"issues\": [");
        if (!document.importReport().issues().isEmpty()) out.append("\n");
        for (int i = 0; i < document.importReport().issues().size(); i++) {
            DocumentImportIssue issue = document.importReport().issues().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "level", quote(issue.level().name()), true);
            field(out, 4, "code", quote(issue.code()), true);
            field(out, 4, "message", quote(issue.message()), true);
            field(out, 4, "blockId", quote(issue.blockId()), false);
            indent(out, 3).append("}");
            if (i < document.importReport().issues().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }

    private static void writeBlocks(StringBuilder out, ReadableDocument document) {
        indent(out, 1).append("\"blocks\": [");
        if (!document.blocks().isEmpty()) out.append("\n");
        for (int i = 0; i < document.blocks().size(); i++) {
            DocumentBlock block = document.blocks().get(i);
            indent(out, 2).append("{\n");
            field(out, 3, "id", quote(block.id()), true);
            field(out, 3, "type", quote(block.type().name()), true);
            field(out, 3, "text", quote(block.text()), true);
            field(out, 3, "originalStyle", quote(block.originalStyle()), true);
            writeMetadata(out, block.metadata());
            indent(out, 2).append("}");
            if (i < document.blocks().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, 1).append("]\n");
    }

    private static void writeMetadata(StringBuilder out, Map<String, String> metadata) {
        indent(out, 3).append("\"metadata\": {");
        if (!metadata.isEmpty()) out.append("\n");
        int index = 0;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            indent(out, 4).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
            if (index < metadata.size() - 1) out.append(",");
            out.append("\n");
            index++;
        }
        if (!metadata.isEmpty()) indent(out, 3);
        out.append("}\n");
    }

    private static void field(StringBuilder out, int level, String name, String value, boolean comma) {
        indent(out, level).append(quote(name)).append(": ").append(value);
        if (comma) out.append(",");
        out.append("\n");
    }

    private static StringBuilder indent(StringBuilder out, int level) {
        return out.append("    ".repeat(level));
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(safe.length() + 16);
        escaped.append('"');
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }

    private static String safeFileName(String fileName) {
        String cleaned = fileName == null ? "source.docx" : fileName.replace('\\', '_').replace('/', '_').strip();
        return cleaned.isBlank() ? "source.docx" : cleaned;
    }

    private static String mimeForSource(Path sourcePath) {
        String name = sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private static int sourcePageCount(ReadableDocument document) throws IOException {
        int pageCount = 0;
        for (DocumentBlock block : document.blocks()) {
            pageCount = Math.max(pageCount, positiveInt(block.metadata().get("sourcePageCount")));
            pageCount = Math.max(pageCount, positiveInt(block.metadata().get("sourcePage")));
        }
        if (pageCount <= 0) {
            throw new IOException("No se pudo determinar el número de páginas del PDF.");
        }
        return pageCount;
    }

    private static int positiveInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.max(0, Integer.parseInt(value.strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (java.io.InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 no está disponible.", ex);
        }
    }
}
