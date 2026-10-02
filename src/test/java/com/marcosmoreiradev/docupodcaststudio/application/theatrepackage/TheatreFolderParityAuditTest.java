package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.*;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

/** Audit probes for the requested manual/folder equivalence; archived after execution. */
class TheatreFolderParityAuditTest {
    @TempDir Path temp;

    @Test void manifestSelectsGrammarAndRepeatedImportIsSafe() throws Exception {
        Path folder = fixture(false);
        Path declared = folder.resolve("dialogos.md");
        Files.move(folder.resolve("obra.teatro.md"), declared);
        Path manifest = folder.resolve("docupodcast-theatre.json");
        Files.writeString(manifest, Files.readString(manifest).replace("obra.teatro.md", "dialogos.md"));
        var importer = new ImportOfficialTheatrePackageUseCase();
        Path projectRoot = temp.resolve("project");
        var first = importer.execute(DocuPodcastProject.createNew("Nuevo"), folder, projectRoot, null);
        var second = importer.execute(first.project(), folder, projectRoot, first.script());
        assertEquals(first.snapshots(), second.snapshots());
        assertEquals(first.document().sourcePath(), second.document().sourcePath());
        assertTrue(Files.isRegularFile(second.document().sourcePath()));
        Files.writeString(declared, Files.readString(declared).replace("Este es el parlamento del paquete.", "Texto actualizado."));
        var changed = importer.execute(second.project(), folder, projectRoot, first.script());
        assertEquals("Texto actualizado.", changed.script().segments().getFirst().narrationText());
        assertNotEquals(second.document().sourcePath(), changed.document().sourcePath());
    }

    @Test void sharedAudioPreservesSpeechAndTrackSettingsAcrossExportImport() throws Exception {
        Path folder = fixture(true);
        Path projectRoot = temp.resolve("first");
        var importer = new ImportOfficialTheatrePackageUseCase();
        var first = importer.execute(DocuPodcastProject.createNew("Nuevo"), folder, projectRoot, null);
        String audioId = first.project().narrativeLayerAssignments().stream()
                .filter(a -> a.kind() == com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind.HUMAN_AUDIO)
                .findFirst().orElseThrow().targetId();
        var track = new com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer.TheatreAudioTrack(
                "TRACK-UNO", audioId, "INTERVENCION-1", first.script().segments().getFirst().id(),
                1, 3, com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer.AudioTrackEndMode.FILE_END, 0.6, 4, true);
        var configured = first.project().withTheatre(first.project().theatre().withAudioTracks(java.util.List.of(track)));
        Path exported = temp.resolve("exported");
        var written = new ExportOfficialTheatrePackageUseCase().execute(configured, projectRoot, exported, first.script());
        assertEquals(1, written.assetCount());
        var second = importer.execute(DocuPodcastProject.createNew("Destino"), exported, temp.resolve("second"), null);
        assertEquals(java.util.List.of(track), second.project().theatre().audioTracks());
        assertEquals(NarrationRenderSourceKind.AUDIO_CLIP,
                new BuildNarrationRenderPlanUseCase().build(second.script(), second.project()).units().getFirst().sourceKind());
    }

    @Test void importedVoiceSampleIsBoundAndRemainsPortable() throws Exception {
        Path folder = fixture(true);
        Path manifest = folder.resolve("docupodcast-theatre.json");
        Files.writeString(manifest, Files.readString(manifest).replace("\"kind\":\"HUMAN_AUDIO\"", "\"kind\":\"VOICE_SAMPLE\",\"voiceProfileId\":\"VOC-NARRATOR\",\"tone\":\"NEUTRAL\""));
        var importer = new ImportOfficialTheatrePackageUseCase();
        var first = importer.execute(DocuPodcastProject.createNew("Nuevo"), folder, temp.resolve("first"), null);
        var sample = first.project().voiceLibrary().referenceSampleSetByVoiceId("VOC-NARRATOR").orElseThrow().neutralSample().orElseThrow();
        assertTrue(Files.isRegularFile(temp.resolve("first").resolve(sample.fileUri())));
        Path exported = temp.resolve("exported");
        new ExportOfficialTheatrePackageUseCase().execute(first.project(), temp.resolve("first"), exported, first.script());
        var second = importer.execute(DocuPodcastProject.createNew("Destino"), exported, temp.resolve("second"), null);
        var restored = second.project().voiceLibrary().referenceSampleSetByVoiceId("VOC-NARRATOR").orElseThrow().neutralSample().orElseThrow();
        assertEquals(sample.id(), restored.id());
        assertTrue(Files.isRegularFile(temp.resolve("second").resolve(restored.fileUri())));
    }

