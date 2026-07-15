package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class TheatreBulkInterventionContextExportWorkflowTest {
    @TempDir
    Path temp;

    @Test
    void estimateAndExportCreateWorkActSceneInterventionHierarchy() throws Exception {
        ProjectSession session = ProjectSession.opened(project(), temp.resolve("obra.docupodcast.json"));
        NarrationScriptDocument script = script();
        TheatreBulkInterventionContextExportWorkflow workflow = new TheatreBulkInterventionContextExportWorkflow();

        var estimate = workflow.estimate(session, script, TheatreContextExportScope.all());
        assertEquals(2, estimate.packages());
        assertTrue(estimate.estimatedBytes() >= 80, "estimate should count repeated real files per intervention");
        assertFalse(estimate.warnings().isEmpty());

        Path target = temp.resolve("out");
        Files.createDirectories(target);
        var result = workflow.export(session, script, TheatreContextExportScope.all(), target);

        assertEquals(2, result.packages());
        assertTrue(Files.isDirectory(result.root().resolve("acto-1").resolve("escena-1")));
        assertTrue(Files.list(result.root().resolve("acto-1").resolve("escena-1"))
                .anyMatch(path -> path.getFileName().toString().startsWith("intervencion-1")));
    }

    private DocuPodcastProject project() throws Exception {
        write("assets/fragmento-1.png", 10);
        write("assets/fragmento-2.png", 11);
        write("assets/capitan.png", 12);
        write("assets/mapa.png", 13);
        write("assets/teatro-vacio.png", 14);
        write("assets/objeto.png", 15);
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra");
        for (ProjectAssetReference asset : assets()) project = project.withAsset(asset);
        return project.withTheatre(new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1),
                        new TheatreProjectLayer.Intervencion("INTERVENCION-2", "B0002", 2)),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-CAPITAN", "CAPITAN BIGOTE", List.of(), "")),
                List.of(),
                List.of(new TheatreProjectLayer.CharacterImage("CHR-CAPITAN", "Frontal", "IMG-CHAR", "")),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-FRAG-1", ""),
                        new TheatreProjectLayer.IntervencionVisual("INTERVENCION-2", "IMG-FRAG-2", "")),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-1", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SC-1", "Escena 1", "", "ACT-1", "IMG-MAP")),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SC-1", "CHR-CAPITAN", "centro", "centro", "", Map.of("CHR-CAPITAN", "centro")),
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-2", "SC-1", "CHR-CAPITAN", "centro", "centro", "", Map.of("CHR-CAPITAN", "centro"))),
                List.of(new TheatreProjectLayer.ObjectImage("OBJ-MAPA", "SC-1", "Referencia", "IMG-OBJ", "")),
                List.of(new TheatreProjectLayer.TheatreObject("OBJ-MAPA", "Mapa", ""))));
    }

    private List<ProjectAssetReference> assets() {
        return List.of(
                asset("IMG-FRAG-1", "fragmento-1.png"),
                asset("IMG-FRAG-2", "fragmento-2.png"),
                asset("IMG-CHAR", "capitan.png"),
                asset("IMG-MAP", "mapa.png"),
                asset("IMG-EMPTY", "teatro-vacio.png"),
                asset("IMG-OBJ", "objeto.png"));
    }

    private ProjectAssetReference asset(String id, String file) {
        return new ProjectAssetReference(id, ProjectAssetKind.IMAGE, file, "assets/" + file, "image/png", "test", "", "");
    }

    private void write(String relative, int bytes) throws Exception {
        Path file = temp.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[bytes]);
    }

    private NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Obra", "es", "source.docx", List.of(
                new NarrationSegment("SEG-1", NarrationSegmentType.PARAGRAPH, "", "CAPITAN BIGOTE: Trae el mapa.", List.of("B0001"), "CHR-CAPITAN", "VOC", "STY", Map.of()),
                new NarrationSegment("SEG-2", NarrationSegmentType.PARAGRAPH, "", "CAPITAN BIGOTE: Listo.", List.of("B0002"), "CHR-CAPITAN", "VOC", "STY", Map.of())));
    }
}
