package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.LocalUserMediaAssetFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportUserMediaAssetUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void importsMp3AsAssignableAudioClip() throws Exception {
        Path projectFile = tempDir.resolve("Obra").resolve("Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("pajaritos cantando.mp3");
        Files.writeString(source, "audio");
        ImportUserMediaAssetUseCase useCase = new ImportUserMediaAssetUseCase(
                new LocalUserMediaAssetFileRepository(), readyFakeExtractor());

        UserMediaImportResult result = useCase.importMedia(DocuPodcastProject.empty("Obra"), projectFile, source);

        assertEquals(UserMediaImportStatus.AUDIO_IMPORTED, result.status());
        assertEquals(ProjectAssetKind.AUDIO_CLIP, result.audioAsset().kind());
        assertEquals(1, result.project().assets().size());
        assertTrue(result.message().contains("Audio importado"));
    }


    @Test
    void normalizesM4aAsAssignableAudioClipWhenFfmpegNormalizerIsConfigured() throws Exception {
        Path projectFile = tempDir.resolve("Obra").resolve("Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("voz celular.m4a");
        Files.writeString(source, "audio");
        ImportUserMediaAssetUseCase useCase = new ImportUserMediaAssetUseCase(
                new LocalUserMediaAssetFileRepository(), readyFakeExtractor(), readyFakeNormalizer());

        UserMediaImportResult result = useCase.importMedia(DocuPodcastProject.empty("Obra"), projectFile, source);

        assertEquals(UserMediaImportStatus.AUDIO_NORMALIZED, result.status());
        assertEquals(ProjectAssetKind.AUDIO_CLIP, result.audioAsset().kind());
        assertTrue(result.audioAsset().relativePath().endsWith(".wav"));
        assertTrue(Files.isRegularFile(projectFile.getParent().resolve(result.audioAsset().relativePath())));
    }

    @Test
    void extractsVideoAudioAsAssignableAudioClipAndKeepsOriginalVideoProvenance() throws Exception {
        Path projectFile = tempDir.resolve("Obra").resolve("Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("escena.mp4");
        Files.writeString(source, "video");
        ImportUserMediaAssetUseCase useCase = new ImportUserMediaAssetUseCase(
                new LocalUserMediaAssetFileRepository(), readyFakeExtractor());

        UserMediaImportResult result = useCase.importMedia(DocuPodcastProject.empty("Obra"), projectFile, source);

        assertEquals(UserMediaImportStatus.VIDEO_AUDIO_EXTRACTED, result.status());
        assertEquals(ProjectAssetKind.AUDIO_CLIP, result.audioAsset().kind());
        assertEquals(ProjectAssetKind.VIDEO_SOURCE, result.originalVideoAsset().kind());
        assertEquals(2, result.project().assets().size());
        assertTrue(Files.isRegularFile(projectFile.getParent().resolve(result.audioAsset().relativePath())));
    }

    @Test
    void blocksVideoImportWhenExtractionGatewayIsNotReady() throws Exception {
        Path projectFile = tempDir.resolve("Obra").resolve("Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("escena.mov");
        Files.writeString(source, "video");
        ImportUserMediaAssetUseCase useCase = new ImportUserMediaAssetUseCase(
                new LocalUserMediaAssetFileRepository(), unavailableExtractor());

        java.io.IOException exception = assertThrows(java.io.IOException.class,
                () -> useCase.importMedia(DocuPodcastProject.empty("Obra"), projectFile, source));
        assertTrue(exception.getMessage().contains("FFmpeg"));
    }

    @Test
    void preparesAudioInPendingThenCommitsAndDeletesOwnedFile() throws Exception {
        Path projectFile = tempDir.resolve("Obra").resolve("Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        Path source = tempDir.resolve("ambiente.wav");
        Files.writeString(source, "wav");
        ImportUserMediaAssetUseCase useCase = new ImportUserMediaAssetUseCase(
                new LocalUserMediaAssetFileRepository(), readyFakeExtractor(), readyFakeNormalizer(),
                new UserMediaFormatPolicy(), path -> 12.5);

        PreparedAudioAsset prepared = useCase.prepareAudio(projectFile, source);

        assertTrue(prepared.pendingPath().toString().replace('\\', '/').contains("media/audio/.pending/"));
        assertTrue(Files.isRegularFile(prepared.pendingPath()));
        UserMediaImportResult committed = useCase.commitPreparedAudio(DocuPodcastProject.empty("Obra"), projectFile, prepared);
        Path committedFile = projectFile.getParent().resolve(committed.audioAsset().relativePath());
        assertTrue(Files.isRegularFile(committedFile));
        assertTrue(Files.notExists(prepared.pendingPath()));
        assertTrue(useCase.deleteProjectAudio(projectFile, committedFile));
        assertTrue(Files.notExists(committedFile));
    }


    private static AudioNormalizationGateway readyFakeNormalizer() {
        return new AudioNormalizationGateway() {
            @Override
            public boolean ready() {
                return true;
            }

            @Override
            public String readinessMessage() {
                return "fake ffmpeg normalizer ready";
            }

            @Override
            public AudioNormalizationResult normalize(Path sourceAudioFile, Path targetAudioFile, AudioNormalizationProfile profile) throws java.io.IOException {
                Files.writeString(targetAudioFile, "wav-normalized");
                return new AudioNormalizationResult(sourceAudioFile, targetAudioFile, profile, true, "Audio normalizado con normalizador fake.");
            }
        };
    }

    private static VideoAudioExtractionGateway readyFakeExtractor() {
        return new VideoAudioExtractionGateway() {
            @Override
            public boolean ready() {
                return true;
            }

            @Override
            public String readinessMessage() {
                return "fake ready";
            }

            @Override
            public VideoAudioExtractionResult extractAudio(Path sourceVideoFile, Path targetAudioFile) throws java.io.IOException {
                Files.writeString(targetAudioFile, "wav");
                return new VideoAudioExtractionResult(sourceVideoFile, targetAudioFile, true, "Audio extraído con extractor fake.");
            }
        };
    }

    private static VideoAudioExtractionGateway unavailableExtractor() {
        return new VideoAudioExtractionGateway() {
            @Override
            public boolean ready() {
                return false;
            }

            @Override
            public String readinessMessage() {
                return "sin FFmpeg";
            }

            @Override
            public VideoAudioExtractionResult extractAudio(Path sourceVideoFile, Path targetAudioFile) {
                return new VideoAudioExtractionResult(sourceVideoFile, targetAudioFile, false, "no disponible");
            }
        };
    }

}
