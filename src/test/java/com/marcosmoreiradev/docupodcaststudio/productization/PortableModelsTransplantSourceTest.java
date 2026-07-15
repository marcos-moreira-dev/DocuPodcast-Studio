package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** MODEL-PORT1 lets users copy a previously downloaded models folder into a new tanda. */
class PortableModelsTransplantSourceTest {
    @Test
    void advancedVoiceInspectionAndCommandPreferUsableLocalModelsFolder() throws Exception {
        String inspector = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSetupReadinessUseCase.java");
        String command = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java");
        String downloader = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java");

        assertTrue(inspector.contains("RuntimeArtifactPaths"));
        assertTrue(inspector.contains("xttsModelDirectory"));
        assertTrue(inspector.contains("Se detectó una carpeta models/ local trasplantada"));
        assertTrue(command.contains("usableXttsModelFolder"));
        assertTrue(command.contains("return portableRoot"));
        assertTrue(downloader.contains("return portableTarget"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