    @Test void optionalLargeProjectCanBeExportedAndImportedAsAnIndependentFolder() throws Exception {
        String selected = System.getProperty("theatre.audit.project", "");
        org.junit.jupiter.api.Assumptions.assumeFalse(selected.isBlank());
        Path projectFile = Path.of(selected);
        var repository = new com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository();
        var project = repository.open(projectFile);
        if (!System.getProperty("theatre.audit.presentationMode", "").isBlank())
            project = project.withViewState("theatre.presentationMode", System.getProperty("theatre.audit.presentationMode"));
        var scripts = new com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository();
        var script = scripts.load(projectFile).orElseThrow();
        Path root = Path.of(System.getProperty("theatre.audit.packageOutput", temp.toString())).toAbsolutePath();
        Files.createDirectories(root);
        Path exported = Files.createTempDirectory(root, "carpeta-").resolve("obra");
        new ExportOfficialTheatrePackageUseCase().execute(project, projectFile.getParent(), exported, script);
        Path destination = exported.getParent().resolve("proyecto");
        var imported = new ImportOfficialTheatrePackageUseCase().execute(DocuPodcastProject.createNew("Vacío"), exported, destination, null);
        assertEquals(script.segments().stream().map(s -> s.narrationText()).toList(), imported.script().segments().stream().map(s -> s.narrationText()).toList());
        assertEquals(project.theatre().objects().size(), imported.project().theatre().objects().size());
        assertEquals(project.theatre().characters().size(), imported.project().theatre().characters().size());
        assertEquals(project.theatre().interventionStates().stream().map(s -> s.tone()).toList(),
                imported.project().theatre().interventionStates().stream().map(s -> s.tone()).toList());
        var sourceUnits = new BuildNarrationRenderPlanUseCase().build(script, project).units();
        var restoredUnits = new BuildNarrationRenderPlanUseCase().build(imported.script(), imported.project()).units();
        assertEquals(sourceUnits.stream().map(u -> u.performanceStyleId()).toList(),
                restoredUnits.stream().map(u -> u.performanceStyleId()).toList());
        assertEquals(sourceUnits.stream().map(u -> u.voiceProfileId()).toList(),
                restoredUnits.stream().map(u -> u.voiceProfileId()).toList());
        assertTrue(script.segments().stream().allMatch(segment -> {
            String title = segment.title() == null ? "" : segment.title().strip();
            return title.isBlank() || !segment.narrationText().strip().toUpperCase(java.util.Locale.ROOT)
                    .startsWith(title.toUpperCase(java.util.Locale.ROOT) + ":");
        }), "El texto TTS teatral no debe incluir la etiqueta PERSONAJE:");
        Path saved = destination.resolve("obra.docupodcast.json");
        var docs = new com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository();
        var materialized = docs.materialize(imported.document(), imported.project().readingProfile(), saved);
        var ready = imported.project().withAsset(materialized.sourceDocumentAsset()).withAsset(materialized.importedDocumentAsset())
                .withAsset(scripts.materialize(imported.script(), saved).narrationScriptAsset());
        repository.save(ready, saved);
        if ("scenery".equals(ready.viewState().get("theatre.presentationMode"))) {
            var placement=ready.theatre().textActionPlacements().get(4);
            var scene=ready.theatre().scenes().stream().filter(s->s.id().equals(placement.sceneId())).findFirst().orElseThrow();
            var sample=imported.script().segments().get(4);
            var names=new java.util.LinkedHashMap<String,String>();
            ready.theatre().characters().forEach(c->names.put(c.id(),c.displayName()));
            new com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreSpatialVideoPlanUseCase().render(
                    new com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreSpatialVideoPlanUseCase.FrameSpec(
                            1920,1080,"scenery",scene,placement,sample,null,null,ready,destination,names,sample.narrationText()),
                    exported.getParent().resolve("escenografia-preview.png"));
        }
        assertEquals(imported.script().segments(), scripts.load(saved).orElseThrow().segments());
        assertEquals(ready.theatre(), repository.open(saved).theatre());
        System.out.println("CARPETA VALIDADA: " + exported + "\nPROYECTO VALIDADO: " + saved);
    }

