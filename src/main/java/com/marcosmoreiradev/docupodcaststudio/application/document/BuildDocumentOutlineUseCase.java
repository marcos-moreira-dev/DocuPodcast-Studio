package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds a robust, read-only navigation projection for large documents. */
public final class BuildDocumentOutlineUseCase {
    public static final int FLAT_NAVIGATION_LIMIT = 180;
    private static final int CONTENTS_SCAN_PAGE_LIMIT = 30;
    private static final int MIN_BOOKMARK_ENTRIES = 2;
    private static final int MIN_CONTENTS_ENTRIES = 4;
    private static final int MIN_INFERRED_ENTRIES = 3;
    private static final int MIN_PDF_PAGE_ANCHORS = 2;
    private static final int MAX_HEADING_TEXT_LENGTH = 160;
    private static final int MAX_CONTENTS_ENTRY_LENGTH = 180;
    private static final Pattern CONTENTS_TITLE = Pattern.compile("(?i)^(contents|table\\s+of\\s+contents)$");
    private static final Pattern CONTENTS_ENTRY = Pattern.compile(
            "^(?:(?<number>\\d+(?:\\.\\d+){0,3})\\s+)?(?<title>.+?)\\s+(?<page>\\d{1,4}|[ivxlcdm]{1,12})$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_ONLY = Pattern.compile("^(\\d{1,4}|[ivxlcdm]{1,12})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIXED_HEADING = Pattern.compile(
            "^(?<prefix>\\d+(?:\\.\\d+){0,3})\\s+(?<title>\\S.{1,150})$");
    private static final Pattern CHAPTER_HEADING = Pattern.compile("(?i)^(chapter\\s+\\d+|appendix\\s+[a-z]|preface|bibliography|references|index)(?:\\b.*)?$");

    private final DocumentOutlineHintProvider hintProvider;

    public BuildDocumentOutlineUseCase() {
        this(DocumentOutlineHintProvider.NONE);
    }

    public BuildDocumentOutlineUseCase(DocumentOutlineHintProvider hintProvider) {
        this.hintProvider = hintProvider == null ? DocumentOutlineHintProvider.NONE : hintProvider;
    }

    public DocumentOutlineProjection build(ReadableDocument document) {
        if (document == null) {
            return DocumentOutlineProjection.empty();
        }
        DocumentIndex index = DocumentIndex.from(document);
        if (document.format() == SourceDocumentFormat.PDF) {
            DocumentOutlineProjection bookmarks = buildFromBookmarks(document, index);
            if (bookmarks.indexedEntryCount() >= MIN_BOOKMARK_ENTRIES) {
                return bookmarks;
            }
            DocumentOutlineProjection contents = buildFromContents(document, index);
            if (contents.indexedEntryCount() >= MIN_CONTENTS_ENTRIES) {
                return contents;
            }
        }
        if (document.hasStructuralHeadings()) {
            return buildFromExistingHeadings(document);
        }
        DocumentOutlineProjection inferred = buildFromInferredSections(document);
        if (inferred.indexedEntryCount() >= MIN_INFERRED_ENTRIES
                && (document.format() != SourceDocumentFormat.PDF || reliablePdfInferredSections(inferred, index))) {
            return inferred;
        }
        if (document.format() == SourceDocumentFormat.PDF) {
            DocumentOutlineProjection pageAnchors = buildFromPdfPages(index);
            if (pageAnchors.indexedEntryCount() >= MIN_PDF_PAGE_ANCHORS) {
                return pageAnchors;
            }
        }
        if (inferred.indexedEntryCount() >= MIN_INFERRED_ENTRIES) {
            return inferred;
        }
        return buildFlat(document);
    }

    private DocumentOutlineProjection buildFromBookmarks(ReadableDocument document, DocumentIndex index) {
        List<DocumentOutlineHint> hints = safeHints(document).stream()
                .filter(hint -> hint.origin() == DocumentOutlineOrigin.PDF_BOOKMARKS)
                .toList();
        if (hints.isEmpty()) {
            return emptyFor(DocumentOutlineOrigin.PDF_BOOKMARKS);
        }
        List<OutlineNode> nodes = new ArrayList<>();
        int ordinal = 0;
        for (DocumentOutlineHint hint : hints) {
            Optional<BlockRef> block = index.closestBlockForPage(hint.sourcePage());
            if (block.isEmpty()) {
                continue;
            }
            nodes.add(new OutlineNode("bookmark-" + ordinal++, hint.level(), block.get(), labelFor("Bookmark", hint.title(), hint.sourcePage())));
        }
        return projection(
                DocumentOutlineOrigin.PDF_BOOKMARKS,
                "Indice del documento",
                "%d entradas desde bookmarks PDF. Clic para saltar sin modificar la fuente.".formatted(nodes.size()),
                nodes);
    }

    private DocumentOutlineProjection buildFromContents(ReadableDocument document, DocumentIndex index) {
        List<ContentsLine> lines = contentsCandidateLines(document.blocks());
        int marker = firstContentsMarker(lines);
        if (marker < 0) {
            return emptyFor(DocumentOutlineOrigin.CONTENTS);
        }
        int contentsPage = lines.get(marker).sourcePageNumber();
        List<ContentsEntry> entries = parseContentsEntries(lines.subList(marker + 1, lines.size()));
        if (entries.isEmpty()) {
            return emptyFor(DocumentOutlineOrigin.CONTENTS);
        }
        List<OutlineNode> nodes = new ArrayList<>();
        int ordinal = 0;
        for (ContentsEntry entry : entries) {
            Optional<BlockRef> block = index.findAfterPage(entry.matchText(), contentsPage)
                    .or(() -> index.closestBlockForPage(entry.printedPage()));
            if (block.isEmpty()) {
                continue;
            }
            nodes.add(new OutlineNode(
                    "contents-" + ordinal++,
                    entry.level(),
                    block.get(),
                    labelFor("Contenido", entry.displayText(), block.get().sourcePage())));
        }
        return projection(
                DocumentOutlineOrigin.CONTENTS,
                "Indice del documento",
                "%d entradas desde tabla de contenidos. Clic para saltar sin modificar la fuente.".formatted(nodes.size()),
                nodes);
    }

    private DocumentOutlineProjection buildFromExistingHeadings(ReadableDocument document) {
        List<OutlineNode> nodes = new ArrayList<>();
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            int level = structuralLevel(block.type());
            if (level < 1) {
                continue;
            }
            nodes.add(new OutlineNode(
                    "heading-" + i,
                    level,
                    new BlockRef(block, i),
                    labelFor(prefixFor(block.type()), block.preview(96), sourcePage(block))));
        }
        return projection(
                DocumentOutlineOrigin.HEADINGS,
                "Indice del documento",
                "%d titulos/secciones detectadas. Clic para saltar sin modificar la fuente.".formatted(nodes.size()),
                nodes);
    }

