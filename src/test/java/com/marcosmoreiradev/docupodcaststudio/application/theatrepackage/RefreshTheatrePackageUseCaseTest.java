package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.application.project.InspectProjectIntegrityUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.AtomicJsonFileWriter;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.FileSystemTheatreAssetStager;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatreImportStateRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatrePackageScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTheatrePackageUseCaseTest {
    @TempDir Path temp;

    @Test
    void appliesIncrementalAssetsThenBecomesIdempotent() throws Exception {
        Path projectRoot = temp.resolve("project");
        Path projectFile = projectRoot.resolve("obra.docupodcast.json");
        Path source = temp.resolve("source");
        Files.createDirectories(source);
        writeManifest(source);
        write(source, "assets/personajes/narrador/frontal.png", "narrador-v1");
        DocuPodcastProject project = theatreProject();
        var projectRepository = new DocuPodcastProjectFileRepository();
        projectRepository.save(project, projectFile);
        var useCase = useCase(new JsonTheatreImportStateRepository(), projectRepository);

        var initial = useCase.prepare(project, projectFile, source, null, null);
        var first = useCase.commit(initial, null, null);
        assertEquals(TheatreRefreshResult.Status.APPLIED, first.status());

        write(source, "assets/personajes/narrador/frontal.png", "narrador-v2");
        write(source, "assets/personajes/concha/frontal.png", "concha");
        write(source, "assets/audio/c08/i_00842.wav", "audio");
        var prepared = useCase.prepare(first.project(), projectFile, source, null, null);

        assertEquals(2, prepared.preflight().newAssets());
        assertEquals(1, prepared.preflight().modifiedAssets());
        assertTrue(prepared.preflight().canCommit());
        var applied = useCase.commit(prepared, null, null);
        assertEquals(2, applied.project().theatre().characterImages().size());
        assertTrue(applied.project().narrativeLayerAssignments().stream()
                .anyMatch(layer -> layer.targetId().contains("SCENE-C08-INTERVENTION-I-00842-AUDIO")));
        assertTrue(Files.isRegularFile(projectRoot.resolve(JsonTheatreImportStateRepository.FILE_NAME)));

        var noOpPrepared = useCase.prepare(applied.project(), projectFile, source, null, null);
        String projectBeforeNoOp = Files.readString(projectFile);
        String stateBeforeNoOp = Files.readString(projectRoot.resolve(JsonTheatreImportStateRepository.FILE_NAME));
        var noOp = useCase.commit(noOpPrepared, null, null);
        assertEquals(TheatreRefreshResult.Status.NO_CHANGES, noOp.status());
        assertEquals(projectBeforeNoOp, Files.readString(projectFile));
        assertEquals(stateBeforeNoOp, Files.readString(projectRoot.resolve(JsonTheatreImportStateRepository.FILE_NAME)));
    }

    @Test
    void cancellationBeforeStagingLeavesProjectAndAssetsUntouched() throws Exception {
        Path projectRoot = temp.resolve("cancel-project");
        Path projectFile = projectRoot.resolve("obra.docupodcast.json");
        Path source = temp.resolve("cancel-source");
        Files.createDirectories(source);
        writeManifest(source);
        write(source, "assets/personajes/narrador/frontal.png", "narrador");
        DocuPodcastProject project = theatreProject();
        var projectRepository = new DocuPodcastProjectFileRepository();
        projectRepository.save(project, projectFile);
        String before = Files.readString(projectFile);
        var useCase = useCase(new JsonTheatreImportStateRepository(), projectRepository);
        var prepared = useCase.prepare(project, projectFile, source, null, null);

        assertThrows(java.io.InterruptedIOException.class,
                () -> useCase.commit(prepared, null, () -> true));

        assertEquals(before, Files.readString(projectFile));
        assertFalse(Files.exists(projectRoot.resolve(JsonTheatreImportStateRepository.FILE_NAME)));
        assertFalse(Files.exists(projectRoot.resolve("media/images/theatre")));
    }

    @Test
    void acceptedRenameReplacesItsPreviousSyncIdentityAndNextRefreshIsClean() throws Exception {
        Path projectRoot = temp.resolve("rename-project");
        Path projectFile = projectRoot.resolve("obra.docupodcast.json");
        Path source = temp.resolve("rename-source");
        Files.createDirectories(source);
        writeManifest(source);
        Path original = source.resolve("assets/personajes/narrador/frontal.png");
        write(source, "assets/personajes/narrador/frontal.png", "same-image-content");
        DocuPodcastProject project = theatreProject();
        var projectRepository = new DocuPodcastProjectFileRepository();
        var stateRepository = new JsonTheatreImportStateRepository();
        projectRepository.save(project, projectFile);
        var useCase = useCase(stateRepository, projectRepository);
        var first = useCase.commit(useCase.prepare(project, projectFile, source, null, null), null, null);
        String previousLogicalId = stateRepository.open(projectRoot).orElseThrow().entries().getFirst().logicalId();

        Path renamed = source.resolve("assets/personajes/narrador/tres-cuartos.png");
        Files.move(original, renamed);
        var renamePrepared = useCase.prepare(first.project(), projectFile, source, null, null);
        assertEquals(1, renamePrepared.preflight().plan().count(
                com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus.RENAMED));
        var renamedResult = useCase.commit(renamePrepared, null, null);

        var stored = stateRepository.open(projectRoot).orElseThrow();
        assertEquals(1, stored.entries().size());
        assertNotEquals(previousLogicalId, stored.entries().getFirst().logicalId());
        var clean = useCase.prepare(renamedResult.project(), projectFile, source, null, null);
        assertEquals(0, clean.preflight().retainedMissingAssets());
        assertTrue(clean.preflight().plan().isNoOp());
    }

    @Test
    void rollsBackProjectAndPublishedAssetsWhenStateSaveFails() throws Exception {
        Path projectRoot = temp.resolve("rollback-project");
        Path projectFile = projectRoot.resolve("obra.docupodcast.json");
        Path source = temp.resolve("rollback-source");
        Files.createDirectories(source);
        writeManifest(source);
        write(source, "assets/personajes/narrador/frontal.png", "narrador");
        DocuPodcastProject project = theatreProject();
        var projectRepository = new DocuPodcastProjectFileRepository();
        projectRepository.save(project, projectFile);
        String before = Files.readString(projectFile);
        TheatreImportStateRepository failingState = new TheatreImportStateRepository() {
            @Override public Optional<com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState> open(Path root) { return Optional.empty(); }
            @Override public void save(com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState state, Path root) throws IOException {
                throw new IOException("simulated state failure");
            }
        };
        var useCase = useCase(failingState, projectRepository);
        var prepared = useCase.prepare(project, projectFile, source, null, null);

        assertThrows(IOException.class, () -> useCase.commit(prepared, null, null));

        assertEquals(before, Files.readString(projectFile));
        Path theatreMedia = projectRoot.resolve("media/images/theatre");
        if (Files.isDirectory(theatreMedia)) try (var files = Files.list(theatreMedia)) {
            assertEquals(0, files.count());
        }
    }

    private static RefreshTheatrePackageUseCase useCase(TheatreImportStateRepository state,
                                                        DocuPodcastProjectFileRepository projectRepository) {
        return new RefreshTheatrePackageUseCase(new JsonTheatrePackageScanner(), state,
                new ComputeTheatreRefreshPlanUseCase(), new PreflightTheatreRefreshUseCase(),
                new FileSystemTheatreAssetStager(), new ReconcileTheatreProjectUseCase(), projectRepository,
                new InspectProjectIntegrityUseCase(), new AtomicJsonFileWriter());
    }

    private static DocuPodcastProject theatreProject() {
        TheatreProjectLayer layer = new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-842", "SEG-842", 842)),
                List.of(new TheatreProjectLayer.CharacterProfile("narrador", "Narrador", List.of(), ""),
                        new TheatreProjectLayer.CharacterProfile("concha", "Concha", List.of(), "")),
                List.of(), List.of(),
                List.of(new TheatreProjectLayer.Scene("c08", "Escena 8", "")),
                List.of(), List.of());
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION).withTheatre(layer);
    }

    private static void writeManifest(Path source) throws Exception {
        Files.writeString(source.resolve("docupodcast-theatre.json"),
                "{\"schemaVersion\":1,\"packageId\":\"obra\",\"packageVersion\":\"1\",\"assets\":[]}");
    }

    private static void write(Path root, String relative, String content) throws Exception {
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
