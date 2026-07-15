package com.marcosmoreiradev.docupodcaststudio.domain.document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Normalized in-memory document shown by the Document workspace. */
public record ReadableDocument(
        String title,
        SourceDocumentFormat format,
        Path sourcePath,
        List<DocumentBlock> blocks,
        DocumentImportReport importReport
) {
    public ReadableDocument {
        title = normalizeTitle(title, sourcePath);
        format = Objects.requireNonNullElse(format, SourceDocumentFormat.UNKNOWN);
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        importReport = importReport == null ? DocumentImportReport.empty() : importReport;
    }

    public ReadableDocument(String title, SourceDocumentFormat format, Path sourcePath, List<DocumentBlock> blocks) {
        this(title, format, sourcePath, blocks, DocumentImportReport.empty());
    }

    public ReadableDocument(String title, SourceDocumentFormat format, Path sourcePath, List<DocumentBlock> blocks, List<DocumentImportIssue> issues) {
        this(title, format, sourcePath, blocks, new DocumentImportReport(issues));
    }

    public Optional<DocumentBlock> blockById(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return Optional.empty();
        }
        return blocks.stream().filter(block -> block.id().equals(blockId)).findFirst();
    }

    public ReadableDocument replaceBlock(DocumentBlock replacement) {
        Objects.requireNonNull(replacement, "replacement");
        List<DocumentBlock> next = new ArrayList<>(blocks.size());
        boolean replaced = false;
        for (DocumentBlock block : blocks) {
            if (block.id().equals(replacement.id())) {
                next.add(replacement);
                replaced = true;
            } else {
                next.add(block);
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("No existe el bloque " + replacement.id());
        }
        return withBlocks(next);
    }

    public ReadableDocument withBlockType(String blockId, DocumentBlockType newType, String reason) {
        DocumentBlock block = blockById(blockId).orElseThrow(() -> new IllegalArgumentException("No existe el bloque " + blockId));
        return replaceBlock(block.withType(newType, reason));
    }

    public ReadableDocument withBlocks(List<DocumentBlock> nextBlocks) {
        return new ReadableDocument(title, format, sourcePath, nextBlocks, importReport);
    }

    public long narratableBlockCount() {
        return blocks.stream().filter(DocumentBlock::narratable).count();
    }

    public long titleCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.TITLE).count();
    }

    public long headingCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.HEADING).count();
    }

    public long subheadingCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.SUBHEADING).count();
    }

    public long paragraphCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.PARAGRAPH).count();
    }

    public long listItemCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.LIST_ITEM).count();
    }

    public long imageNoticeCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.IMAGE_NOTICE).count();
    }

    public long tableNoticeCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.TABLE_NOTICE).count();
    }

    public long mathNoticeCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.MATH_NOTICE).count();
    }

    public long sourceVisualBlockCount() {
        return blocks.stream().filter(DocumentBlock::sourceVisual).count();
    }

    public long ignoredCount() {
        return blocks.stream().filter(block -> block.type() == DocumentBlockType.IGNORED).count();
    }

    public boolean hasStructuralHeadings() {
        return titleCount() + headingCount() + subheadingCount() > 0;
    }

    public long structuralBlockCount() {
        return titleCount() + headingCount() + subheadingCount();
    }

    public long warningCount() {
        return importReport.warningCount();
    }

    public long errorCount() {
        return importReport.errorCount();
    }

    public List<DocumentImportIssue> issues() {
        return importReport.issues();
    }

    public long wordCount() {
        return blocks.stream()
                .filter(DocumentBlock::narratable)
                .map(DocumentBlock::text)
                .flatMap(text -> java.util.Arrays.stream(text.strip().split("\\s+")))
                .filter(token -> !token.isBlank())
                .count();
    }

    private static String normalizeTitle(String title, Path sourcePath) {
        String normalized = title == null ? "" : title.strip();
        if (!normalized.isBlank()) {
            return normalized;
        }
        if (sourcePath != null && sourcePath.getFileName() != null) {
            String fileName = sourcePath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        return "Documento importado";
    }
}
