package com.marcosmoreiradev.docupodcaststudio.presentation.welcome;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectJsonReader;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectJsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecentProjectsStoreTest {
    @TempDir
    Path temp;

    @Test
    void rememberPersistsEightMostRecentProjectsInTextFile() throws Exception {
        RecentProjectsStore store = new RecentProjectsStore(temp.resolve("recent-projects.txt"));

        for (int i = 1; i <= 9; i++) {
            Path projectFile = temp.resolve("project-" + i + ".docupodcast.json");
            Files.writeString(projectFile, "{}");
            store.remember(projectFile, "Proyecto " + i);
        }
        List<RecentProjectEntry> entries = store.load();

        assertEquals(8, entries.size());
        assertEquals("Proyecto 9", entries.get(0).displayName());
        assertEquals("Proyecto 2", entries.get(7).displayName());
        assertTrue(Files.readString(store.storeFile()).contains("Proyecto 9\t"));
    }

    @Test
    void rememberDeduplicatesByNormalizedPathAndMovesProjectToTop() {
        RecentProjectsStore store = new RecentProjectsStore(temp.resolve("recent-projects.txt"));
        Path first = temp.resolve("alpha.docupodcast.json");
        Path second = temp.resolve("beta.docupodcast.json");
        writeProject(first);
        writeProject(second);

        store.remember(first, "Alpha");
        store.remember(second, "Beta");
        List<RecentProjectEntry> entries = store.remember(first, "Alpha nuevo");

        assertEquals(2, entries.size());
        assertEquals("Alpha nuevo", entries.get(0).displayName());
        assertEquals(first.toAbsolutePath().normalize(), entries.get(0).projectFile());
        assertEquals("Beta", entries.get(1).displayName());
    }

    @Test
    void rememberPersistsProjectTypeForWelcomeSubtitle() {
        RecentProjectsStore store = new RecentProjectsStore(temp.resolve("recent-projects.txt"));
        Path projectFile = temp.resolve("theatre.docupodcast.json");
        writeProject(projectFile);

        store.remember(projectFile, "Obra", "Produccion teatral");
        List<RecentProjectEntry> entries = store.load();

        assertEquals(1, entries.size());
        assertEquals("Produccion teatral", entries.get(0).typeLabel());
    }

    @Test
    void loadRepairsStaleStoredProjectTypeFromProjectDescriptorAndPolicy() throws Exception {
        RecentProjectsStore store = new RecentProjectsStore(temp.resolve("recent-projects.txt"),
                (projectFile, storedType) -> {
                    try {
                        DocuPodcastProject project = new DocuPodcastProjectJsonReader()
                                .read(Files.readString(projectFile));
                        return new ProjectModePolicy().resolve(project).displayName();
                    } catch (Exception ex) {
                        return storedType;
                    }
                });
        Path projectFile = temp.resolve("aviadores.docupodcast.json");
        writeTheatreProjectWithWrongMode(projectFile);
        Files.writeString(store.storeFile(), "Aviadores Comicos\tEstudio documental\t" + projectFile);

        List<RecentProjectEntry> entries = store.load();

        assertEquals(1, entries.size());
        assertEquals("Producción teatral", entries.get(0).typeLabel());
        assertTrue(Files.readString(store.storeFile()).contains("Producción teatral"));
    }

    @Test
    void loadPrunesProjectsDeletedOrMovedBeforeStartup() throws Exception {
        RecentProjectsStore store = new RecentProjectsStore(temp.resolve("recent-projects.txt"));
        Path existing = temp.resolve("existing.docupodcast.json");
        Path missing = temp.resolve("missing.docupodcast.json");
        Files.writeString(existing, "{}");
        Files.writeString(store.storeFile(), "Missing\t" + missing + System.lineSeparator()
                + "Existing\t" + existing + System.lineSeparator());

        List<RecentProjectEntry> entries = store.load();

        assertEquals(1, entries.size());
        assertEquals("Existing", entries.get(0).displayName());
        String persisted = Files.readString(store.storeFile());
        assertTrue(persisted.contains("Existing\t"));
        assertTrue(!persisted.contains("Missing\t"));
    }

    private static void writeProject(Path projectFile) {
        try {
            Files.writeString(projectFile, "{}");
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static void writeTheatreProjectWithWrongMode(Path projectFile) {
        try {
            DocuPodcastProject project = DocuPodcastProject.createNew("Aviadores Comicos", ProjectMode.DOCUMENTARY_STUDIO)
                    .withTheatre(new TheatreProjectLayer(
                            List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B1")),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of()));
            Files.writeString(projectFile, new DocuPodcastProjectJsonWriter().write(project));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
