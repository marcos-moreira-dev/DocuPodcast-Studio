package com.marcosmoreiradev.docupodcaststudio.application.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImportImageAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImportUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.grammar.FileSystemProjectSemanticsRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard.LocalImageAssetFileRepository;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionCatalogo;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TheatreGrammarImportIntegrationTest {
    @TempDir Path temp;

    @Test void importsOnlySpeechWithStableBindingsAssetsVoicesAndChorusAcrossReopen() throws Exception {
        Path source = temp.resolve("obra.md");
        Files.writeString(source, """
                # Obra
                <!--
                CATALOGO: Nunca narrar esta declaración.
                -->
                ## Personajes
                - personaje: UNO_UNO | id=CHR-UNO | voz=hombre_maduro_narrativo
                - cara.png | angulo=frontal
                - personaje: DOS | voz=hombre_20_idealista_ecuador_dialogo
                - personaje: CORO
                ## Objetos
                - objeto: LLAVE
                - cara.png
                ## Acto: Acto primero
                ### Escena: Plaza
                > mapa_espacial=cara.png | fondo_escenario=cara.png
                UNO_UNO: Primera oración. Segunda: permanece entera.
                > id=7 | origen=centro | imagen=cara.png | tono=SERIOUS
                DOS: Respuesta.
                > id=9 | destino=izquierda
                CORO: Cantamos juntos.
                > id=12
                """);
        Files.write(temp.resolve("cara.png"), Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aGxkAAAAASUVORK5CYII="));
        Files.createDirectories(temp.resolve("config"));
        Files.writeString(temp.resolve("config/voces.csv"), "personaje_id,voz_alias_o_mezcla,modo,sintetizar\nCORO,hombre_maduro_narrativo;hombre_20_idealista_ecuador_dialogo,MIX,true\n");
        Path projectFile = temp.resolve("output/obra.docupodcast.json");
        var result = importAndReopen(source, projectFile);
        assertEquals(3, result.script().segments().size());
        assertEquals("Primera oración. Segunda: permanece entera.", result.script().segments().getFirst().narrationText());
        assertEquals("CHR-UNO", result.script().segments().getFirst().characterId());
        assertEquals("INTERVENCION-7", result.project().theatre().intervenciones().getFirst().id());
        assertEquals(1, result.project().theatre().characterImages().size());
        assertEquals(1, result.project().theatre().objectImages().size());
        assertEquals(1, result.project().theatre().choralVoiceAssignments().size());
        assertEquals("VOC-PRESET-HOMBRE-MADURO-NARRATIVO", result.project().theatre().voiceRoleAliases().getFirst().voiceProfileId());
        var render = new com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase()
                .build(result.script(), result.project());
        assertEquals("VOC-PRESET-HOMBRE-MADURO-NARRATIVO", render.units().getFirst().voiceProfileId());
        assertEquals(3, com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreCharacterDetector
                .detect(null, result.script(), result.project().theatre().characters()).size());
        assertEquals(1, result.assetCount());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        String sidecar = Files.readString(projectFile.getParent().resolve("project-semantics.json"));
        assertTrue(sidecar.contains("SEG-INTERVENCION-7"));
        assertTrue(sidecar.contains("B-INTERVENCION-9"));
    }

    @Test void missingAndEscapingAssetsWarnWithoutLosingDialogue() throws Exception {
        Path source = temp.resolve("obra.md");
        Files.writeString(source, "# Obra\nUNO: Hola.\n> imagen=../fuera.png\nDOS: Adiós.\n> imagen=no-existe.png\n");
        var plan = TheatreGrammarMarkdownParser.parse(source);
        AtomicInteger calls = new AtomicInteger();
        var assets = new TheatreGrammarAssetImporter().importAssets(source, plan, p -> "IMG-" + calls.incrementAndGet());
        assertEquals(0, calls.get());
        assertEquals(2, assets.diagnostics().size());
        assertEquals(2, new TheatreGrammarDocumentBuilder().build(plan, source).blocks().size());
    }

    @Test void explicitlySelectedProductionFixtureCanBeAuditedWithoutChangingItsFiles() throws Exception {
        String input = System.getProperty("theatre.audit.source", "");
        Assumptions.assumeFalse(input.isBlank(), "Optional local production fixture");
        Path source = Path.of(input).toAbsolutePath();
        byte[] before = Files.readAllBytes(source);
        Path output = Path.of(System.getProperty("theatre.audit.output", temp.resolve("audit/obra.docupodcast.json").toString())).toAbsolutePath();
        assertFalse(output.startsWith(source.getParent()), "Audit output must be outside the original work");
        var result = importAndReopen(source, output);
        assertArrayEquals(before, Files.readAllBytes(source));
        assertTrue(result.script().segments().size() > 100);
        assertTrue(result.assetCount() > 20);
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        var layer = result.project().theatre();
        String report = "Intervenciones: " + result.script().segments().size() + "\nPersonajes: " + layer.characters().size()
                + "\nObjetos: " + layer.objects().size() + "\nEscenas: " + layer.scenes().size()
                + "\nImágenes importadas: " + result.assetCount() + "\nAsignaciones corales: " + layer.choralVoiceAssignments().size()
                + "\nGuardar/reabrir: OK\n";
        Files.writeString(output.getParent().resolve("auditoria.txt"), report);
        System.out.println(report);
    }

    private record Result(DocuPodcastProject project, NarrationScriptDocument script, int assetCount, List<GrammarDiagnostic> diagnostics) { }

    private Result importAndReopen(Path source, Path projectFile) throws Exception {
        var grammar = new ImportProjectGrammarMarkdownUseCase(new FileSystemProjectSemanticsRepository());
        var parsed = grammar.parseTheatre(source);
        var plan = parsed.plan();
        var document = new TheatreGrammarDocumentBuilder().build(plan, source);
        var script = new BuildNarrationScriptUseCase().build(document, "es", true);
        assertEquals(plan.interventions().size(), script.segments().size());
        assertEquals(plan.interventions().stream().map(i -> i.spokenText()).toList(), script.segments().stream().map(s -> s.narrationText()).toList());
        assertEquals(script.segments().size(), IntervencionCatalogo.intervenciones(document, script).size());
        assertEquals(plan.interventions().stream().map(i -> i.stableInterventionId()).toList(),
                IntervencionCatalogo.intervenciones(document, script).stream().map(i -> i.alias()).toList());
        DocuPodcastProject[] project = {DocuPodcastProject.createNew(plan.title(), ProjectMode.THEATRE_PRODUCTION)};
        var importer = new ImportImageAssetUseCase(new LocalImageAssetFileRepository());
        var assets = new TheatreGrammarAssetImporter().importAssets(source, plan, image -> {
            var imported = importer.importImage(project[0], projectFile, image);
            project[0] = imported.project();
            return imported.imageAsset().id();
        });
        var imported = new TheatreImportUseCase().execute(plan, script, project[0].voiceLibrary(), assets.assetIds());
        project[0] = project[0].withTheatre(imported.layer());
        for (var a : imported.imageAssignments()) project[0] = project[0].withNarrativeLayerAssignment(a);
        for (var a : imported.emotionAssignments()) project[0] = project[0].withNarrativeLayerAssignment(a);
        for (var b : imported.sceneBoundariesStart().entrySet()) project[0] = project[0].withViewState(
                "theatre.sceneBoundary." + b.getKey(), b.getValue() + "|" + imported.sceneBoundariesEnd().get(b.getKey()));
        for (var p : plan.characters()) {
            if (!p.voz().isBlank()) assertTrue(project[0].voiceLibrary().voiceById(TheatreImportUseCase.resolveVoiceId(p.voz(), project[0].voiceLibrary())).isPresent(), p.voz());
        }
        var documents = new ReadableDocumentWorkspaceRepository();
        var materialized = documents.materialize(document, ReadingProfile.academicDefaults(), projectFile);
        project[0] = project[0].withAsset(materialized.sourceDocumentAsset()).withAsset(materialized.importedDocumentAsset());
        var scripts = new NarrationScriptWorkspaceFileRepository();
        project[0] = project[0].withAsset(scripts.materialize(script, projectFile).narrationScriptAsset());
        var projects = new DocuPodcastProjectFileRepository();
        projects.save(project[0], projectFile);
        grammar.materializeTheatre(project[0], projectFile, source, plan, script, parsed.report().withAdditionalDiagnostics(assets.diagnostics()));
        var reopened = projects.open(projectFile);
        var reloadedScript = scripts.load(projectFile).orElseThrow();
        var reloadedDocument = ((BlockDocumentSource) documents.load(projectFile).orElseThrow()).document();
        for (var component : project[0].theatre().getClass().getRecordComponents()) {
            Object expected = component.getAccessor().invoke(project[0].theatre());
            Object actual = component.getAccessor().invoke(reopened.theatre());
            assertTrue(Objects.equals(expected, actual), "Reapertura cambió " + component.getName());
        }
        assertEquals(script.segments(), reloadedScript.segments());
        assertEquals(script.segments(), new BuildNarrationScriptUseCase().build(reloadedDocument, "es", false).segments());
        return new Result(reopened, reloadedScript, new HashSet<>(assets.assetIds().values()).size(), assets.diagnostics());
    }
}
