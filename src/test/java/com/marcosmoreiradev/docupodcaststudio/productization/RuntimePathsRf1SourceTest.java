package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** RUNTIME-PATHS-RF1 protects the central runtime layout for tools, models, smoke and voices. */
final class RuntimePathsRf1SourceTest {
    @Test
    void runtimeArtifactPathsCentralizesCriticalToolAndModelLocations() throws Exception {
        String paths = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RuntimeArtifactPaths.java");
        assertTrue(paths.contains("xttsWrapperDirectory"));
        assertTrue(paths.contains("xttsPythonExecutable"));
        assertTrue(paths.contains("xttsSynthesizeScript"));
        assertTrue(paths.contains("xttsPowerShellScript"));
        assertTrue(paths.contains("xttsModelDirectory"));
        assertTrue(paths.contains("xttsSmokeDirectory"));
        assertTrue(paths.contains("xttsCudaSmokeManifest"));
        assertTrue(paths.contains("piperExecutable"));
        assertTrue(paths.contains("ffmpegExecutable"));
        assertTrue(paths.contains("voiceSamplesDirectory"));
        assertTrue(paths.contains("voiceTempRecordingsDirectory"));
    }

    @Test
    void xttsSmokeUsesRuntimeArtifactPathsInsteadOfDuplicatedStrings() throws Exception {
        String runCuda = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/RunXttsCudaSmokeUseCase.java");
        String inspectCuda = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/InspectXttsCudaSmokeUseCase.java");
        String inspectReadiness = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectXttsSmokeTestUseCase.java");
        assertTrue(runCuda.contains("RuntimeArtifactPaths.fromRoot"));
        assertTrue(runCuda.contains("xttsPythonExecutable()"));
        assertTrue(runCuda.contains("xttsCudaSmokeManifest()"));
        assertTrue(inspectCuda.contains("RuntimeArtifactPaths.fromRoot"));
        assertTrue(inspectReadiness.contains("RuntimeArtifactPaths.fromRoot"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
