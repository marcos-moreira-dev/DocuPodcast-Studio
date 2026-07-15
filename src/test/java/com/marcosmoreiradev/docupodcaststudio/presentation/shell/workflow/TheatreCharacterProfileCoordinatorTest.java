package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreCharacterProfileCoordinatorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void savingACharacterProfileCreatesProjectCharacterFolderAndTechnicalSheet() throws Exception {
        Path projectFile = tempDirectory.resolve("obra.docupodcast.json");
        ProjectSession session = ProjectSession.opened(DocuPodcastProject.createNew("Obra"), projectFile);
        TheatreCharacterProfileCoordinator coordinator = new TheatreCharacterProfileCoordinator();

        TheatreCharacterProfileCoordinator.SaveResult result = coordinator.save(
                Optional.of(session),
                "CHR-NARRADOR",
                "NARRADOR",
                "Narrador externo con voz sobria y funcion de enlace entre escenas.");

        Path characterDirectory = tempDirectory.resolve("personajes").resolve("CHR-NARRADOR");
        String profile = Files.readString(characterDirectory.resolve("ficha-tecnica.txt"));

        assertTrue(result.saved());
        assertTrue(session.dirty());
        assertTrue(Files.isDirectory(characterDirectory.resolve("imagenes")));
        assertTrue(profile.contains("Ficha tecnica de NARRADOR"));
        assertTrue(profile.contains("Narrador externo"));
    }
}
