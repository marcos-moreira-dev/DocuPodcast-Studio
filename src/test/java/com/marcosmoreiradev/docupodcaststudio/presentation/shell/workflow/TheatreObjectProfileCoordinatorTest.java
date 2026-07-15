package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreObjectProfileCoordinatorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void savingAnObjectProfileCreatesProjectObjectFolderAndTechnicalSheet() throws Exception {
        Path projectFile = tempDirectory.resolve("obra.docupodcast.json");
        ProjectSession session = ProjectSession.opened(DocuPodcastProject.createNew("Obra"), projectFile);
        TheatreObjectProfileCoordinator coordinator = new TheatreObjectProfileCoordinator();

        TheatreObjectProfileCoordinator.SaveResult result = coordinator.save(
                Optional.of(session),
                "",
                "Brujula oxidada",
                "Objeto de utileria con metal envejecido y continuidad en el hangar.");

        Path objectDirectory = tempDirectory.resolve("objetos").resolve("OBJ-BRUJULA-OXIDADA");
        String profile = Files.readString(objectDirectory.resolve("ficha-tecnica.txt"));

        assertTrue(result.saved());
        assertTrue(session.dirty());
        assertTrue(Files.isDirectory(objectDirectory.resolve("imagenes")));
        assertTrue(profile.contains("Ficha tecnica de Brujula oxidada"));
        assertTrue(profile.contains("Objeto de utileria"));
        assertEquals("OBJ-BRUJULA-OXIDADA", session.project().theatre().objects().get(0).id());
    }
}
