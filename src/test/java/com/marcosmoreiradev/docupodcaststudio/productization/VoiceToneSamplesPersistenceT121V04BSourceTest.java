package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceToneSamplesPersistenceT121V04BSourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void voiceLibraryPersistsReferenceSampleSets() throws Exception {
        String library = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceLibrary.java"));
        String writer = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java"));
        String reader = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java"));

        assertTrue(library.contains("List<VoiceReferenceSampleSet> referenceSampleSets"));
        assertTrue(library.contains("referenceSampleSetByVoiceId"));
        assertTrue(writer.contains("referenceSampleSets"));
        assertTrue(reader.contains("readReferenceSampleSets"));
    }

    @Test
    void importedSamplesAreStoredByVoiceAndTone() throws Exception {
        String request = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceSampleImportRequest.java"));
        String useCase = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/ImportVoiceSampleUseCase.java"));

        assertTrue(request.contains("VoiceReferenceTone tone"));
        assertTrue(request.contains("forOwnVoiceTone"));
        assertTrue(useCase.contains("request.tone().name()"));
        assertTrue(useCase.contains("VoiceReferenceSample"));
        assertTrue(useCase.contains("withReferenceSample"));
    }

    @Test
    void materializedVoiceLibraryIncludesToneSampleSets() throws Exception {
        String repository = Files.readString(ROOT.resolve("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/voice/VoiceLibraryWorkspaceFileRepository.java"));

        assertTrue(repository.contains("writeReferenceSampleSets"));
        assertTrue(repository.contains("sample.tone().name()"));
        assertTrue(repository.contains("sample.fileUri()"));
    }
}
