package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreGrammarRibbonSourceTest {
    @Test
    void theatreRibbonImportsGrammarAndExportsTemplate() throws Exception {
        String commands = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java");
        String icons = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonIconCatalog.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String template = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarTemplate.java");
        String parser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreGrammarMarkdownParser.java");
        String applicationTemplate = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarTemplate.java");
        String applicationParser = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarMarkdownParser.java");

        assertTrue(commands.contains("IMPORT_THEATRE_GRAMMAR"));
        assertTrue(commands.contains("EXPORT_THEATRE_GRAMMAR_TEMPLATE"));
        assertTrue(commands.contains("EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(registry.contains("Importar obra a partir de gramatica"));
        assertTrue(registry.contains("Exportar plantilla de gramatica teatral"));
        assertTrue(registry.contains("Exportar video de mapa teatral"));
        assertTrue(registry.contains("MP4 teatral con mapa espacial"));
        assertTrue(ribbon.contains("group(\"Gramatica\""));
        assertTrue(ribbon.contains("cmd(AppCommandId.IMPORT_THEATRE_GRAMMAR, true)"));
        assertTrue(ribbon.contains("cmd(AppCommandId.EXPORT_THEATRE_GRAMMAR_TEMPLATE, false)"));
        assertFalse(ribbon.contains("cmd(AppCommandId.EXPORT_THEATRE_SPATIAL_VIEW"));
        assertTrue(icons.contains("case IMPORT_THEATRE_GRAMMAR -> AppIcon.UPLOAD"));
        assertTrue(icons.contains("case EXPORT_THEATRE_GRAMMAR_TEMPLATE -> AppIcon.TEXT"));
        assertTrue(icons.contains("case EXPORT_THEATRE_SPATIAL_VIEW -> AppIcon.FULLSCREEN"));
        assertTrue(shell.contains("handleImportTheatreGrammar"));
        assertTrue(shell.contains("handleExportTheatreGrammarTemplate"));
        assertTrue(shell.contains("handleExportTheatreSpatialView"));
        assertTrue(shell.contains("chooseTargetMp4AndExport"));
        assertTrue(shell.contains("docupodcast-mapa-teatral"));
        assertTrue(shell.contains("Video MP4 (*.mp4)"));
        assertTrue(shell.contains("grammarWorkflow.importTheatreGrammar"));
        assertTrue(shell.contains("grammarWorkflow.exportTemplate(ProjectGrammarKind.THEATRE_PRODUCTION"));
        assertTrue(template.contains("Compatibility facade"));
        assertTrue(parser.contains("Compatibility facade"));
        assertTrue(applicationTemplate.contains("DocuPodcast Teatro Grammar v1"));
        assertTrue(applicationTemplate.contains("cualquier obra"));
        assertTrue(applicationTemplate.contains("personaje:"));
        assertTrue(applicationTemplate.contains("objeto:"));
        assertTrue(applicationTemplate.contains("TONO_CATALOGO"));
        assertTrue(applicationTemplate.contains("VoiceReferenceTone.values()"));
        assertTrue(applicationTemplate.contains("tono=SERIOUS"));
        assertTrue(applicationTemplate.contains("interaccion=TENIENTE TORNILLO, Publico"));
        assertTrue(applicationParser.contains("ACT_HEADING"));
        assertTrue(applicationParser.contains("SCENE_HEADING"));
        assertTrue(applicationParser.contains("MARKDOWN_LINK"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
