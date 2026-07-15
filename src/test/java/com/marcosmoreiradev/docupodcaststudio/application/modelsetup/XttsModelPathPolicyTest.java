package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsModelPathPolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizesModelPthFileToParentFolder() {
        Path modelFile = tempDir.resolve("recursos locales IA avanzada").resolve("model.pth");

        Path normalized = XttsModelPathPolicy.normalizeUserSelectedModelPath(modelFile, tempDir);

        assertEquals(modelFile.getParent().toAbsolutePath().normalize(), normalized);
        assertFalse(XttsModelPathPolicy.containsDuplicatedModelPth(normalized.resolve("model.pth")));
    }

    @Test
    void resolvesDefaultModelsRootToXttsFolder() {
        OperationalSettings settings = OperationalSettings.defaults();

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(tempDir.resolve("models/tts/xtts").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void keepsConcreteXttsFolderSelectedInSettings() {
        Path concrete = tempDir.resolve("custom-xtts");
        OperationalSettings settings = withModelsDirectory(concrete.toString());

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(concrete.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void settingsPointingToModelPthAreRepairedBeforeCommandBuilding() {
        Path modelFile = tempDir.resolve("custom-xtts").resolve("model.pth");
        OperationalSettings settings = withModelsDirectory(modelFile.toString());

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(modelFile.getParent().toAbsolutePath().normalize(), resolved);
        assertFalse(resolved.toString().replace('\\', '/').contains("model.pth/model.pth"));
    }

    @Test
    void settingsPointingToDuplicatedModelPthAreCollapsed() {
        Path duplicated = tempDir.resolve("custom-xtts").resolve("model.pth").resolve("model.pth");
        OperationalSettings settings = withModelsDirectory(duplicated.toString());

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(tempDir.resolve("custom-xtts").toAbsolutePath().normalize(), resolved);
        assertFalse(resolved.toString().replace('\\', '/').contains("model.pth/model.pth"));
    }

    @Test
    void legacyAdvancedVoiceFolderResolvesToPortableModelDirectory() {
        OperationalSettings settings = withModelsDirectory(tempDir.resolve("recursos locales IA avanzada/model.pth").toString());

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(tempDir.resolve("models/tts/xtts").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void portableCompleteModelWinsOverStaleAbsoluteSetting() throws IOException {
        Path portable = tempDir.resolve("models/tts/xtts");
        createCompleteModel(portable);
        OperationalSettings settings = withModelsDirectory(tempDir.resolve("old-folder/model.pth").toString());

        Path resolved = XttsModelPathPolicy.modelDirectoryFromSettings(settings, tempDir);

        assertEquals(portable.toAbsolutePath().normalize(), resolved);
        assertTrue(Files.isRegularFile(resolved.resolve("model.pth")));
    }

    private static OperationalSettings withModelsDirectory(String modelsDirectory) {
        return new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Voz IA avanzada", "es", "VOC-NARRATOR", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings(modelsDirectory, "exports"),
                OperationalSettings.DiagnosticSettings.defaults());
    }

    private static void createCompleteModel(Path folder) throws IOException {
        Files.createDirectories(folder);
        for (String name : new String[]{"config.json", "model.pth", "vocab.json", "speakers_xtts.pth", "dvae.pth", "mel_stats.pth"}) {
            Files.writeString(folder.resolve(name), "ok");
        }
    }
}
