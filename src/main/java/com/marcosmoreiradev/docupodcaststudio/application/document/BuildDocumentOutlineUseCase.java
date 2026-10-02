package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds navigation exclusively for DOCX, TXT and Markdown block documents. */
public final class BuildDocumentOutlineUseCase {
    public static final int FLAT_NAVIGATION_LIMIT = 180;
    private static final int MIN_INFERRED_ENTRIES = 3;
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(?<number>\\d+(?:\\.\\d+){0,3})\\s+(?<title>\\S.{1,150})$");
    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "(?i)^(chapter\\s+\\d+|cap[ií]tulo\\s+\\d+|appendix\\s+[a-z]|preface|bibliography|references)(?:\\b.*)?$");

    public DocumentOutlineProjection build(ReadableDocument document) {
        if (document == null) return empty();
        if (document.format() == SourceDocumentFormat.PDF) {
            throw new IllegalArgumentException("PDF outline must use BuildPdfEnhancedOutlineUseCase");
        }
        if (document.hasStructuralHeadings()) return fromStructuralHeadings(document);
        DocumentOutlineProjection inferred = fromInferredHeadings(document);
        if (inferred.indexedEntryCount() >= MIN_INFERRED_ENTRIES) return inferred;
        return flat(document);
    }

    private static DocumentOutlineProjection fromStructuralHeadings(ReadableDocument document) {
        ArrayList<OutlineNode> nodes = new ArrayList<>();
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            int level = structuralLevel(block.type());
            if (level > 0) nodes.add(new OutlineNode("heading-" + i, level, block, i));
        }
        return projection(DocumentOutlineOrigin.HEADINGS,
                "Índice del documento",
                nodes.size() + " títulos y secciones del documento.",
                nodes);
    }

    private static DocumentOutlineProjection fromInferredHeadings(ReadableDocument document) {
        ArrayList<OutlineNode> nodes = new ArrayList<>();
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            String text = normalized(block.text());
            if (!candidate(block, text)) continue;
            int level = inferredLevel(text);
            if (level > 0) nodes.add(new OutlineNode("inferred-" + i, level, block, i));
        }
        return projection(DocumentOutlineOrigin.INFERRED_SECTIONS,
                "Índice inferido",
                nodes.size() + " secciones inferidas desde el texto.",
                nodes);
    }

    private static DocumentOutlineProjection flat(ReadableDocument document) {
        ArrayList<DocumentOutlineEntry> entries = new ArrayList<>();
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size() && entries.size() < FLAT_NAVIGATION_LIMIT; i++) {
            DocumentBlock block = blocks.get(i);
            if (!block.narratable() && !block.sourceVisual()
                    && block.type() != DocumentBlockType.LIST_ITEM) continue;
            entries.add(entry("block-" + i, block, i, List.of()));
        }
        return new DocumentOutlineProjection(DocumentOutlineOrigin.FLAT,
                "Índice del documento",
                "Navegación plana por bloques disponibles.",
                entries, entries.size(), FLAT_NAVIGATION_LIMIT);
    }

    private static DocumentOutlineProjection projection(DocumentOutlineOrigin origin,
                                                        String title,
                                                        String detail,
                                                        List<OutlineNode> nodes) {
        TreeBuilder root = new TreeBuilder(DocumentOutlineEntry.message("root", "Documento"));
        Deque<TreeBuilder> stack = new ArrayDeque<>();
        stack.push(root);
        for (OutlineNode node : nodes) {
            int level = Math.min(3, Math.max(1, node.level()));
            while (stack.size() > level) stack.pop();
            TreeBuilder child = new TreeBuilder(entry(node.id(), node.block(),
                    node.sourceIndex(), List.of()));
            stack.peek().children.add(child);
            stack.push(child);
        }
        List<DocumentOutlineEntry> entries = root.children.stream().map(TreeBuilder::build).toList();
        return new DocumentOutlineProjection(origin, title, detail, entries,
                nodes.size(), FLAT_NAVIGATION_LIMIT);
    }

    private static DocumentOutlineEntry entry(String id, DocumentBlock block,
                                              int sourceIndex,
                                              List<DocumentOutlineEntry> children) {
        return new DocumentOutlineEntry(id, block.id(), block.type(),
                normalized(block.text()), block.metadata().getOrDefault("sourcePage", ""),
                sourceIndex, children);
    }

    private static boolean candidate(DocumentBlock block, String text) {
        return block != null && block.type() != DocumentBlockType.IGNORED
                && !text.isBlank() && text.length() <= 160
                && text.split("\\s+").length <= 18 && !text.contains("://");
    }

    private static int inferredLevel(String text) {
        if (CHAPTER_HEADING.matcher(text).matches()) return 1;
        Matcher numbered = NUMBERED_HEADING.matcher(text);
        if (!numbered.matches()) return -1;
        String title = numbered.group("title");
        if (title.endsWith(".") || title.endsWith(";") || title.endsWith(",")) return -1;
        return Math.min(3, 1 + (int) numbered.group("number").chars()
                .filter(ch -> ch == '.').count());
    }

    private static int structuralLevel(DocumentBlockType type) {
        return switch (type) {
            case TITLE -> 1;
            case HEADING -> 2;
            case SUBHEADING -> 3;
            default -> -1;
        };
    }

    private static String normalized(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").strip();
    }

    private static DocumentOutlineProjection empty() {
        return new DocumentOutlineProjection(DocumentOutlineOrigin.FLAT,
                "Índice del documento", "", List.of(), 0, FLAT_NAVIGATION_LIMIT);
    }

    private record OutlineNode(String id, int level, DocumentBlock block, int sourceIndex) {
    }

    private static final class TreeBuilder {
        private final DocumentOutlineEntry entry;
        private final List<TreeBuilder> children = new ArrayList<>();

        private TreeBuilder(DocumentOutlineEntry entry) {
            this.entry = entry;
        }

        private DocumentOutlineEntry build() {
            return entry.withChildren(children.stream().map(TreeBuilder::build).toList());
        }
    }
}
