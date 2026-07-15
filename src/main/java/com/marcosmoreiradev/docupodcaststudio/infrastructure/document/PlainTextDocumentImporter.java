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

/** Imports plain TXT files as read-only source documents. */
public final class PlainTextDocumentImporter implements DocumentImporter {
    @Override
    public boolean supports(Path sourceFile) {
        return hasExtension(sourceFile, ".txt");
    }

    @Override
    public ReadableDocument importDocument(Path sourceFile) throws IOException {
        String text = Files.readString(sourceFile, StandardCharsets.UTF_8);
        List<DocumentBlock> blocks = blocksFromParagraphs(text, SourceDocumentFormat.TXT);
        List<DocumentImportIssue> issues = new ArrayList<>();
        if (blocks.isEmpty()) {
            issues.add(DocumentImportIssue.warning("empty-text", "El TXT no contiene texto narrable."));
            blocks = List.of(DocumentBlock.of("B0001", DocumentBlockType.IGNORED, "Documento TXT vacío", "txt-empty",
                    Map.of("sourceMode", "read-only")));
        }
        issues.add(DocumentImportIssue.info("read-only-source", "TXT importado como fuente solo lectura; las capas viven en el proyecto."));
        return new ReadableDocument(titleFrom(sourceFile), SourceDocumentFormat.TXT, sourceFile, blocks, new DocumentImportReport(issues));
    }

    static List<DocumentBlock> blocksFromParagraphs(String text, SourceDocumentFormat format) {
        List<DocumentBlock> blocks = new ArrayList<>();
        String[] paragraphs = text == null ? new String[0] : text.replace("\r\n", "\n").split("\n\\s*\n");
        int index = 1;
        for (String paragraph : paragraphs) {
            String normalized = paragraph == null ? "" : paragraph.strip();
            if (normalized.isBlank()) {
                continue;
            }
            DocumentBlockType type = index == 1 ? DocumentBlockType.TITLE : DocumentBlockType.PARAGRAPH;
            blocks.add(DocumentBlock.of(blockId(index), type, normalized, format.displayName(),
                    Map.of("sourceMode", "read-only", "sourceFormat", format.name())));
            index++;
        }
        return blocks;
    }

    static String blockId(int index) {
        return "B" + String.format(Locale.ROOT, "%04d", index);
    }

    static String titleFrom(Path sourceFile) {
        if (sourceFile == null || sourceFile.getFileName() == null) {
            return "Documento";
        }
        String fileName = sourceFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    static boolean hasExtension(Path sourceFile, String extension) {
        if (sourceFile == null || sourceFile.getFileName() == null) {
            return false;
        }
        return sourceFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }
}
