package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail for T122-C01: legacy script remains internal behind a prepared-reading projection boundary. */
final class PreparedReadingProjectionT122C01SourceTest {
    @Test
    void preparedReadingProjectionBoundaryExists() throws Exception {
        String projection = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/reading/PreparedReadingProjection.java");
        String builder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/reading/BuildPreparedReadingProjectionUseCase.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ScriptApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");

        assertTrue(projection.contains("record PreparedReadingProjection"));
        assertTrue(projection.contains("narrationScript"));
        assertTrue(builder.contains("BuildPreparedReadingProjectionUseCase"));
        assertTrue(builder.contains("BuildNarrationScriptUseCase"));
        assertTrue(services.contains("buildPreparedReadingProjection"));
        assertTrue(factory.contains("new BuildPreparedReadingProjectionUseCase"));
    }

    @Test
    void documentCoordinatorUsesPreparedReadingBoundary() throws Exception {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentNarrationCoordinator.java");
        assertTrue(coordinator.contains("buildPreparedReadingProjection()"));
        assertTrue(coordinator.contains("Lectura preparada: %d fragmentos"));
        assertFalse(coordinator.contains("Proyección interna de narración preparada"));
    }

    @Test
    void compatibilityClassesAreDocumentedAsInternalOnly() throws Exception {
        String script = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/script/NarrationScriptDocument.java");
        String builder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/script/BuildNarrationScriptUseCase.java");
        String audioRequest = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioGenerationRequest.java");
        String render = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/render/BuildNarrationRenderPlanUseCase.java");

        assertTrue(script.contains("Internal compatibility payload"));
        assertTrue(script.contains("must not be presented to the user"));
        assertTrue(builder.contains("Compatibility builder"));
        assertTrue(audioRequest.contains("PreparedReadingProjection"));
        assertTrue(render.contains("PreparedReadingProjection"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
