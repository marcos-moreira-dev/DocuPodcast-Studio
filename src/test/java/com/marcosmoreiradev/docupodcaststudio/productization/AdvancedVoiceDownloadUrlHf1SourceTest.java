package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdvancedVoiceDownloadUrlHf1SourceTest {
    @Test
    void settingsNoLongerShowsBareResolveEndpointAsUserEditablePage() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String formModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String operational = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/OperationalSettings.java"));

        assertTrue(settings.contains("Página oficial del modelo de voz"));
        assertTrue(settings.contains("no pegues enlaces directos de descarga"));
        assertFalse(settings.contains("Página del modelo XTTS-v2"));
        assertFalse(settings.contains("no pegues /resolve/main"));
        assertTrue(formModel.contains("normalizeRepositoryUrlForDisplay(current.tts().xttsDownloadBaseUrl())"));
        assertTrue(formModel.contains("normalizeRepositoryUrlForDisplay(xttsDownloadBaseUrl.getText())"));
        assertTrue(operational.contains("DEFAULT_XTTS_DOWNLOAD_BASE_URL = \"https://huggingface.co/coqui/XTTS-v2\""));
        assertFalse(operational.contains("DEFAULT_XTTS_DOWNLOAD_BASE_URL = \"https://huggingface.co/coqui/XTTS-v2/resolve/main/\""));
    }

    @Test
    void downloaderAcceptsCoquiGithubAsCodeSourceButUsesModelRepositoryForArtifacts() throws Exception {
        String downloader = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));

        assertTrue(downloader.contains("COQUI_TTS_SOURCE_REPOSITORY_URL = \"https://github.com/coqui-ai/TTS\""));
        assertTrue(downloader.contains("isCoquiTtsCodeRepository"));
        assertTrue(downloader.contains("return OFFICIAL_MODEL_REPOSITORY_URL"));
        assertTrue(downloader.contains("normalizeDownloadResolveBaseUrl"));
    }
}
