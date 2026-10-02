package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryPreparedPdfDocumentRepository implements PreparedPdfDocumentRepository {
    private final Map<Path, PdfDocumentManifest> manifests = new LinkedHashMap<>();
    private final Map<Path, Map<Integer, PreparedPdfPage>> pages = new LinkedHashMap<>();

    @Override public void initialize(Path root, PdfDocumentManifest manifest) {
        manifests.put(normalize(root), manifest);
    }
    @Override public Optional<PdfDocumentManifest> loadManifest(Path root) {
        return Optional.ofNullable(manifests.get(normalize(root)));
    }
    @Override public void saveManifest(Path root, PdfDocumentManifest manifest) {
        manifests.put(normalize(root), manifest);
    }
    @Override public Optional<PreparedPdfPage> loadPage(Path root, int pageNumber) {
        return Optional.ofNullable(pages.getOrDefault(normalize(root), Map.of()).get(pageNumber));
    }
    @Override public List<PreparedPdfPage> loadPages(Path root) {
        ArrayList<PreparedPdfPage> result = new ArrayList<>(
                pages.getOrDefault(normalize(root), Map.of()).values());
        result.sort(Comparator.comparingInt(PreparedPdfPage::pageNumber));
        return List.copyOf(result);
    }
    @Override public void savePage(Path root, PreparedPdfPage page) {
        pages.computeIfAbsent(normalize(root), ignored -> new LinkedHashMap<>())
                .put(page.pageNumber(), page);
    }
    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
