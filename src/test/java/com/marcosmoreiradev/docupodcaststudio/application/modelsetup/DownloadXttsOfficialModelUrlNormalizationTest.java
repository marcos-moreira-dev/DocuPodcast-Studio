package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DownloadXttsOfficialModelUrlNormalizationTest {
    @Test
    void visibleUrlIsTheBrowsableModelPageNotBareResolveEndpoint() {
        assertEquals("https://huggingface.co/coqui/XTTS-v2",
                DownloadXttsOfficialModelUseCase.normalizeRepositoryUrlForDisplay(
                        "https://huggingface.co/coqui/XTTS-v2/resolve/main/"));
        assertEquals("https://huggingface.co/coqui/XTTS-v2",
                DownloadXttsOfficialModelUseCase.normalizeRepositoryUrlForDisplay(
                        "https://huggingface.co/coqui/XTTS-v2/resolve/main/config.json?download=1"));
    }

    @Test
    void coquiTtsGithubRepositoryIsRecognizedAsCodeSourceNotModelFileBase() {
        assertEquals("https://huggingface.co/coqui/XTTS-v2",
                DownloadXttsOfficialModelUseCase.normalizeRepositoryUrlForDisplay("https://github.com/coqui-ai/TTS"));
        assertEquals("https://huggingface.co/coqui/XTTS-v2/resolve/main/",
                DownloadXttsOfficialModelUseCase.normalizeDownloadResolveBaseUrl("https://github.com/coqui-ai/TTS"));
    }

    @Test
    void downloaderStillBuildsPerFileResolveBaseInternally() {
        String base = DownloadXttsOfficialModelUseCase.normalizeDownloadResolveBaseUrl("https://huggingface.co/coqui/XTTS-v2");
        assertTrue(base.endsWith("/resolve/main/"));
    }
}
