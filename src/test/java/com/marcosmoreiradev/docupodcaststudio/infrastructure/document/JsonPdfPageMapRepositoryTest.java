package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfPageMapUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JsonPdfPageMapRepositoryTest {
    @TempDir Path temp;

    @Test
    void roundTripsSidecarAndLeavesV3BytesUntouched() throws Exception {
        Path v3 = temp.resolve("document/pages/page-000001.json");
        Files.createDirectories(v3.getParent());
        byte[] original = "{\"v3\":\"intacto\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(v3, original);
        PdfRegion region = new PdfRegion("R1", 1, 10, 20, 300, 60, 0, 0,
                "Texto lateral con ñ.", PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE,
                List.of(), new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.9,
                "e", "p", "g", "c"), PdfRegionOverride.empty(), Map.of(), 1);
        PreparedPdfPage page = new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1, List.of(region), List.of(), "");
        var pageMap = new BuildPdfPageMapUseCase().build(page, null);
        JsonPdfPageMapRepository repository = new JsonPdfPageMapRepository();

        repository.savePage(temp, "sha256-source", pageMap);

        assertEquals(pageMap, repository.loadPage(temp, 1).orElseThrow());
        assertEquals(List.of(1), repository.loadManifest(temp).orElseThrow().pageNumbers());
        assertArrayEquals(original, Files.readAllBytes(v3));
        assertTrue(Files.notExists(temp.resolve("document/page-maps/page-000001.json.tmp")));
    }
}
