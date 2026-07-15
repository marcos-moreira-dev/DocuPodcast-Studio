package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessJobContractT94SourceTest {
    @Test
    void t94IntroducesCommonLongRunningProcessContractWithoutReplacingAudioJobs() throws IOException {
        String processSnapshot = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/process/ProcessJobSnapshot.java"));
        String processKind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/process/ProcessJobKind.java"));
        String mapper = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/AudioJobProcessMapper.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ProcessApplicationServices.java"));
        String audioJob = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/audio/AudioJobSnapshot.java"));

        assertTrue(processSnapshot.contains("ProcessJobKind"));
        assertTrue(processSnapshot.contains("ProcessJobState"));
        assertTrue(processSnapshot.contains("ProcessJobLogReference"));
        assertTrue(processKind.contains("TTS_AUDIO"));
        assertTrue(processKind.contains("MEDIA_PREPARATION"));
        assertTrue(processKind.contains("ENGINE_SETUP"));
        assertTrue(processKind.contains("VIDEO_RENDER"));
        assertTrue(mapper.contains("AudioJobSnapshot"));
        assertTrue(mapper.contains("ProcessJobSnapshot"));
        assertTrue(services.contains("ListProcessJobsUseCase"));
        assertTrue(audioJob.contains("Persistable snapshot of an audio job"));
    }

    @Test
    void t94IsDocumentedAsAConservativeConvergenceStep() throws IOException {
        String docs = Files.readString(Path.of("docs/productizacion/T94_JOBS_COMUNES_PROCESOS_LARGOS.md"));

        assertTrue(docs.contains("TTS"));
        assertTrue(docs.contains("FFmpeg"));
        assertTrue(docs.contains("video"));
        assertTrue(docs.contains("no reemplaza"));
        assertTrue(docs.contains("contrato común"));
    }
}
