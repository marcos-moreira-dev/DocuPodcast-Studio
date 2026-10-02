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
    }
}
