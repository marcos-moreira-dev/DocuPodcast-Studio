package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class MigrationClosureTanda10SourceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path BACKUP_DOMAIN = Path.of(
            "C:/Users/MARCOS MOREIRA/Downloads/docupodcast studio respaldo estudiar/src/main/java/com/marcosmoreiradev/docupodcaststudio/domain");

    @Test
    void domainCurrentKeepsCoreAggregateWithoutOuterLayerDependencies() throws Exception {
        String project = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/DocuPodcastProject.java");
        String metadata = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMetadata.java");
        String mode = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/ProjectMode.java");

        assertTrue(project.contains("ProjectAssetCatalog assets"));
        assertTrue(project.contains("VoiceLibrary voiceLibrary"));
        assertTrue(project.contains("List<NarrativeLayerAssignment> narrativeLayerAssignments"));
        assertTrue(project.contains("TheatreProjectLayer theatre"));
        assertTrue(metadata.contains("ProjectKind kind"));
        assertTrue(metadata.contains("ProjectMode mode"));
        assertTrue(mode.contains("DOCUMENTARY_STUDIO"));
        assertTrue(mode.contains("NARRATIVE_VIDEO"));
        assertTrue(mode.contains("THEATRE_PRODUCTION"));
        assertTrue(!project.contains("presentation."));
        assertTrue(!project.contains("infrastructure."));
    }

    @Test
    void migrationChecklistCoversOfficialModesExportCenterAndLegacyHiddenSurfaces() throws Exception {
        String checklist = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RunMigrationClosureChecklistUseCase.java");

        assertTrue(checklist.contains("DOCUMENTARY_STUDIO"));
        assertTrue(checklist.contains("NARRATIVE_VIDEO"));
        assertTrue(checklist.contains("THEATRE_PRODUCTION"));
        assertTrue(checklist.contains("FragmentId"));
        assertTrue(checklist.contains("ExportCenterCoordinator"));
        assertTrue(checklist.contains("OPEN_STORYBOARD"));
        assertTrue(checklist.contains("OPEN_AUDIO_JOBS"));
        assertTrue(checklist.contains("Abrir Centro de exportaciones"));
    }

    @Test
    void backupHasNoDomainFilesMissingFromCurrentWhenAvailable() throws Exception {
        Path currentDomain = ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain");
        if (Files.isDirectory(BACKUP_DOMAIN)) {
            Set<String> current = relativeJavaFiles(currentDomain);
            Set<String> backup = relativeJavaFiles(BACKUP_DOMAIN);
            backup.removeAll(current);
            assertTrue(backup.isEmpty(), "Archivos domain solo en respaldo: " + backup);
            return;
        }
        String report = read("DOCUMENTACION_ACTUAL/TANDA_10_CIERRE_ALINEACION/ALIGNMENT_CLOSURE_REPORT.md");
        assertTrue(report.contains("OnlyBackup: 0"));
    }

    private static Set<String> relativeJavaFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> root.relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
