package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ResolveTheatreInterventionSnapshotUseCase;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OfficialTheatrePackageTest {
    @TempDir Path temp;

    @Test void folderAndZipProduceIdenticalSnapshotsAndOptionalAbsenceIsClean() throws Exception {
        Path folder = fixture(false);
        Path zip = temp.resolve("obra.zip"); zip(folder, zip);
        var useCase = new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner());
        var a = useCase.execute(DocuPodcastProject.createNew("A"), folder, Files.createDirectory(temp.resolve("project-a")), null);
        var b = useCase.execute(DocuPodcastProject.createNew("B"), zip, Files.createDirectory(temp.resolve("project-b")), null);
        assertEquals(a.snapshots(), b.snapshots());
        assertEquals(a.warnings(), b.warnings());
        assertEquals(1, a.warnings().size());
        assertTrue(a.warnings().get(0).contains("no declara tonos"));
        assertEquals("CONCHA", a.snapshots().get(0).speakerCharacterId());
        assertEquals("", a.snapshots().get(0).frameAssetId());
    }

    @Test void declaredMissingAssetFailsButUndeclaredOptionalAssetDoesNot() throws Exception {
        Path folder = fixture(true);
        IOException error = assertThrows(IOException.class, () -> new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner())
                .execute(DocuPodcastProject.createNew("A"), folder, Files.createDirectory(temp.resolve("broken-project")), null));
        assertTrue(error.getMessage().contains("inexistente"));
    }

    @Test void hashMismatchFails() throws Exception {
        Path folder = fixture(false); Files.writeString(folder.resolve("assets/fondos/plaza.txt"), "alterado");
        IOException error = assertThrows(IOException.class, () -> new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner())
                .execute(DocuPodcastProject.createNew("A"), folder, Files.createDirectory(temp.resolve("hash-project")), null));
        assertTrue(error.getMessage().contains("Hash"));
    }

    @Test void exportImportRoundTripPreservesCanonicalSnapshots() throws Exception {
        Path sourceProject = Files.createDirectory(temp.resolve("roundtrip-a"));
        var importer = new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner());
        var first = importer.execute(DocuPodcastProject.createNew("Obra fixture"), fixture(false), sourceProject, null);
        Path exported = temp.resolve("roundtrip.zip");
        new ExportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository()).execute(first.project(), sourceProject, exported, first.script());
        var second = importer.execute(DocuPodcastProject.createNew("Destino"), exported,
                Files.createDirectory(temp.resolve("roundtrip-b")), null);
        assertEquals(first.snapshots(), second.snapshots());
    }

    @Test void saveCloseOpenPreservesIdsAndSnapshots() throws Exception {
        Path projectRoot = Files.createDirectory(temp.resolve("persist-project"));
        var imported = new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner()).execute(
                DocuPodcastProject.createNew("Persistencia"), fixture(false), projectRoot, null);
        Path file = projectRoot.resolve("obra.docupodcast.json");
        var repository = new DocuPodcastProjectFileRepository(); repository.save(imported.project(), file);
        DocuPodcastProject reopened = repository.open(file);
        var resolver = new ResolveTheatreInterventionSnapshotUseCase();
        assertEquals(imported.project().theatre().intervenciones(), reopened.theatre().intervenciones());
        assertEquals(imported.snapshots().get(0), resolver.execute(reopened, imported.script(), "INTERVENCION-1"));
    }

    @Test void complexFixtureCoversScenesCharactersPropsChorusAndOptionalAssets() throws Exception {
        Path fixture = Path.of("src/test/resources/theatre-v2/complex").toAbsolutePath();
        var result = new ImportOfficialTheatrePackageUseCase(new com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner()).execute(DocuPodcastProject.createNew("Complejo"),
                fixture, Files.createDirectory(temp.resolve("complex-project")), null);
        assertEquals(3, result.project().theatre().scenes().size());
        assertEquals(5, result.project().theatre().characters().size());
        assertEquals(7, result.snapshots().size());
        assertEquals(1, result.project().theatre().choralVoiceAssignments().size());
        assertFalse(result.snapshots().get(0).audioAssetIds().isEmpty());
        assertFalse(result.snapshots().get(0).frameAssetId().isBlank());
        assertTrue(result.snapshots().get(1).frameAssetId().isBlank());
        assertTrue(result.warnings().isEmpty());
    }

    private Path fixture(boolean missing) throws Exception {
        Path root = Files.createDirectory(temp.resolve(missing ? "missing" : "folder"));
        Files.createDirectories(root.resolve("assets/fondos"));
        Path asset = root.resolve("assets/fondos/plaza.txt"); if (!missing) Files.writeString(asset, "plaza", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("obra.teatro.md"), """
                # Obra
                > DocuPodcast Teatro Grammar v2
                > grammarVersion: theatre-v2
                ## Personajes
                - personaje: Concha | id=CONCHA | voz=voz_concha
                ## Objetos
                - objeto: Sombrero | id=SOMBRERO
                ## Acto: Primero
                ### Escena: Plaza
                > fondo_escenario=assets/fondos/plaza.txt
                Concha: Hola.
                > id=INTERVENCION-1 | hereda=ninguna | presentes=CONCHA@centro | objetos=SOMBRERO@centro
                """);
        String hash = missing ? "0".repeat(64) : sha(asset); long size = missing ? 5 : Files.size(asset);
        Files.writeString(root.resolve("docupodcast-theatre.json"), """
                {"schemaVersion":2,"grammarVersion":"theatre-v2","grammar":"obra.teatro.md","packageId":"fixture","packageVersion":"1.0.0","assets":[
                {"path":"assets/fondos/plaza.txt","logicalId":"backdrop:plaza","kind":"BACKDROP","sha256":"%s","size":%d,"backdropId":"PLAZA","scope":"SCENE","scopeId":"SCN-PLAZA"}]}
                """.formatted(hash,size));
        return root;
    }
    private static String sha(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
    private static void zip(Path root, Path zip) throws Exception { try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){ try(var files=Files.walk(root)){ for(Path file:files.filter(Files::isRegularFile).toList()){ out.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\','/'))); Files.copy(file,out); out.closeEntry(); } } } }
}
