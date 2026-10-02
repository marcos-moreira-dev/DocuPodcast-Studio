package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadAdministration;
import com.marcosmoreiradev.docupodcaststudio.media.api.ConfigurationFieldType;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LocalMediaAdministrationCatalogTest {
    @TempDir Path temporary;

    @Test
    void publishesSpecificAdministratorsWithoutArbitraryTargetOrCommandInputs() {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));

        assertEquals(Set.of("piper", "xtts", "qwen3-tts-local", "comfyui", "comfyui-video",
                        "comfyui-real-esrgan", "comfyui-controlnet-tile", "ffmpeg",
                        "qwen3-vl-local", "pp-structure-v3-local", "mathcat-local"),
                platform.administration().entries().stream()
                        .map(item -> item.engineId().value()).collect(Collectors.toSet()));
        Set<String> forbidden = Set.of("target", "command", "arguments", "workingDirectory", "stagingDirectory");
        for (var administration : platform.administration().entries()) {
            assertFalse(administration.actions().isEmpty());
            Set<String> keys = administration.actions().stream()
                    .flatMap(action -> action.inputs().stream())
                    .map(field -> field.key()).collect(Collectors.toSet());
            assertTrue(java.util.Collections.disjoint(keys, forbidden),
                    () -> administration.engineId() + " exposes unsafe input " + keys);
            assertTrue(administration.actions().stream().map(EngineActionDescriptor::id)
                    .anyMatch(id -> "smoke-test".equals(id.value())));
        }
    }

    @Test
    void everyPublishedManagedDownloadHasPreflightAndRealExecutor() {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));

        for (var administration : platform.administration().entries()) {
            boolean publishesDownload = administration.actions().stream().anyMatch(action ->
                    Boolean.parseBoolean(action.metadata().getOrDefault("managedDownload", "false")));
            if (publishesDownload) {
                assertInstanceOf(ManagedDownloadAdministration.class, administration);
            }
        }
    }

    @Test
    void mathCatPhysicalSmokePublishesAReusableCertification() throws Exception {
        MediaEnginePlatform platform =
                LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));

        platform.administration().require(MathCatSpeechEngine.ID).execute(
                new EngineActionRequest(MathCatSpeechEngine.ID,
                        EngineActionId.SMOKE_TEST, Map.of()),
                com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext
                        .defaults("mathcat-physical-certification"));

        assertEquals(
                com.marcosmoreiradev.docupodcaststudio.media.api.ReadinessState.READY,
                platform.contentAnalysisEngines().find(MathCatSpeechEngine.ID)
                        .orElseThrow().inspectReadiness(
                                new com.marcosmoreiradev.docupodcaststudio.media.api
                                        .EngineConfiguration(
                                        MathCatSpeechEngine.ID, Map.of())).state());
        assertTrue(java.nio.file.Files.isDirectory(
                temporary.resolve("state/document-ai-certifications")));
    }

    @Test
    void realEsrganLocationIsReadOnlyAndFileChoiceLivesOnlyOnImportAction() {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));
        var engine = platform.imageSuperResolutionEngines()
                .require(ComfyUiSuperResolutionEngine.ID);

        assertEquals(ConfigurationFieldType.READ_ONLY_PATH,
                engine.configurationSchema().fields().stream()
                        .filter(field -> "modelPath".equals(field.key()))
                        .findFirst().orElseThrow().type());
        var administration = platform.administration().require(ComfyUiSuperResolutionEngine.ID);
        assertTrue(administration.actions().stream()
                .filter(action -> action.id().equals(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId.IMPORT))
                .flatMap(action -> action.inputs().stream())
                .anyMatch(field -> field.type() == ConfigurationFieldType.FILE));
    }

    @Test
    void controlNetTileIsManagedAndItsRuntimeLocationIsReadOnly() {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));
        var engine = platform.imageRefinementEngines().require(ComfyUiTileRefinementEngine.ID);

        assertEquals(ConfigurationFieldType.READ_ONLY_PATH,
                engine.configurationSchema().fields().stream()
                        .filter(field -> "controlNetPath".equals(field.key()))
                        .findFirst().orElseThrow().type());
        var administration = platform.administration().require(ComfyUiTileRefinementEngine.ID);
        assertInstanceOf(ManagedDownloadAdministration.class, administration);
        assertTrue(administration.actions().stream()
                .filter(action -> action.id().equals(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId.IMPORT))
                .flatMap(action -> action.inputs().stream())
                .anyMatch(field -> field.type() == ConfigurationFieldType.FILE));
    }

    @Test
    void controlNetTilePublishesOfficialMetadataAndRunsPreflightBeforeNetwork() throws Exception {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));
        var administration = platform.administration().require(ComfyUiTileRefinementEngine.ID);
        var install = administration.actions().stream()
                .filter(action -> EngineActionId.INSTALL.equals(action.id()))
                .findFirst().orElseThrow();

        assertEquals(1_445_235_023L, install.approximateBytes());
        assertEquals("8aa69b8d391e72c2fced6a650268137dc0ed594cafe8a2c0f8b994799a21979b",
                install.metadata().get("sha256"));
        assertEquals("CreativeML OpenRAIL-M", install.metadata().get("license"));

        var managed = assertInstanceOf(ManagedDownloadAdministration.class, administration);
        var preflight = managed.inspectDownload(new EngineActionRequest(
                ComfyUiTileRefinementEngine.ID, EngineActionId.INSTALL, Map.of()));
        assertEquals(ManagedDownloadState.MISSING, preflight.state());
        assertEquals(1_445_235_023L, preflight.expectedBytes());
    }

    @Test
    void characterConsistencyPublishesOnlyManagedDownloadsWithOfficialMetadata() throws Exception {
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(temporary, temporary));
        var administration = platform.administration().require(ComfyUiImageEngine.ID);
        var downloads = administration.actions().stream()
                .filter(action -> "Consistencia de personajes · IP-Adapter".equals(
                        action.metadata().get("component")))
                .toList();

        assertEquals(4, downloads.size());
        assertTrue(downloads.stream().allMatch(action ->
                Boolean.parseBoolean(action.metadata().getOrDefault("managedDownload", "false"))));
        assertEquals(3_349_465_596L,
                downloads.stream().mapToLong(EngineActionDescriptor::approximateBytes).sum());
        assertTrue(downloads.stream().anyMatch(action ->
                "a1c250be40455cc61a43da1201ec3f1edaea71214865fb47f57927e06cbe4996"
                        .equals(action.metadata().get("sha256"))));

        var managed = assertInstanceOf(ManagedDownloadAdministration.class, administration);
        for (EngineActionDescriptor action : downloads) {
            var preflight = managed.inspectDownload(new EngineActionRequest(
                    ComfyUiImageEngine.ID, action.id(), Map.of()));
            assertEquals(ManagedDownloadState.MISSING, preflight.state());
            assertEquals(action.approximateBytes(), preflight.expectedBytes());
        }
    }
}
