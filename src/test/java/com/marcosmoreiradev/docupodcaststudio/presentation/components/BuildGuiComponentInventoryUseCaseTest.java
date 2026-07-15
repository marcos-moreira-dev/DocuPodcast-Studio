package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildGuiComponentInventoryUseCaseTest {
    @Test
    void classifiesTransversalNarrativeDocumentAndLegacyComponents() {
        GuiComponentInventoryReport report = new BuildGuiComponentInventoryUseCase()
                .build(GuiComponentCatalog.official());

        assertTrue(report.total() > 0);
        assertEquals("especializado-narrativo",
                report.classifications().get("NarrativeVisualProductionWorkspaceView"));
        assertEquals("soporte-documental",
                report.classifications().get("DocumentSidePanelChrome"));
        assertEquals("legacy-interno",
                report.classifications().get("ToolbarActionButton"));
        assertTrue(report.transversalCount() > report.legacyInternalCount());
        assertTrue(report.classifications().containsKey("ExportCenterDialog"));
        assertTrue(report.classifications().containsKey("AudioInputDeviceSelector"));
    }
}