    private DocumentOutlineProjection buildFromInferredSections(ReadableDocument document) {
        List<OutlineNode> nodes = new ArrayList<>();
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            Optional<InferredHeading> heading = inferHeading(block);
            if (heading.isEmpty()) {
                continue;
            }
            nodes.add(new OutlineNode(
                    "inferred-" + i,
                    heading.get().level(),
                    new BlockRef(block, i),
                    labelFor(heading.get().prefix(), block.preview(96), sourcePage(block))));
        }
        return projection(
                DocumentOutlineOrigin.INFERRED_SECTIONS,
                "Indice del documento",
                "%d secciones inferidas por patrones de libro tecnico. Clic para saltar sin modificar la fuente.".formatted(nodes.size()),
                nodes);
    }

    private DocumentOutlineProjection buildFlat(ReadableDocument document) {
        List<DocumentOutlineEntry> entries = new ArrayList<>();
        int indexed = 0;
        List<DocumentBlock> blocks = document.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            if (!flatNavigable(block)) {
                continue;
            }
            entries.add(entryFromBlock("flat-" + i, block, i, labelFor(prefixFor(block.type()), block.preview(96), sourcePage(block)), List.of()));
            indexed++;
            if (indexed >= FLAT_NAVIGATION_LIMIT) {
                entries.add(DocumentOutlineEntry.message(
                        "flat-more",
                        "Mas bloques disponibles - el lector carga el bloque exacto al buscarlo o reproducirlo; el indice plano muestra los primeros " + FLAT_NAVIGATION_LIMIT + "."));
                break;
            }
        }
        if (entries.isEmpty()) {
            entries.add(DocumentOutlineEntry.message("flat-empty", "Sin bloques navegables - el documento no expuso titulos ni bloques narrables para indexar."));
        }
        return new DocumentOutlineProjection(
                DocumentOutlineOrigin.FLAT,
                "Indice del documento",
                flatNavigationLabel(document),
                entries,
                indexed,
                FLAT_NAVIGATION_LIMIT);
    }

    private DocumentOutlineProjection buildFromPdfPages(DocumentIndex index) {
        List<PageAnchor> anchors = index.pageAnchors();
        if (anchors.isEmpty()) {
            return emptyFor(DocumentOutlineOrigin.PDF_PAGES);
        }
        List<DocumentOutlineEntry> entries = new ArrayList<>();
        for (PageAnchor anchor : anchors) {
            entries.add(entryFromBlock(
                    "pdf-page-" + anchor.page(),
                    anchor.blockRef().block(),
                    anchor.blockRef().sourceIndex(),
                    "Pagina " + anchor.page(),
                    List.of()));
        }
        int indexed = entries.size();
        return new DocumentOutlineProjection(
                DocumentOutlineOrigin.PDF_PAGES,
                "Indice del documento",
                "No se encontro temario confiable; se muestran " + indexed
                        + " paginas del PDF para navegar sin modificar la fuente.",
                entries,
                indexed,
                Math.max(FLAT_NAVIGATION_LIMIT, indexed));
    }

    private static boolean reliablePdfInferredSections(DocumentOutlineProjection inferred, DocumentIndex index) {
        int count = inferred == null ? 0 : inferred.indexedEntryCount();
        if (count < MIN_INFERRED_ENTRIES) {
            return false;
        }
        List<DocumentOutlineEntry> entries = flattenEntries(inferred.entries());
        if (bibliographicNoiseDominates(entries)) {
            return false;
        }
        int pageCount = index.sourcePageCount();
        if (pageCount >= 80) {
            int minimumForLargePdf = Math.min(24, Math.max(8, pageCount / 60));
            if (count < minimumForLargePdf) {
                return false;
            }
            int minPage = entries.stream().map(DocumentOutlineEntry::sourcePage)
                    .map(BuildDocumentOutlineUseCase::parsePositiveInt)
                    .flatMap(Optional::stream)
                    .min(Integer::compareTo)
                    .orElse(0);
            int maxPage = entries.stream().map(DocumentOutlineEntry::sourcePage)
                    .map(BuildDocumentOutlineUseCase::parsePositiveInt)
                    .flatMap(Optional::stream)
                    .max(Integer::compareTo)
                    .orElse(0);
            if (minPage > 0 && maxPage > 0 && maxPage - minPage < Math.max(6, pageCount / 20)) {
                return false;
            }
        }
        return true;
    }

    private static List<DocumentOutlineEntry> flattenEntries(List<DocumentOutlineEntry> entries) {
        List<DocumentOutlineEntry> flattened = new ArrayList<>();
        collectEntries(entries, flattened);
        return flattened;
    }

    private static void collectEntries(List<DocumentOutlineEntry> entries, List<DocumentOutlineEntry> output) {
        for (DocumentOutlineEntry entry : entries == null ? List.<DocumentOutlineEntry>of() : entries) {
            if (entry.blockId() != null && !entry.blockId().isBlank()) {
                output.add(entry);
            }
            collectEntries(entry.children(), output);
        }
    }

    private static boolean bibliographicNoiseDominates(List<DocumentOutlineEntry> entries) {
        if (entries == null || entries.size() < MIN_INFERRED_ENTRIES) {
            return false;
        }
        long noisy = entries.stream()
                .map(DocumentOutlineEntry::label)
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT))
                .filter(BuildDocumentOutlineUseCase::bibliographicNoiseLabel)
                .count();
        return noisy >= Math.max(2, (entries.size() + 1) / 2);
    }

    private static boolean bibliographicNoiseLabel(String label) {
        return label.contains("bibliography")
                || label.contains("references")
                || label.contains("all rights reserved")
                || label.contains("copyright")
                || label.contains("cengage")
                || label.contains("prentice")
                || label.contains("publisher")
                || label.contains("may not be copied");
    }

    private List<DocumentOutlineHint> safeHints(ReadableDocument document) {
        try {
            List<DocumentOutlineHint> hints = hintProvider.hintsFor(document);
            return hints == null ? List.of() : List.copyOf(hints);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static DocumentOutlineProjection projection(DocumentOutlineOrigin origin, String title, String detail, List<OutlineNode> nodes) {
        return new DocumentOutlineProjection(origin, title, detail, treeFromNodes(nodes), nodes.size(), FLAT_NAVIGATION_LIMIT);
    }

    private static DocumentOutlineProjection emptyFor(DocumentOutlineOrigin origin) {
        return new DocumentOutlineProjection(origin, "Indice del documento", "", List.of(), 0, FLAT_NAVIGATION_LIMIT);
    }

    private static List<DocumentOutlineEntry> treeFromNodes(List<OutlineNode> nodes) {
        int minLevel = nodes.stream().mapToInt(OutlineNode::level).min().orElse(1);
        TreeBuilder root = new TreeBuilder(DocumentOutlineEntry.message("root", "Documento"));
        Deque<TreeBuilder> stack = new ArrayDeque<>();
        stack.push(root);
        for (OutlineNode node : nodes) {
            int level = Math.min(3, Math.max(1, node.level() - minLevel + 1));
            while (stack.size() > level) {
                stack.pop();
            }
            TreeBuilder item = new TreeBuilder(entryFromBlock(
                    node.id(),
                    node.blockRef().block(),
                    node.blockRef().sourceIndex(),
                    node.label(),
                    List.of()));
            stack.peek().children.add(item);
            stack.push(item);
        }
        return root.children.stream().map(TreeBuilder::build).toList();
    }

    private static DocumentOutlineEntry entryFromBlock(String id, DocumentBlock block, int sourceIndex, String label, List<DocumentOutlineEntry> children) {
        return new DocumentOutlineEntry(id, block.id(), block.type(), label, sourcePage(block), sourceIndex, children);
    }

    private static List<ContentsLine> contentsCandidateLines(List<DocumentBlock> blocks) {
        List<ContentsLine> lines = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            DocumentBlock block = blocks.get(i);
            int page = sourcePageNumber(block);
            if (page > CONTENTS_SCAN_PAGE_LIMIT) {
                continue;
            }
            String text = block.text();
            if (text.isBlank()) {
                continue;
            }
            for (String rawLine : text.split("\\R+")) {
                String line = rawLine.strip();
                if (!line.isBlank()) {
                    lines.add(new ContentsLine(line, i, page));
                }
            }
        }
        return lines;
    }

    private static int firstContentsMarker(List<ContentsLine> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (CONTENTS_TITLE.matcher(lines.get(i).text()).matches()) {
                return i;
            }
        }
        return -1;
    }

    private static List<ContentsEntry> parseContentsEntries(List<ContentsLine> lines) {
        List<ContentsEntry> entries = new ArrayList<>();
        String pending = "";
        for (ContentsLine line : lines) {
            String text = normalizeContentsLine(line.text());
            if (text.isBlank() || CONTENTS_TITLE.matcher(text).matches()) {
                continue;
            }
            Matcher matcher = CONTENTS_ENTRY.matcher(text);
            if (matcher.matches()) {
                addContentsEntry(entries, matcher.group("number"), matcher.group("title"), matcher.group("page"));
                pending = "";
                continue;
            }
            Matcher pageOnly = PAGE_ONLY.matcher(text);
            if (!pending.isBlank() && pageOnly.matches()) {
                addContentsEntry(entries, "", pending, pageOnly.group(1));
                pending = "";
                continue;
            }
            if (plausibleContentsFragment(text)) {
                pending = pending.isBlank() ? text : pending + " " + text;
                if (pending.length() > MAX_CONTENTS_ENTRY_LENGTH) {
                    pending = "";
                }
            } else {
                pending = "";
            }
        }
        return entries;
    }

    private static void addContentsEntry(List<ContentsEntry> entries, String number, String title, String page) {
        String cleanNumber = number == null ? "" : number.strip();
        String cleanTitle = title == null ? "" : title.strip();
        if (cleanNumber.isBlank()) {
            Matcher prefixed = PREFIXED_HEADING.matcher(cleanTitle);
            if (prefixed.matches()) {
                cleanNumber = prefixed.group("prefix");
                cleanTitle = prefixed.group("title").strip();
            }
        }
        if (cleanTitle.isBlank() || cleanTitle.length() > MAX_CONTENTS_ENTRY_LENGTH) {
            return;
        }
        String display = cleanNumber.isBlank() ? cleanTitle : cleanNumber + " " + cleanTitle;
        entries.add(new ContentsEntry(display, normalizeForMatch(display), levelFromNumber(cleanNumber), page == null ? "" : page.strip()));
    }

    private static String normalizeContentsLine(String line) {
        return line.replaceAll("\\.{2,}", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static boolean plausibleContentsFragment(String text) {
        if (text.length() > MAX_CONTENTS_ENTRY_LENGTH || text.length() < 3) {
            return false;
        }
        if (text.endsWith(".") && text.split("\\s+").length > 12) {
            return false;
        }
        return text.matches("(?i)^(\\d+(?:\\.\\d+){0,3}\\s+)?[\\p{L}\\d].*");
    }

    private static Optional<InferredHeading> inferHeading(DocumentBlock block) {
        if (!candidateHeadingBlock(block)) {
            return Optional.empty();
        }
        String text = block.text().replaceAll("\\s+", " ").strip();
        Matcher chapter = CHAPTER_HEADING.matcher(text);
        if (chapter.matches()) {
            return Optional.of(new InferredHeading(1, "Seccion"));
        }
        Matcher prefixed = PREFIXED_HEADING.matcher(text);
        if (!prefixed.matches()) {
            return Optional.empty();
        }
        String title = prefixed.group("title");
        if (title.endsWith(".") || title.endsWith(";") || title.endsWith(",")) {
            return Optional.empty();
        }
        int words = title.split("\\s+").length;
        if (words > 14) {
            return Optional.empty();
        }
        return Optional.of(new InferredHeading(levelFromNumber(prefixed.group("prefix")), "Seccion"));
    }

    private static boolean candidateHeadingBlock(DocumentBlock block) {
        if (block == null || block.text().isBlank()) {
            return false;
        }
        if (block.type() == DocumentBlockType.IGNORED) {
            return false;
        }
        String text = block.text().replaceAll("\\s+", " ").strip();
        if (text.length() > MAX_HEADING_TEXT_LENGTH || text.contains("://")) {
            return false;
        }
        return text.split("\\s+").length <= 18;
    }

    private static int structuralLevel(DocumentBlockType type) {
        return switch (type) {
            case TITLE -> 1;
            case HEADING -> 2;
            case SUBHEADING -> 3;
            default -> -1;
        };
    }

    private static int levelFromNumber(String number) {
        if (number == null || number.isBlank()) {
            return 1;
        }
        int dots = (int) number.chars().filter(ch -> ch == '.').count();
        return Math.min(3, dots + 1);
    }

    private static String labelFor(String prefix, String text, String sourcePage) {
        String page = sourcePage == null || sourcePage.isBlank() ? "" : " - p. " + sourcePage;
        return prefix + " - " + text + page;
    }

    private static String prefixFor(DocumentBlockType type) {
        return switch (type) {
            case TITLE -> "Titulo";
            case HEADING -> "Seccion";
            case SUBHEADING -> "Subseccion";
            case IMAGE_NOTICE -> "Imagen";
            case TABLE_NOTICE -> "Tabla";
            case MATH_NOTICE -> "Formula";
            case LIST_ITEM -> "Lista";
            default -> "Bloque";
        };
    }

    private static boolean flatNavigable(DocumentBlock block) {
        return block.narratable() || block.sourceVisual() || block.type() == DocumentBlockType.LIST_ITEM;
    }

    private static String flatNavigationLabel(ReadableDocument document) {
        return switch (document.format()) {
            case TXT -> "Origen del indice: navegacion plana. TXT sin encabezados; se muestran bloques narrables y visuales.";
            case MARKDOWN -> "Origen del indice: navegacion plana. No se detectaron encabezados; se muestran bloques disponibles.";
            default -> "Origen del indice: navegacion plana. No se detectaron titulos confiables; se muestran bloques disponibles.";
        };
    }

    private static String sourcePage(DocumentBlock block) {
        return block.metadata().getOrDefault("sourcePage", "").strip();
    }

    private static int sourcePageNumber(DocumentBlock block) {
        return parsePositiveInt(sourcePage(block)).orElse(Integer.MAX_VALUE);
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        if (value == null || value.isBlank() || !value.strip().matches("\\d+")) {
            return Optional.empty();
        }
        int parsed = Integer.parseInt(value.strip());
        return parsed > 0 ? Optional.of(parsed) : Optional.empty();
    }

    private static String normalizeForMatch(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9. ]", " ")
                .replaceAll("\\s+", " ")
                .strip();
        return normalized.length() > 120 ? normalized.substring(0, 120).strip() : normalized;
    }

    private record OutlineNode(String id, int level, BlockRef blockRef, String label) {
    }

    private record BlockRef(DocumentBlock block, int sourceIndex) {
        String sourcePage() {
            return BuildDocumentOutlineUseCase.sourcePage(block);
        }
    }

    private record ContentsLine(String text, int sourceIndex, int sourcePageNumber) {
    }

    private record ContentsEntry(String displayText, String matchText, int level, String printedPage) {
    }

    private record InferredHeading(int level, String prefix) {
    }

    private record PageAnchor(int page, BlockRef blockRef) {
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

    private static final class DocumentIndex {
        private final List<BlockRef> blocks;
        private final Map<Integer, BlockRef> firstBlockByPage;
        private final int sourcePageCount;

        private DocumentIndex(List<BlockRef> blocks, Map<Integer, BlockRef> firstBlockByPage, int sourcePageCount) {
            this.blocks = blocks;
            this.firstBlockByPage = firstBlockByPage;
            this.sourcePageCount = Math.max(0, sourcePageCount);
        }

        private int sourcePageCount() {
            return sourcePageCount;
        }

        private static DocumentIndex from(ReadableDocument document) {
            List<BlockRef> refs = new ArrayList<>();
            Map<Integer, BlockRef> byPage = new LinkedHashMap<>();
            int sourcePageCount = 0;
            List<DocumentBlock> blocks = document.blocks();
            for (int i = 0; i < blocks.size(); i++) {
                DocumentBlock block = blocks.get(i);
                sourcePageCount = Math.max(sourcePageCount,
                        parsePositiveInt(block.metadata().getOrDefault("sourcePageCount", "")).orElse(0));
                sourcePageCount = Math.max(sourcePageCount, parsePositiveInt(sourcePage(block)).orElse(0));
                if (!flatNavigable(block)) {
                    continue;
                }
                BlockRef ref = new BlockRef(block, i);
                refs.add(ref);
                parsePositiveInt(sourcePage(block)).ifPresent(page -> byPage.putIfAbsent(page, ref));
            }
            return new DocumentIndex(List.copyOf(refs), Map.copyOf(byPage), sourcePageCount);
        }

        private Optional<BlockRef> closestBlockForPage(String sourcePage) {
            Optional<Integer> page = parsePositiveInt(sourcePage);
            if (page.isEmpty() || firstBlockByPage.isEmpty()) {
                return Optional.empty();
            }
            BlockRef exact = firstBlockByPage.get(page.get());
            if (exact != null) {
                return Optional.of(exact);
            }
            return firstBlockByPage.entrySet().stream()
                    .filter(entry -> entry.getKey() >= page.get())
                    .min(Comparator.comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue);
        }

        private List<PageAnchor> pageAnchors() {
            if (sourcePageCount <= 0) {
                return firstBlockByPage.entrySet().stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .map(entry -> new PageAnchor(entry.getKey(), entry.getValue()))
                        .toList();
            }
            List<PageAnchor> anchors = new ArrayList<>();
            for (int page = 1; page <= sourcePageCount; page++) {
                int currentPage = page;
                closestBlockForPageIncludingPrevious(currentPage)
                        .ifPresent(block -> anchors.add(new PageAnchor(currentPage, block)));
            }
            return anchors;
        }

        private Optional<BlockRef> closestBlockForPageIncludingPrevious(int page) {
            if (page <= 0 || firstBlockByPage.isEmpty()) {
                return Optional.empty();
            }
            BlockRef exact = firstBlockByPage.get(page);
            if (exact != null) {
                return Optional.of(exact);
            }
            Optional<BlockRef> next = firstBlockByPage.entrySet().stream()
                    .filter(entry -> entry.getKey() >= page)
                    .min(Comparator.comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue);
            if (next.isPresent()) {
                return next;
            }
            return firstBlockByPage.entrySet().stream()
                    .filter(entry -> entry.getKey() <= page)
                    .max(Comparator.comparingInt(Map.Entry::getKey))
                    .map(Map.Entry::getValue);
        }

        private Optional<BlockRef> findAfterPage(String matchText, int afterPage) {
            String needle = normalizeForMatch(matchText);
            if (needle.isBlank()) {
                return Optional.empty();
            }
            String sectionPrefix = firstTokenIfNumbered(needle);
            List<String> meaningful = meaningfulSearchPhrases(needle);
            for (BlockRef ref : blocks) {
                int page = sourcePageNumber(ref.block());
                if (page != Integer.MAX_VALUE && page <= afterPage) {
                    continue;
                }
                String haystack = normalizeForMatch(ref.block().text());
                if (haystack.isBlank()) {
                    continue;
                }
                if (!sectionPrefix.isBlank() && haystack.startsWith(sectionPrefix + " ")) {
                    return Optional.of(ref);
                }
                for (String phrase : meaningful) {
                    if (phrase.length() >= 12 && haystack.contains(phrase)) {
                        return Optional.of(ref);
                    }
                }
            }
            return Optional.empty();
        }

        private static String firstTokenIfNumbered(String value) {
            int firstSpace = value.indexOf(' ');
            String first = firstSpace < 0 ? value : value.substring(0, firstSpace);
            return first.matches("\\d+(?:\\.\\d+){0,3}") ? first : "";
        }

        private static List<String> meaningfulSearchPhrases(String value) {
            String withoutNumber = value.replaceFirst("^\\d+(?:\\.\\d+){0,3}\\s+", "");
            List<String> phrases = new ArrayList<>();
            phrases.add(withoutNumber.length() > 72 ? withoutNumber.substring(0, 72).strip() : withoutNumber);
            String[] words = withoutNumber.split("\\s+");
            if (words.length > 3) {
                phrases.add(String.join(" ", java.util.Arrays.copyOf(words, Math.min(words.length, 5))));
            }
            return phrases.stream().filter(phrase -> !phrase.isBlank()).distinct().toList();
        }
    }
}
