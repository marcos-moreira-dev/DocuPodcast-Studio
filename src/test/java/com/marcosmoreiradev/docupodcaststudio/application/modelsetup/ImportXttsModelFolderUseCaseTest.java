package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportXttsModelFolderUseCaseTest {
    @Test
    void copiesUserSelectedModelFolderIntoRuntimeModelsFolder() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-xtts-import-root");
        Path source = Files.createTempDirectory("docupodcast-xtts-import-source");
        Files.writeString(source.resolve("config.json"), "{}");
        Files.writeString(source.resolve("model.pth"), "weights");
        Files.writeString(source.resolve("vocab.json"), "{}");
        Files.writeString(source.resolve("speakers_xtts.pth"), "speakers");
        Files.writeString(source.resolve("dvae.pth"), "dvae");
        Files.writeString(source.resolve("mel_stats.pth"), "mel");
        Files.createDirectories(source.resolve("nested"));
        Files.writeString(source.resolve("nested/extra.txt"), "kept");

        XttsModelImportReport report = new ImportXttsModelFolderUseCase().importFrom(source, OperationalSettings.defaults(), root);

        assertTrue(report.success(), report.userMessage());
        assertTrue(Files.exists(root.resolve("models/tts/xtts/config.json")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/model.pth")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/vocab.json")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/speakers_xtts.pth")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/dvae.pth")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/mel_stats.pth")));
        assertTrue(Files.exists(root.resolve("models/tts/xtts/nested/extra.txt")));
    }

    @Test
    void rejectsFolderThatDoesNotContainConcreteModelPth() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-xtts-import-root-missing");
        Path source = Files.createTempDirectory("docupodcast-xtts-import-source-missing");
        Files.writeString(source.resolve("config.json"), "{}");
        Files.writeString(source.resolve("weights.safetensors"), "weights");
        Files.writeString(source.resolve("vocab.json"), "{}");
        Files.writeString(source.resolve("speakers_xtts.pth"), "speakers");
        Files.writeString(source.resolve("dvae.pth"), "dvae");
        Files.writeString(source.resolve("mel_stats.pth"), "mel");

        XttsModelImportReport report = new ImportXttsModelFolderUseCase().importFrom(source, OperationalSettings.defaults(), root);

        assertFalse(report.success());
        assertTrue(report.userMessage().contains("model.pth"));
    }
}
