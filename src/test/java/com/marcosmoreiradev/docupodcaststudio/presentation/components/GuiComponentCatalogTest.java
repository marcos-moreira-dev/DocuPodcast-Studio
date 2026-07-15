package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiComponentCatalogTest {
    @Test
    void officialCatalogFreezesCurrentAndFutureGuiBuildingBlocks() {
        GuiComponentCatalog catalog = GuiComponentCatalog.official();
        Set<String> names = catalog.components().stream()
                .map(GuiComponentContract::componentName)
                .collect(Collectors.toSet());

        assertTrue(names.contains("ActionButtonFactory"));
        assertTrue(names.contains("SectionHeader"));
        assertTrue(names.contains("FloatingReadingControlBar"));
        assertTrue(names.contains("TransportControls"));
        assertTrue(names.contains("MediaThumbnailCard"));
        assertTrue(names.contains("CollapsibleMediaRail"));
        assertTrue(names.contains("SettingsPageView"));
        assertTrue(names.contains("RibbonView"));
        assertTrue(names.contains("RibbonButton"));
        assertTrue(names.contains("RibbonGroup"));
        assertTrue(names.contains("SidebarIconTab"));
        assertTrue(names.contains("RailToggleButton"));
        assertTrue(names.contains("SidePanelToggleButton"));
        assertTrue(names.contains("DocumentSidePanelChrome"));
        assertTrue(names.contains("CollapsibleModuleSplitPane"));
        assertTrue(names.contains("StatusBarView"));
        assertTrue(names.contains("ReadingZoomControl"));
        assertTrue(names.contains("LongProcessOverlayView"));
        assertTrue(names.contains("SourceTableGridView"));
    }

    @Test
    void catalogDeclaresOwnerSurfaceAndGuardsAgainstMisuse() {
        GuiComponentCatalog catalog = GuiComponentCatalog.official();
        GuiComponentContract playbar = catalog.findByClassName("FloatingReadingControlBar").orElseThrow();
        GuiComponentContract mediaRail = catalog.findByClassName("MediaThumbnailCard").orElseThrow();
        GuiComponentContract ribbonView = catalog.findByClassName("RibbonView").orElseThrow();
        GuiComponentContract sidebarTab = catalog.findByClassName("SidebarIconTab").orElseThrow();
        GuiComponentContract zoom = catalog.findByClassName("ReadingZoomControl").orElseThrow();
        GuiComponentContract processOverlay = catalog.findByClassName("LongProcessOverlayView").orElseThrow();

        assertEquals(GuiComponentSurface.FLOATING_PLAYBAR, playbar.primarySurface());
        assertTrue(playbar.canBeUsedOn(GuiComponentSurface.WORKSPACE));
        assertFalse(playbar.canBeUsedOn(GuiComponentSurface.MENU_BAR));

        assertEquals(GuiComponentSurface.RIGHT_MEDIA_RAIL, mediaRail.primarySurface());
        assertTrue(mediaRail.forbiddenUse().contains("panel de asignación"));

        assertEquals(GuiComponentSurface.RIBBON, ribbonView.primarySurface());
        assertTrue(ribbonView.forbiddenUse().contains("menú duplicado"));

        assertEquals(GuiComponentSurface.LEFT_SIDEBAR, sidebarTab.primarySurface());
        assertTrue(sidebarTab.forbiddenUse().contains("Det/Aud/Img"));

        assertEquals(GuiComponentSurface.STATUS_BAR, zoom.primarySurface());
        assertTrue(zoom.forbiddenUse().contains("canvas"));

        assertEquals(GuiComponentSurface.PROCESS_OVERLAY, processOverlay.primarySurface());
        assertTrue(processOverlay.forbiddenUse().contains("Audio Jobs"));
    }
}
