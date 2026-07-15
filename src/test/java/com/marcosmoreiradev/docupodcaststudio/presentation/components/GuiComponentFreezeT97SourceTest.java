package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiComponentFreezeT97SourceTest {
    @Test
    void t97AddsReusablePrimitivesForRibbonSidebarAndRailBeforeStrongRedesign() throws Exception {
        String appStyles = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java");
        String actionsCss = read("src/main/resources/css/components/actions.css");
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/GuiComponentCatalog.java");

        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java").contains("UI_RIBBON_BUTTON"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonGroup.java").contains("UI_RIBBON_GROUP"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SidebarIconTab.java").contains("UI_SIDEBAR_ICON_TAB"));
        assertTrue(read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RailToggleButton.java").contains("UI_RAIL_TOGGLE_BUTTON"));

        assertTrue(appStyles.contains("UI_RIBBON_BUTTON"));
        assertTrue(appStyles.contains("UI_SIDEBAR_ICON_TAB"));
        assertTrue(appStyles.contains("UI_RAIL_TOGGLE_BUTTON"));
        assertTrue(actionsCss.contains(".ui-ribbon-button"));
        assertTrue(actionsCss.contains(".ui-sidebar-icon-tab"));
        assertTrue(actionsCss.contains(".ui-rail-toggle-button"));

        assertTrue(catalog.contains("No volver a etiquetas truncadas tipo Det/Aud/Img"));
        assertTrue(catalog.contains("No incrustar el rail como si fuera parte de la hoja"));
        assertTrue(catalog.contains("No usar new Button(...) en workspaces"));
    }

    @Test
    void documentationFreezesGuiComponentPolicyBeforeRedesign() throws Exception {
        String doc = read("docs/productizacion/T97_COMPONENTES_GUI_TRANSVERSALES.md");
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");

        assertTrue(doc.contains("catálogo oficial de componentes"));
        assertTrue(doc.contains("No diseñar encima de alucinaciones"));
        assertTrue(doc.contains("RibbonButton"));
        assertTrue(doc.contains("SidebarIconTab"));
        assertTrue(doc.contains("RailToggleButton"));
        assertTrue(readme.contains("T97 — Componentes GUI transversales"));
        assertTrue(handoff.contains("Handoff T97"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