    private Path fixture(boolean audio) throws Exception {
        Path folder = Files.createDirectory(temp.resolve("input"));
        Files.writeString(folder.resolve("obra.teatro.md"), """
                # Ejemplo independiente
                > grammarVersion: theatre-v2
                ## Personajes
                - personaje: ANA | id=CHR-ANA | voz=VOC-NARRATOR
                ## Acto: Primero | id=ACT-UNO
                ### Escena: Plaza | id=SCN-PLAZA
                ANA: Este es el parlamento del paquete.
                > id=INTERVENCION-1 | origen=centro | aplicar_plano=false | contexto_ia=Contexto técnico de continuidad
                """);
        String assets = "";
        if (audio) {
            Path wav = folder.resolve("audio.wav");
            Files.write(wav, new byte[]{1,2,3,4}); // No decoder invoked: tests binding, not media validity.
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(wav)));
            assets = "{\"path\":\"audio.wav\",\"logicalId\":\"spoken-audio\",\"kind\":\"HUMAN_AUDIO\",\"sha256\":\"" + hash
                    + "\",\"size\":4,\"interventionId\":\"INTERVENCION-1\"}";
        }
        Files.writeString(folder.resolve("docupodcast-theatre.json"),
                "{\"schemaVersion\":2,\"grammarVersion\":\"theatre-v2\",\"grammar\":\"obra.teatro.md\",\"packageId\":\"parity\",\"packageVersion\":\"1\",\"assets\":[" + assets + "]}");
        return folder;
    }

    @Test void folderAloneMustProvideItsSpokenText() throws Exception {
        var result = new ImportOfficialTheatrePackageUseCase().execute(DocuPodcastProject.createNew("Nuevo"),
                fixture(false), temp.resolve("project"), null);
        assertEquals("Este es el parlamento del paquete.", result.snapshots().getFirst().text());
    }

    @Test void spanishEmotionReachesTheRenderPlanAndUnknownToneIsRejected() throws Exception {
        Path folder=fixture(false);
        Path grammar=folder.resolve("obra.teatro.md");
        Files.writeString(grammar,Files.readString(grammar).replace("| origen=centro", "| emocion=enojado | origen=centro"));
        var importer=new ImportOfficialTheatrePackageUseCase();
        var result=importer.execute(DocuPodcastProject.createNew("Nuevo"),folder,temp.resolve("valid"),null);
        var unit=new BuildNarrationRenderPlanUseCase().build(result.script(),result.project()).units().getFirst();
        assertEquals("TONE-ANGRY",unit.performanceStyleId());
        Files.writeString(grammar,Files.readString(grammar).replace("emocion=enojado","tono=enojaod"));
        var error=assertThrows(java.io.IOException.class,()->importer.execute(
                DocuPodcastProject.createNew("Nuevo"),folder,temp.resolve("invalid"),null));
        assertTrue(error.getMessage().contains("Tono teatral desconocido"));
    }

    @Test void sceneryModeAndOnlyDeclaredParticipantsSurviveFolderRoundTrip() throws Exception {
        Path folder=fixture(false);
        Path md=folder.resolve("obra.teatro.md");
        Files.writeString(md,Files.readString(md).replace("## Acto:",
                "- personaje: BEA | id=CHR-BEA\n- personaje: CLARA | id=CHR-CLARA\n## Acto:")
                .replace("| origen=centro", "| interaccion=BEA | origen=centro"));
        Path manifest=folder.resolve("docupodcast-theatre.json");
        Files.writeString(manifest,Files.readString(manifest).replace("\"schemaVersion\":2", "\"schemaVersion\":2,\"presentationMode\":\"scenery\""));
        var importer=new ImportOfficialTheatrePackageUseCase();
        var first=importer.execute(DocuPodcastProject.createNew("Nuevo"),folder,temp.resolve("first"),null);
        assertEquals("scenery",first.project().viewState().get("theatre.presentationMode"));
        var placement=first.project().theatre().textActionPlacements().getFirst();
        var composition=new com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSceneryComposition()
                .resolve(first.project(),placement,temp.resolve("first"));
        assertEquals(java.util.List.of("CHR-ANA","CHR-BEA"),composition.figures().stream().map(f->f.id()).toList());
        Path exported=temp.resolve("exported");
        new ExportOfficialTheatrePackageUseCase().execute(first.project(),temp.resolve("first"),exported,first.script());
        var second=importer.execute(DocuPodcastProject.createNew("Destino"),exported,temp.resolve("second"),null);
        assertEquals("scenery",second.project().viewState().get("theatre.presentationMode"));
    }

    @Test void importedHumanAudioMustBeUsedByTheRenderPlan() throws Exception {
        Path folder = fixture(true);
        var plan = TheatreGrammarMarkdownParser.parse(folder.resolve("obra.teatro.md"));
        var script = new BuildNarrationScriptUseCase().build(new TheatreGrammarDocumentBuilder()
                .build(plan, folder.resolve("obra.teatro.md")), "es");
        var result = new ImportOfficialTheatrePackageUseCase().execute(DocuPodcastProject.createNew("Nuevo"),
                folder, temp.resolve("project"), script);
        assertEquals(NarrationRenderSourceKind.AUDIO_CLIP,
                new BuildNarrationRenderPlanUseCase().build(script, result.project()).units().getFirst().sourceKind());
    }

    @Test void exportMustPreserveExplicitCameraApplicationPolicy() throws Exception {
        Path folder = fixture(false);
        var plan = TheatreGrammarMarkdownParser.parse(folder.resolve("obra.teatro.md"));
        var script = new BuildNarrationScriptUseCase().build(new TheatreGrammarDocumentBuilder()
                .build(plan, folder.resolve("obra.teatro.md")), "es");
        var result = new ImportOfficialTheatrePackageUseCase().execute(DocuPodcastProject.createNew("Nuevo"),
                folder, temp.resolve("project"), script);
        Path output = temp.resolve("output");
        new ExportOfficialTheatrePackageUseCase().execute(result.project(), temp.resolve("project"), output, script);
        var exported = TheatreGrammarMarkdownParser.parse(output.resolve("obra.teatro.md"));
        assertFalse(exported.interventions().getFirst().applyCamera());
        assertFalse(new com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreCameraApplicationPolicy()
                .appliesToSegment(null, result.script().segments().getFirst()));
    }
}
