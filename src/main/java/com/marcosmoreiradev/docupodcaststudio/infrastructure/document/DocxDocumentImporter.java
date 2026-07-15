package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DOCX importer implemented with JDK ZIP/XML APIs.
 *
 * <p>The importer intentionally works in terms of normalized document blocks,
 * not layout fidelity. It walks {@code word/document.xml} in body order,
 * uses {@code word/styles.xml} when present, extracts paragraphs, list items,
 * heading-like styles, simple table notices, image notices and a lightweight
 * diagnostic report for the Document workspace.</p>
 */
public final class DocxDocumentImporter implements DocumentImporter {
    private static final String WORD_DOCUMENT_XML = "word/document.xml";
    private static final String WORD_STYLES_XML = "word/styles.xml";
    private static final String WORD_DOCUMENT_RELS_XML = "word/_rels/document.xml.rels";
    private static final String CORE_PROPERTIES_XML = "docProps/core.xml";
    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    @Override
    public boolean supports(Path sourceFile) {
        if (sourceFile == null || sourceFile.getFileName() == null) {
            return false;
        }
        return sourceFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".docx");
    }

    @Override
    public ReadableDocument importDocument(Path sourceFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("El archivo DOCX no existe: " + sourceFile);
        }

        DocxPackageContent packageContent = readPackageContent(sourceFile);
        Map<String, String> packageEntries = packageContent.textEntries();
        String documentXml = packageEntries.get(WORD_DOCUMENT_XML);
        if (documentXml == null || documentXml.isBlank()) {
            throw new IOException("El DOCX no contiene " + WORD_DOCUMENT_XML);
        }

        Document documentDom = parseXml(documentXml);
        Map<String, String> styleNames = parseStyleNames(packageEntries.get(WORD_STYLES_XML));
        String title = firstNonBlank(parseCoreTitle(packageEntries.get(CORE_PROPERTIES_XML)), titleFromPath(sourceFile));

        ArrayList<DocumentBlock> blocks = new ArrayList<>();
        ArrayList<DocumentImportIssue> issues = new ArrayList<>();
        int[] counter = {1};
        int[] imageCounter = {0};
        walkBody(documentDom, styleNames, packageContent.mediaImages(), packageContent.imagesByRelationshipId(), blocks, issues, counter, imageCounter);
        appendUnreferencedMediaImagesIfNeeded(packageContent.mediaImages(), blocks, issues, counter);

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.of(nextId(counter), DocumentBlockType.EMPTY,
                    "El documento Word no contiene texto extraíble en word/document.xml.", ""));
            issues.add(DocumentImportIssue.warning("DOCX_NO_TEXT", "El DOCX no contiene texto narrable extraíble."));
        } else {
            addStructuralDiagnostics(blocks, issues);
        }

        return new ReadableDocument(title, SourceDocumentFormat.DOCX, sourceFile, blocks, new DocumentImportReport(issues));
    }

    private static DocxPackageContent readPackageContent(Path sourceFile) throws IOException {
        Map<String, String> textEntries = new HashMap<>();
        ArrayList<EmbeddedImage> mediaImages = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(sourceFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                byte[] bytes = zip.readAllBytes();
                if (WORD_DOCUMENT_XML.equals(name)
                        || WORD_STYLES_XML.equals(name)
                        || WORD_DOCUMENT_RELS_XML.equals(name)
                        || CORE_PROPERTIES_XML.equals(name)) {
                    textEntries.put(name, new String(bytes, StandardCharsets.UTF_8));
                } else if (isWordMediaImage(name)) {
                    mediaImages.add(new EmbeddedImage(name, mimeType(name), Base64.getEncoder().encodeToString(bytes)));
                }
            }
        }
        return new DocxPackageContent(textEntries, List.copyOf(mediaImages), imageRelationshipMap(textEntries.get(WORD_DOCUMENT_RELS_XML), mediaImages));
    }

    private record DocxPackageContent(Map<String, String> textEntries, List<EmbeddedImage> mediaImages,
                                      Map<String, EmbeddedImage> imagesByRelationshipId) {
    }

    private record EmbeddedImage(String packagePath, String mimeType, String base64) {
    }

    private record ImageReference(String description, String relationshipId) {
    }

    private static boolean isWordMediaImage(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return lower.startsWith("word/media/")
                && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp"));
    }

    private static String mimeType(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }

    private static Map<String, EmbeddedImage> imageRelationshipMap(String relsXml, List<EmbeddedImage> mediaImages) throws IOException {
        if (relsXml == null || relsXml.isBlank() || mediaImages.isEmpty()) {
            return Map.of();
        }
        Map<String, EmbeddedImage> byPackagePath = new HashMap<>();
        for (EmbeddedImage image : mediaImages) {
            byPackagePath.put(image.packagePath(), image);
        }
        Map<String, EmbeddedImage> byRelationshipId = new HashMap<>();
        Document rels = parseXml(relsXml);
        NodeList relationships = rels.getElementsByTagNameNS("*", "Relationship");
        for (int i = 0; i < relationships.getLength(); i++) {
            if (!(relationships.item(i) instanceof Element relationship)) {
                continue;
            }
            String id = relationship.getAttribute("Id");
            String target = relationship.getAttribute("Target");
            String normalizedTarget = normalizeWordTarget(target);
            EmbeddedImage image = byPackagePath.get(normalizedTarget);
            if (!id.isBlank() && image != null) {
                byRelationshipId.put(id, image);
            }
        }
        return Map.copyOf(byRelationshipId);
    }

    private static String normalizeWordTarget(String target) {
        String normalized = target == null ? "" : target.replace('\\', '/').strip();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        if (!normalized.startsWith("word/")) {
            normalized = "word/" + normalized;
        }
        return normalized;
    }

    private static Document parseXml(String xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml.stripLeading())));
        } catch (Exception ex) {
            throw new IOException("No se pudo leer el XML interno del DOCX", ex);
        }
    }

    private static Map<String, String> parseStyleNames(String stylesXml) throws IOException {
        if (stylesXml == null || stylesXml.isBlank()) {
            return Map.of();
        }
        Document stylesDom = parseXml(stylesXml);
        HashMap<String, String> styleNames = new HashMap<>();
        NodeList styles = stylesDom.getElementsByTagNameNS("*", "style");
        for (int i = 0; i < styles.getLength(); i++) {
            if (!(styles.item(i) instanceof Element style)) {
                continue;
            }
            String styleId = attr(style, W_NS, "styleId");
            NodeList names = style.getElementsByTagNameNS("*", "name");
            String name = "";
            if (names.getLength() > 0 && names.item(0) instanceof Element nameElement) {
                name = attr(nameElement, W_NS, "val");
            }
            if (!styleId.isBlank() && !name.isBlank()) {
                styleNames.put(styleId, name);
            }
        }
        return styleNames;
    }

    private static String parseCoreTitle(String coreXml) throws IOException {
        if (coreXml == null || coreXml.isBlank()) {
            return "";
        }
        Document core = parseXml(coreXml);
        NodeList titleNodes = core.getElementsByTagNameNS("*", "title");
        if (titleNodes.getLength() == 0) {
            return "";
        }
        return titleNodes.item(0).getTextContent() == null ? "" : titleNodes.item(0).getTextContent().strip();
    }

    private static void walkBody(Document dom, Map<String, String> styleNames, List<EmbeddedImage> mediaImages,
                                 Map<String, EmbeddedImage> imagesByRelationshipId,
                                 List<DocumentBlock> blocks, List<DocumentImportIssue> issues, int[] counter, int[] imageCounter) {
        NodeList bodyNodes = dom.getElementsByTagNameNS("*", "body");
        if (bodyNodes.getLength() == 0 || !(bodyNodes.item(0) instanceof Element body)) {
            issues.add(DocumentImportIssue.warning("DOCX_NO_BODY", "El DOCX no contiene cuerpo w:body."));
            return;
        }
        NodeList children = body.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element element)) {
                continue;
            }
            String localName = element.getLocalName();
            if ("p".equals(localName)) {
                appendParagraph(element, styleNames, mediaImages, imagesByRelationshipId, blocks, issues, counter, imageCounter);
            } else if ("tbl".equals(localName)) {
                appendTable(element, blocks, issues, counter);
            }
        }
    }

    private static void appendParagraph(Element paragraph, Map<String, String> styleNames, List<EmbeddedImage> mediaImages,
                                        Map<String, EmbeddedImage> imagesByRelationshipId,
                                        List<DocumentBlock> blocks, List<DocumentImportIssue> issues, int[] counter, int[] imageCounter) {
        String text = textContentOf(paragraph).replace('\u00A0', ' ').strip();
        String styleId = paragraphStyleId(paragraph);
        String styleName = styleNames.getOrDefault(styleId, "");
        boolean numbered = hasDescendant(paragraph, "numPr");
        boolean partialBold = hasDescendant(paragraph, "b");
        boolean structurallyBold = isEffectivelyBoldParagraph(paragraph);
        boolean math = hasMathDescendant(paragraph) || hasLatexText(text);
        DocumentBlockType type = typeFromStyle(styleId, styleName, text, numbered, structurallyBold);
        String id = null;
        if (math) {
            id = nextId(counter);
            Map<String, String> metadata = new LinkedHashMap<>();
            putIfNotBlank(metadata, "styleId", styleId);
            putIfNotBlank(metadata, "styleName", styleName);
            metadata.put("source", "docx-math");
            metadata.put("sourceLocatorLabel", "Word/DOCX · bloque " + id + " (paginación dinámica)");
            metadata.put("visualBlock", "true");
            metadata.put("storyboardAssignment", "user-controlled");
            metadata.put("renderPolicy", "identified-only");
            String mathText = text.isBlank()
                    ? "Bloque matemático/fórmula detectado en el documento fuente."
                    : "Bloque matemático/fórmula detectado: " + abbreviate(text, 220);
            blocks.add(DocumentBlock.of(id, DocumentBlockType.MATH_NOTICE, mathText, firstNonBlank(styleName, styleId, "math"), metadata));
            issues.add(new DocumentImportIssue(DocumentImportIssueLevel.INFO, "MATH_BLOCK_DETECTED",
                    "Bloque matemático/fórmula detectado como visual fuente; se identifica sin intentar renderizar LaTeX/OMML en esta versión.", id));
        } else if (!text.isBlank()) {
            id = nextId(counter);
            Map<String, String> metadata = new LinkedHashMap<>();
            putIfNotBlank(metadata, "styleId", styleId);
            putIfNotBlank(metadata, "styleName", styleName);
            metadata.put("sourceLocatorLabel", "Word/DOCX · bloque " + id + " (paginación dinámica)");
            if (numbered) metadata.put("list", "true");
            if (structurallyBold) metadata.put("bold", "true");
            if (partialBold && !structurallyBold) metadata.put("partialBold", "true");
            blocks.add(DocumentBlock.of(id, type, text, firstNonBlank(styleName, styleId), metadata));
            if (type == DocumentBlockType.LIST_ITEM) {
                issues.add(DocumentImportIssue.info("LIST_ITEM_DETECTED", "Se detectó un elemento de lista."));
            }
            if (numbered) {
                issues.add(DocumentImportIssue.info("LIST_ITEM_DETECTED", "Elemento de lista detectado en el bloque " + id + "."));
            }
            if (text.length() > 1200) {
                issues.add(DocumentImportIssue.warning("LONG_PARAGRAPH", "El bloque es largo y quizá deba dividirse antes del TTS.", id));
            }
        }
        for (ImageReference imageReference : imageReferences(paragraph)) {
            String description = imageReference.description();
            String imageId = nextId(counter);
            String message = description.isBlank()
                    ? "Imagen detectada sin descripción en el documento fuente."
                    : "Imagen del documento fuente: " + description;
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("source", "docx-inline-image");
            metadata.put("sourceLocatorLabel", "Word/DOCX · bloque " + imageId + " (paginación dinámica)");
            metadata.put("visualBlock", "true");
            metadata.put("storyboardAssignment", "user-controlled");
            putIfNotBlank(metadata, "styleId", styleId);
            putIfNotBlank(metadata, "styleName", styleName);
            if (!description.isBlank()) {
                metadata.put("description", description);
            }
            EmbeddedImage embeddedImage = null;
            if (!imageReference.relationshipId().isBlank()) {
                embeddedImage = imagesByRelationshipId.get(imageReference.relationshipId());
                metadata.put("embeddedImageRelationshipId", imageReference.relationshipId());
            }
            if (embeddedImage == null && imageCounter[0] < mediaImages.size()) {
                embeddedImage = mediaImages.get(imageCounter[0]);
            }
            if (embeddedImage != null) {
                imageCounter[0]++;
                metadata.put("embeddedImagePath", embeddedImage.packagePath());
                metadata.put("embeddedImageMimeType", embeddedImage.mimeType());
                metadata.put("embeddedImageBase64", embeddedImage.base64());
            }
            blocks.add(DocumentBlock.of(imageId, DocumentBlockType.IMAGE_NOTICE, message, firstNonBlank(styleName, styleId), metadata));
            if (description.isBlank()) {
                issues.add(DocumentImportIssue.warning("IMAGE_WITHOUT_DESCRIPTION", "Imagen sin descripción textual detectada como bloque visual fuente; se muestra en la hoja y el usuario decide si la asocia a la secuencia visual.", imageId));
            }
        }
    }


    private static void appendUnreferencedMediaImagesIfNeeded(List<EmbeddedImage> mediaImages, List<DocumentBlock> blocks,
                                                            List<DocumentImportIssue> issues, int[] counter) {
        if (mediaImages == null || mediaImages.isEmpty()) {
            return;
        }
        java.util.Set<String> referencedPaths = blocks.stream()
                .filter(block -> block.type() == DocumentBlockType.IMAGE_NOTICE)
                .map(block -> block.metadata().getOrDefault("embeddedImagePath", ""))
                .filter(path -> path != null && !path.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        for (EmbeddedImage image : mediaImages) {
            if (referencedPaths.contains(image.packagePath())) {
                continue;
            }
            String imageId = nextId(counter);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("source", "docx-media-fallback");
            metadata.put("sourceLocatorLabel", "Word/DOCX · bloque " + imageId + " (paginación dinámica)");
            metadata.put("visualBlock", "true");
            metadata.put("storyboardAssignment", "user-controlled");
            metadata.put("embeddedImagePath", image.packagePath());
            metadata.put("embeddedImageMimeType", image.mimeType());
            metadata.put("embeddedImageBase64", image.base64());
            blocks.add(DocumentBlock.of(imageId, DocumentBlockType.IMAGE_NOTICE,
                    "Imagen detectada sin descripción en el documento fuente.", "image", metadata));
            issues.add(DocumentImportIssue.warning("IMAGE_MEDIA_FALLBACK",
                    "Imagen embebida recuperada desde word/media como bloque visual fuente.", imageId));
        }
    }

    private static void appendTable(Element table, List<DocumentBlock> blocks, List<DocumentImportIssue> issues, int[] counter) {
        List<List<String>> tableRows = extractTableRows(table);
        int rows = tableRows.isEmpty() ? table.getElementsByTagNameNS("*", "tr").getLength() : tableRows.size();
        int cells = table.getElementsByTagNameNS("*", "tc").getLength();
        int columns = tableRows.stream().mapToInt(List::size).max().orElse(0);
        String text = textContentOf(table).replaceAll("\\s+", " ").strip();
        if (text.isBlank()) {
            text = "Tabla detectada sin texto extraíble.";
        } else if (text.length() > 260) {
            text = text.substring(0, 260).strip() + "…";
        }
        String id = nextId(counter);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("rows", Integer.toString(rows));
        metadata.put("cells", Integer.toString(cells));
        metadata.put("columns", Integer.toString(columns));
        metadata.put("table.rowCount", Integer.toString(rows));
        metadata.put("table.columnCount", Integer.toString(columns));
        addTableStructureMetadata(metadata, tableRows, columns);
        putIfNotBlank(metadata, "tableMarkdown", tablePreviewMarkdown(tableRows));
        metadata.put("sourceLocatorLabel", "Word/DOCX · bloque " + id + " (paginación dinámica)");
        metadata.put("visualBlock", "true");
        metadata.put("storyboardAssignment", "user-controlled");
        blocks.add(DocumentBlock.of(id, DocumentBlockType.TABLE_NOTICE,
                "Tabla del documento fuente (%d filas, %d columnas): %s".formatted(rows, columns, text), "table", metadata));
        issues.add(DocumentImportIssue.info("TABLE_DETECTED", "Tabla detectada como bloque visual fuente; el usuario decide si la asocia a la secuencia visual."));
    }

    private static void addTableStructureMetadata(Map<String, String> metadata, List<List<String>> rows, int columns) {
        if (rows == null || rows.isEmpty() || columns <= 0) {
            return;
        }
        List<String> headers = rows.get(0);
        for (int c = 0; c < columns; c++) {
            String header = c < headers.size() ? headers.get(c) : "";
            metadata.put("table.header." + c, header.isBlank() ? "Columna " + (c + 1) : header);
        }
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < columns; c++) {
                metadata.put("table.cell." + r + "." + c, c < row.size() ? row.get(c) : "");
            }
        }
    }

    private static List<List<String>> extractTableRows(Element table) {
        ArrayList<List<String>> rows = new ArrayList<>();
        NodeList rowNodes = table.getElementsByTagNameNS("*", "tr");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            if (!(rowNodes.item(i) instanceof Element row)) {
                continue;
            }
            ArrayList<String> cells = new ArrayList<>();
            NodeList cellNodes = row.getElementsByTagNameNS("*", "tc");
            for (int c = 0; c < cellNodes.getLength(); c++) {
                if (cellNodes.item(c) instanceof Element cell) {
                    cells.add(textContentOf(cell).replaceAll("\\s+", " ").strip());
                }
            }
            if (!cells.isEmpty()) {
                rows.add(List.copyOf(cells));
            }
        }
        return List.copyOf(rows);
    }

    private static String tablePreviewMarkdown(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        int rowLimit = rows.size();
        StringBuilder out = new StringBuilder();
        for (int r = 0; r < rowLimit; r++) {
            out.append('|');
            for (int c = 0; c < columns; c++) {
                String cell = c < rows.get(r).size() ? rows.get(r).get(c) : "";
                out.append(' ').append(cell.replace("|", "\\|")).append(' ').append('|');
            }
            out.append('\n');
            if (r == 0) {
                out.append('|');
                for (int c = 0; c < columns; c++) {
                    out.append(" --- |");
                }
                out.append('\n');
            }
        }
        return out.toString().strip();
    }

    private static String textContentOf(Element element) {
        StringBuilder out = new StringBuilder();
        appendText(element, out);
        return out.toString();
    }

    private static void appendText(Node node, StringBuilder out) {
        if (node instanceof Element element) {
            String localName = element.getLocalName();
            if ("t".equals(localName)) {
                out.append(element.getTextContent());
                return;
            }
            if ("tab".equals(localName)) {
                out.append(' ');
                return;
            }
            if ("br".equals(localName) || "cr".equals(localName)) {
                out.append('\n');
                return;
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            appendText(children.item(i), out);
        }
    }

    private static String paragraphStyleId(Element paragraph) {
        NodeList styles = paragraph.getElementsByTagNameNS("*", "pStyle");
        if (styles.getLength() == 0 || !(styles.item(0) instanceof Element style)) {
            return "";
        }
        return attr(style, W_NS, "val");
    }


    private static boolean isEffectivelyBoldParagraph(Element paragraph) {
        if (paragraph == null) {
            return false;
        }
        NodeList runs = paragraph.getElementsByTagNameNS("*", "r");
        int textChars = 0;
        int boldChars = 0;
        for (int i = 0; i < runs.getLength(); i++) {
            if (!(runs.item(i) instanceof Element run)) {
                continue;
            }
            String runText = textContentOf(run).replace('\u00A0', ' ').strip();
            if (runText.isBlank()) {
                continue;
            }
            int length = runText.length();
            textChars += length;
            if (hasDescendant(run, "b")) {
                boldChars += length;
            }
        }
        return textChars > 0 && boldChars >= Math.ceil(textChars * 0.85d);
    }

    private static DocumentBlockType typeFromStyle(String styleId, String styleName, String text, boolean numbered, boolean bold) {
        String normalized = normalizeKey(styleId + " " + styleName);
        if (normalized.contains("heading1") || normalized.contains("heading 1") || normalized.contains("titulo1") || normalized.contains("titulo 1")) {
            return DocumentBlockType.HEADING;
        }
        if (normalized.contains("heading2") || normalized.contains("heading 2") || normalized.contains("heading3") || normalized.contains("heading 3")
                || normalized.contains("titulo2") || normalized.contains("titulo 2") || normalized.contains("titulo3") || normalized.contains("titulo 3")) {
            return DocumentBlockType.SUBHEADING;
        }
        if (normalized.contains("title") || normalized.contains("titulo")) {
            return DocumentBlockType.TITLE;
        }
        if (numbered) {
            return DocumentBlockType.LIST_ITEM;
        }
        if (!text.isBlank() && text.length() < 90 && text.matches("^\\d+(\\.\\d+)*[.)]?\\s+.+")) {
            return text.contains(".") ? DocumentBlockType.SUBHEADING : DocumentBlockType.HEADING;
        }
        if (bold && text.length() < 90) {
            return DocumentBlockType.SUBHEADING;
        }
        return DocumentBlockType.PARAGRAPH;
    }

    private static List<ImageReference> imageReferences(Element paragraph) {
        ArrayList<ImageReference> references = new ArrayList<>();
        NodeList drawings = paragraph.getElementsByTagNameNS("*", "drawing");
        NodeList picts = paragraph.getElementsByTagNameNS("*", "pict");
        NodeList blips = paragraph.getElementsByTagNameNS("*", "blip");
        NodeList imageData = paragraph.getElementsByTagNameNS("*", "imagedata");
        int imageCount = Math.max(drawings.getLength() + picts.getLength(), Math.max(blips.getLength(), imageData.getLength()));
        if (imageCount == 0) {
            return references;
        }
        NodeList docPr = paragraph.getElementsByTagNameNS("*", "docPr");
        for (int i = 0; i < imageCount; i++) {
            String description = "";
            if (i < docPr.getLength() && docPr.item(i) instanceof Element element) {
                description = firstNonBlank(element.getAttribute("descr"), element.getAttribute("title"));
            }
            String relationshipId = "";
            if (i < blips.getLength() && blips.item(i) instanceof Element blip) {
                relationshipId = relationshipId(blip, "embed");
            }
            if (relationshipId.isBlank() && i < imageData.getLength() && imageData.item(i) instanceof Element legacyImage) {
                relationshipId = relationshipId(legacyImage, "id");
            }
            references.add(new ImageReference(description, relationshipId));
        }
        return references;
    }

    private static String relationshipId(Element element, String localName) {
        String value = element.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", localName);
        if (value == null || value.isBlank()) {
            value = element.getAttribute("r:" + localName);
        }
        if (value == null || value.isBlank()) {
            value = element.getAttribute(localName);
        }
        return value == null ? "" : value.strip();
    }

    private static void addStructuralDiagnostics(List<DocumentBlock> blocks, List<DocumentImportIssue> issues) {
        long narratable = blocks.stream().filter(DocumentBlock::narratable).count();
        long headings = blocks.stream().filter(block -> block.type() == DocumentBlockType.TITLE
                || block.type() == DocumentBlockType.HEADING
                || block.type() == DocumentBlockType.SUBHEADING).count();
        long paragraphs = blocks.stream().filter(block -> block.type() == DocumentBlockType.PARAGRAPH).count();
        if (narratable == 0) {
            issues.add(DocumentImportIssue.warning("DOCX_NO_NARRATABLE_BLOCKS", "No se detectaron bloques narrables."));
        }
        if (headings == 0 && narratable > 0) {
            issues.add(DocumentImportIssue.warning("DOCX_NO_HEADINGS", "No se detectaron títulos o subtítulos; quizá debas ajustar el perfil de lectura."));
        }
        if (blocks.stream().noneMatch(block -> block.type() == DocumentBlockType.IMAGE_NOTICE)) {
            issues.add(DocumentImportIssue.info("DOCX_NO_IMAGES", "No se detectaron imágenes embebidas en el documento."));
        }
    }

    private static boolean hasMathDescendant(Element element) {
        return hasDescendant(element, "oMath")
                || hasDescendant(element, "oMathPara")
                || hasDescendant(element, "f")
                || hasDescendant(element, "rad")
                || hasDescendant(element, "sSup")
                || hasDescendant(element, "sSub")
                || hasDescendant(element, "nary");
    }

    private static boolean hasLatexText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String value = text.strip();
        return value.contains("\\(")
                || value.contains("\\[")
                || value.contains("$$")
                || value.matches(".*\\\\[a-zA-Z]+\\s*\\{.*");
    }

    private static boolean hasDescendant(Element element, String localName) {
        return element.getElementsByTagNameNS("*", localName).getLength() > 0;
    }

    private static String attr(Element element, String namespace, String localName) {
        String value = element.getAttributeNS(namespace, localName);
        if (value == null || value.isBlank()) {
            value = element.getAttribute("w:" + localName);
        }
        if (value == null || value.isBlank()) {
            value = element.getAttribute(localName);
        }
        return value == null ? "" : value.strip();
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.strip());
        }
    }

    private static String normalizeKey(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replace('_', ' ').replace('-', ' ');
    }

    private static String abbreviate(String value, int maxCharacters) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= maxCharacters) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxCharacters)).strip() + "…";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    private static String titleFromPath(Path sourceFile) {
        String name = sourceFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String nextId(int[] counter) {
        return "B%04d".formatted(counter[0]++);
    }
}
