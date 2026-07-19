package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Builds bounded prompt context without summaries, inferred facts or external knowledge.
 * Every semantic sentence in the result comes directly from the imported Word document.
 */
public final class NarrativeDocumentContextCompiler {
    public static final int DEFAULT_MAX_CHARACTERS = 12_000;

    public NarrativeDocumentContext compile(ReadableDocument document, String blockId) {
        return compile(document, blockId, DEFAULT_MAX_CHARACTERS);
    }

    public NarrativeDocumentContext compile(ReadableDocument document, String blockId, int maxCharacters) {
        Objects.requireNonNull(document, "document");
        String targetId = blockId == null ? "" : blockId.strip();
        DocumentBlock target = document.blockById(targetId)
                .filter(this::isNarrativeParagraph)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El bloque " + targetId + " no es un parrafo narrable."));

        String normalizedDocument = normalizedDocumentText(document);
        int budget = Math.max(500, maxCharacters);
        List<DocumentBlock> blocks = document.blocks();
        int targetIndex = blocks.indexOf(target);
        LinkedHashSet<DocumentBlock> selected = new LinkedHashSet<>();
        addStructuralAncestors(blocks, targetIndex, selected);
        selected.add(target);

        List<DocumentBlock> relevant = blocks.stream()
                .filter(this::isNarrativeParagraph)
                .filter(block -> !block.id().equals(target.id()))
                .map(block -> new ScoredBlock(block, relevance(target.text(), block.text()),
                        Math.abs(blocks.indexOf(block) - targetIndex)))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredBlock::score).reversed()
                        .thenComparingInt(ScoredBlock::distance))
                .map(ScoredBlock::block)
                .toList();

        ArrayList<String> sections = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        appendBlock(sections, ids, target, "PARRAFO ACTUAL", budget);
        for (DocumentBlock block : selected) {
            if (block.id().equals(target.id())) {
                continue;
            }
            appendBlock(sections, ids, block, label(block.type()), budget);
        }
        for (DocumentBlock block : relevant) {
            if (selected.contains(block)) {
                continue;
            }
            if (!appendBlock(sections, ids, block, "EXTRACTO RELACIONADO", budget)) {
                break;
            }
        }
        return new NarrativeDocumentContext(
                fingerprint(normalizedDocument),
                normalizedDocument,
                target.id(),
                target.text(),
                String.join("\n\n", sections),
                ids);
    }

    public String normalizedDocumentText(ReadableDocument document) {
        Objects.requireNonNull(document, "document");
        StringBuilder out = new StringBuilder();
        for (DocumentBlock block : document.blocks()) {
            if (!includedAsTextSource(block)) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(block.type().name()).append('\t')
                    .append(block.id()).append('\t')
                    .append(normalizeWhitespace(block.text()));
        }
        return out.toString();
    }

    public String fingerprint(ReadableDocument document) {
        return fingerprint(normalizedDocumentText(document));
    }

    public List<DocumentBlock> narrativeParagraphs(ReadableDocument document) {
        if (document == null) {
            return List.of();
        }
        return document.blocks().stream().filter(this::isNarrativeParagraph).toList();
    }

    private boolean appendBlock(List<String> sections,
                                List<String> ids,
                                DocumentBlock block,
                                String label,
                                int budget) {
        String section = label + ":\n" + normalizeWhitespace(block.text());
        int used = sections.stream().mapToInt(String::length).sum() + Math.max(0, sections.size() - 1) * 2;
        if (used + section.length() > budget) {
            return false;
        }
        sections.add(section);
        ids.add(block.id());
        return true;
    }

    private void addStructuralAncestors(List<DocumentBlock> blocks,
                                        int targetIndex,
                                        Set<DocumentBlock> selected) {
        DocumentBlock title = null;
        DocumentBlock heading = null;
        DocumentBlock subheading = null;
        for (int index = 0; index < targetIndex; index++) {
            DocumentBlock block = blocks.get(index);
            switch (block.type()) {
                case TITLE -> title = block;
                case HEADING -> {
                    heading = block;
                    subheading = null;
                }
                case SUBHEADING -> subheading = block;
                default -> {
                }
            }
        }
        if (title != null) selected.add(title);
        if (heading != null) selected.add(heading);
        if (subheading != null) selected.add(subheading);
    }

    private int relevance(String target, String candidate) {
        Set<String> targetTerms = terms(target);
        Set<String> candidateTerms = terms(candidate);
        candidateTerms.retainAll(targetTerms);
        return candidateTerms.size();
    }

    private Set<String> terms(String text) {
        HashSet<String> terms = new HashSet<>();
        for (String token : normalizeWhitespace(text).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 5) {
                terms.add(token);
            }
        }
        return terms;
    }

    private boolean includedAsTextSource(DocumentBlock block) {
        return block != null && !block.text().isBlank()
                && switch (block.type()) {
                    case TITLE, HEADING, SUBHEADING, PARAGRAPH -> true;
                    default -> false;
                };
    }

    private boolean isNarrativeParagraph(DocumentBlock block) {
        return block != null && block.type() == DocumentBlockType.PARAGRAPH && block.narratable();
    }

    private String label(DocumentBlockType type) {
        return switch (type) {
            case TITLE -> "TITULO DEL DOCUMENTO";
            case HEADING -> "ENCABEZADO";
            case SUBHEADING -> "SUBENCABEZADO";
            default -> "TEXTO DEL DOCUMENTO";
        };
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    private record ScoredBlock(DocumentBlock block, int score, int distance) {
    }
}
