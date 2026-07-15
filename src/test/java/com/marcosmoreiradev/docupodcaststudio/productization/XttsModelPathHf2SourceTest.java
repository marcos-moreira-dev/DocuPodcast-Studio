package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class XttsModelPathHf2SourceTest {
    @Test
    void javaSidePolicyIsTheSinglePathAuthorityBeforePowershellAndPython() throws Exception {
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/XttsModelPathPolicy.java"));
        String command = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        String readiness = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportXttsModelFolderUseCase.java"));
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));

        assertTrue(policy.contains("normalizeModelDirectory"));
        assertTrue(policy.contains("MODEL_FILE_NAME = \"model.pth\""));
        assertTrue(policy.contains("containsDuplicatedModelPth"));
        assertTrue(command.contains("XttsModelPathPolicy.modelDirectoryFromSettings"));
        assertTrue(readiness.contains("XttsModelPathPolicy.modelDirectoryFromSettings"));
        assertTrue(importer.contains("XttsModelPathPolicy.normalizeUserSelectedModelPath"));
        assertTrue(downloader.contains("XttsModelPathPolicy.modelDirectoryFromSettings"));
    }

    @Test
    void noJavaCommandBuilderMayAppendModelPthToModelPth() throws Exception {
        String command = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java"));
        String readiness = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java"));
        String importer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportXttsModelFolderUseCase.java"));

        assertFalse(command.contains("resolve(\"model.pth\")"));
        assertFalse(command.contains("resolve(\"model.pth\").resolve(\"model.pth\")"));
        assertFalse(readiness.contains("resolve(\"model.pth\").resolve(\"model.pth\")"));
        assertFalse(importer.contains("resolve(\"model.pth\").resolve(\"model.pth\")"));
    }
}
