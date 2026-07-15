package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DEMO-READINESS-HF1 shows what a bundled demo operationally provides before creation. */
final class DemoReadinessHf1SourceTest {
    @Test
    void exampleDialogShowsOperationalReadinessBeforeCreatingDemoProject() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/InspectExampleProjectReadinessUseCase.java"));
        String report = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/examples/ExampleProjectReadinessReport.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/examples/ExampleProjectDialog.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ExampleApplicationServices.java"));

        assertTrue(useCase.contains("sourceDocumentResource"));
        assertTrue(useCase.contains("visualBindings"));
        assertTrue(report.contains("Listo para demo visual"));
        assertTrue(report.contains("asociaciones visuales listas para el rail"));
        assertTrue(dialog.contains("readiness.inspect(example)"));
        assertTrue(dialog.contains("example-project-readiness"));
        assertTrue(services.contains("inspectReadiness"));
    }
}
