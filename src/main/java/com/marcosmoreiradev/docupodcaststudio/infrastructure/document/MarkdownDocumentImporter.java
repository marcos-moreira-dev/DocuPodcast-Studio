package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentImporter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Imports human Markdown/MD files as read-only source documents, not as a special script contract. */
public final class MarkdownDocumentImporter implements DocumentImporter {
    @Override
    public boolean supports(Path sourceFile) {
        return PlainTextDocumentImporter.hasExtension(sourceFile, ".md")
                || PlainTextDocumentImporter.hasExtension(sourceFile, ".markdown");
    }

    @Override
    public ReadableDocument importDocument(Path sourceFile) throws IOException {
        String markdown = Files.readString(sourceFile, StandardCharsets.UTF_8);
        List<DocumentBlock> blocks = blocksFromMarkdown(markdown);
        List<DocumentImportIssue> issues = new ArrayList<>();
        if (blocks.isEmpty()) {
            issues.add(DocumentImportIssue.warning("empty-markdown", "El Markdown no contiene texto narrable."));
            blocks = List.of(DocumentBlock.of("B0001", DocumentBlockType.IGNORED, "Documento Markdown vacío", "markdown-empty",
                    Map.of("sourceMode", "read-only")));
        }
        issues.add(DocumentImportIssue.info("read-only-source", "Markdown importado como documento fuente solo lectura; no se confunde con contratos internos especiales."));
        return new ReadableDocument(PlainTextDocumentImporter.titleFrom(sourceFile), SourceDocumentFormat.MARKDOWN, sourceFile, blocks, new DocumentImportReport(issues));
    }

    private static List<DocumentBlock> blocksFromMarkdown(String markdown) {
        List<DocumentBlock> blocks = new ArrayList<>();
        String[] paragraphs = markdown == null ? new String[0] : markdown.replace("\r\n", "\n").split("\n\\s*\n");
        int index = 1;
        for (String paragraph : paragraphs) {
            String normalized = paragraph == null ? "" : paragraph.strip();
            if (normalized.isBlank()) {
                continue;
            }
            DocumentBlockType type = typeFor(normalized, index);
            String clean = cleanMarkdownPrefix(normalized);
            blocks.add(DocumentBlock.of(PlainTextDocumentImporter.blockId(index), type, clean, "Markdown", Map.of(
                    "sourceMode", "read-only",
                    "sourceFormat", SourceDocumentFormat.MARKDOWN.name(),
                    "markdownBlock", Boolean.toString(true))));
            index++;
        }
        return blocks;
    }

    private static DocumentBlockType typeFor(String text, int index) {
        if (looksLikeMathBlock(text)) {
            return DocumentBlockType.MATH_NOTICE;
        }
        if (looksLikeMarkdownTable(text)) {
            return DocumentBlockType.TABLE_NOTICE;
        }
        if (text.startsWith("# ")) {
            return DocumentBlockType.TITLE;
        }
        if (text.startsWith("## ")) {
            return DocumentBlockType.HEADING;
        }
        if (text.startsWith("### ")) {
            return DocumentBlockType.SUBHEADING;
        }
        if (text.startsWith("- ") || text.startsWith("* ") || text.matches("\\d+\\.\\s+.*")) {
            return DocumentBlockType.LIST_ITEM;
        }
        return index == 1 ? DocumentBlockType.TITLE : DocumentBlockType.PARAGRAPH;
    }

    private static String cleanMarkdownPrefix(String text) {
        String normalized = text.strip();
        if (looksLikeMathBlock(normalized)) {
            return "Bloque matemático/fórmula detectado en Markdown; se identifica sin leer código LaTeX en voz.";
        }
        if (looksLikeMarkdownTable(normalized)) {
            return "Tabla Markdown detectada como bloque visual fuente; no se narra su código.";
        }
        if (normalized.startsWith("### ")) {
            return normalized.substring(4).strip();
        }
        if (normalized.startsWith("## ")) {
            return normalized.substring(3).strip();
        }
        if (normalized.startsWith("# ")) {
            return normalized.substring(2).strip();
        }
        if (normalized.startsWith("- ") || normalized.startsWith("* ")) {
            return normalized.substring(2).strip();
        }
        return normalized.replaceFirst("^\\d+\\.\\s+", "").strip();
    }
    private static boolean looksLikeMarkdownTable(String text) {
        String normalized = text == null ? "" : text.strip();
        if (!normalized.contains("|")) {
            return false;
        }
        String[] lines = normalized.split("\n");
        if (lines.length < 2) {
            return false;
        }
        boolean hasSeparator = java.util.Arrays.stream(lines)
                .map(String::strip)
                .anyMatch(line -> line.matches("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?$"));
        return hasSeparator;
    }

    private static boolean looksLikeMathBlock(String text) {
        String normalized = text == null ? "" : text.strip();
        String lower = normalized.toLowerCase(Locale.ROOT);
        return normalized.startsWith("$$")
                || normalized.startsWith("\\[")
                || lower.startsWith("```math")
                || lower.startsWith("```latex")
                || lower.contains("\\begin{equation}")
                || lower.contains("\\begin{align}");
    }

}
