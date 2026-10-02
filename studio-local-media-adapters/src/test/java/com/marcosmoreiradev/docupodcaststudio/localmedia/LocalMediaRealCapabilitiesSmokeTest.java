package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real smoke through the same neutral adapters and administration used by the launcher. */
class LocalMediaRealCapabilitiesSmokeTest {
    @Test
    void exercisesEveryRequiredInstalledCapability() throws Exception {
        boolean enabled = Boolean.parseBoolean(System.getProperty("docupodcast.realCapabilitiesSmoke.enabled", "false"));
        Assumptions.assumeTrue(enabled, "real local capability smoke is opt-in");
        Path installation = Path.of(System.getProperty("docupodcast.app.root", ".")).toAbsolutePath().normalize();
        Path runtime = Path.of(System.getProperty("docupodcast.runtime.root", installation.toString()))
                .toAbsolutePath().normalize();
        Set<String> required = parse(System.getProperty("docupodcast.realCapabilitiesSmoke.required", ""));
        assertFalse(required.isEmpty(), "At least one installed capability must be required");
        String preset = System.getProperty("docupodcast.realCapabilitiesSmoke.preset", "").strip();
        Path evidence = evidenceRoot(runtime);
        ComfyUiMemoryProfile memoryProfile = ComfyUiMemoryProfile.from(
                System.getProperty("docupodcast.comfy.memoryProfile", "SAFE_LOW_VRAM"));
        MediaEnginePlatform platform = LocalMediaAdapters.create(new LocalMediaLayout(installation, runtime),
                requested -> ComfyUiLaunchProfile.automatic(
                        requested == null || requested.isBlank()
                                ? memoryProfile : ComfyUiMemoryProfile.from(requested)));
        boolean comfyStarted = false;
        try {
            if (required.contains("image") || required.contains("video") || required.contains("rife")) {
                EngineId owner = required.contains("image") || required.contains("rife")
                        ? ComfyUiImageEngine.ID : ComfyUiVideoGenerationEngine.ID;
                execute(platform, owner, EngineActionId.START);
                comfyStarted = true;
                awaitReady(platform, owner, Duration.ofMinutes(4));
            }
            if (required.contains("piper")) assertSmoke(platform, PiperVoiceEngine.ID, EngineActionId.SMOKE_TEST,
                    "default", evidence, Map.of());
            if (required.contains("xtts")) {
                Path officialPresetSample = installation.resolve(
                        "samples/voices/advanced-presets/hombre_40_popular_ecuador_dialogo/neutral.wav");
                assertTrue(Files.isRegularFile(officialPresetSample),
                        () -> "Missing official XTTS preset sample: " + officialPresetSample);
                assertSmoke(platform, XttsVoiceEngine.ID, EngineActionId.SMOKE_TEST,
                        "unit", evidence, Map.of("speakerFile", officialPresetSample.toString()));
                assertSmoke(platform, XttsVoiceEngine.ID, new EngineActionId("batch-smoke-test"),
                        "batch", evidence, Map.of());
            }
            if (required.contains("image")) {
                String selected = preset.isBlank() ? "draft" : preset;
                String prompt = configuredPrompt();
                assertSmoke(platform, ComfyUiImageEngine.ID, EngineActionId.SMOKE_TEST,
                        selected, evidence, Map.of("presetId", selected,
                                "seed", System.getProperty("docupodcast.realCapabilitiesSmoke.seed", "424242"),
                                "prompt", prompt,
                                "negativePrompt", System.getProperty("docupodcast.realCapabilitiesSmoke.negativePrompt",
                                        "texto, marca de agua, letras deformes"),
                                "width", System.getProperty("docupodcast.realCapabilitiesSmoke.width", "512"),
                                "height", System.getProperty("docupodcast.realCapabilitiesSmoke.height", "512"),
                                "deliveryWidth", System.getProperty(
                                        "docupodcast.realCapabilitiesSmoke.deliveryWidth",
                                        System.getProperty("docupodcast.realCapabilitiesSmoke.width", "512")),
                                "deliveryHeight", System.getProperty(
                                        "docupodcast.realCapabilitiesSmoke.deliveryHeight",
                                        System.getProperty("docupodcast.realCapabilitiesSmoke.height", "512")),
                                "label", System.getProperty("docupodcast.realCapabilitiesSmoke.label", "image-smoke")));
            }
            if (required.contains("rife")) assertSmoke(platform, ComfyUiImageEngine.ID,
                    ComfyUiImageEngineAdministration.RIFE_SMOKE_TEST, "rife-v4.25-lite", evidence, Map.of());
            if (required.contains("video")) {
                String selected = preset.isBlank() ? ComfyUiVideoGenerationEngine.WAN_BALANCED.value() : preset;
                assertSmoke(platform, ComfyUiVideoGenerationEngine.ID, EngineActionId.SMOKE_TEST,
                        selected, evidence, Map.of("presetId", selected, "seed", "424242"));
            }
            if (required.contains("ffmpeg")) assertSmoke(platform, FfmpegVideoRenderEngine.ID,
                    EngineActionId.SMOKE_TEST, "short-timeline", evidence, Map.of());
        } finally {
            if (comfyStarted) {
                EngineId owner = required.contains("image") || required.contains("rife")
                        ? ComfyUiImageEngine.ID : ComfyUiVideoGenerationEngine.ID;
                execute(platform, owner, EngineActionId.STOP);
            }
        }
    }

    private static void assertSmoke(MediaEnginePlatform platform, EngineId engineId, EngineActionId actionId,
                                    String preset, Path evidenceRoot, Map<String, String> inputs)
            throws Exception {
        Instant started = Instant.now();
        Path directory = evidenceRoot.resolve(engineId.value()).resolve(safe(preset));
        Files.createDirectories(directory);
        writePayload(directory, engineId, actionId, preset, inputs);
        try {
            EngineActionResult result = execute(platform, engineId, actionId, inputs);
            assertTrue(result.success(), () -> engineId + ": " + result.message());
            assertFalse(result.artifacts().isEmpty(), () -> engineId + " did not publish smoke artifacts");
            ArrayList<Path> copies = new ArrayList<>();
            for (GenerationArtifact artifact : result.artifacts()) {
                assertEquals("file", artifact.location().getScheme());
                Path source = Path.of(artifact.location());
                Path target = directory.resolve(source.getFileName().toString());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                copies.add(target);
            }
            writeManifest(directory, "PASS", engineId, preset, started, result.message(), copies);
        } catch (Exception | AssertionError failure) {
            writeManifest(directory, state(failure), engineId, preset, started,
                    failure.getMessage(), List.of());
            throw failure;
        }
    }

    private static EngineActionResult execute(MediaEnginePlatform platform, EngineId engineId,
                                              EngineActionId actionId) throws Exception {
        return execute(platform, engineId, actionId, Map.of());
    }

    private static EngineActionResult execute(MediaEnginePlatform platform, EngineId engineId,
                                              EngineActionId actionId, Map<String, String> inputs) throws Exception {
        return platform.administration().require(engineId).execute(
                new EngineActionRequest(engineId, actionId, inputs),
                certificationContext("real-smoke-" + engineId.value()));
    }

    private static ExecutionContext certificationContext(String operationId) {
        long hours;
        try {
            hours = Long.parseLong(System.getProperty("docupodcast.realCapabilitiesSmoke.timeoutHours", "1"));
        } catch (NumberFormatException ignored) {
            hours = 1;
        }
        Duration timeout = Duration.ofHours(Math.max(1, Math.min(6, hours)));
        return new ExecutionContext(operationId, CancellationToken.NONE, ProgressSink.NONE,
                new ExecutionPolicy(timeout, 1), ResourceLease.NONE, GenerationArtifactStaging.NONE);
    }

    private static Path evidenceRoot(Path runtime) {
        String configured = System.getProperty("docupodcast.certification.root", "").strip();
        return configured.isBlank()
                ? runtime.resolve("target/certification/manual").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
    }

    private static void writePayload(Path directory, EngineId engineId, EngineActionId actionId,
                                     String preset, Map<String, String> inputs) throws Exception {
        String json = "{\n  \"engineId\": \"" + json(engineId.value()) + "\",\n"
                + "  \"actionId\": \"" + json(actionId.value()) + "\",\n"
                + "  \"presetId\": \"" + json(preset) + "\",\n"
                + "  \"inputs\": " + mapJson(inputs) + "\n}\n";
        Files.writeString(directory.resolve("payload-neutral.json"), json);
    }

    private static void writeManifest(Path directory, String state, EngineId engineId, String preset,
                                      Instant started, String message, List<Path> artifacts) throws Exception {
        long duration = Duration.between(started, Instant.now()).toMillis();
        String files = artifacts.stream().map(path -> "\"" + json(path.getFileName().toString()) + "\"")
                .reduce((left, right) -> left + ", " + right).orElse("");
        String manifest = "{\n  \"state\": \"" + state + "\",\n"
                + "  \"engineId\": \"" + json(engineId.value()) + "\",\n"
                + "  \"presetId\": \"" + json(preset) + "\",\n"
                + "  \"startedAt\": \"" + started + "\",\n"
                + "  \"durationMs\": " + duration + ",\n"
                + "  \"message\": \"" + json(message) + "\",\n"
                + "  \"artifacts\": [" + files + "]\n}\n";
        Files.writeString(directory.resolve("manifest.json"), manifest);
    }

    private static String state(Throwable failure) {
        String message = failure.getMessage() == null ? "" : failure.getMessage();
        return message.contains("RESOURCE_MISSING") ? "RESOURCE_MISSING" : "FAIL";
    }

    private static String mapJson(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> "\"" + json(entry.getKey()) + "\": \"" + json(entry.getValue()) + "\"")
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "default" : value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void awaitReady(MediaEnginePlatform platform, EngineId engineId, Duration timeout)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (readiness(platform, engineId).ready()) return;
            Thread.sleep(1000);
        }
        fail("Runtime did not become ready: " + engineId);
    }

    private static EngineReadiness readiness(MediaEnginePlatform platform, EngineId id) {
        if (ComfyUiImageEngine.ID.equals(id)) return platform.imageEngines().require(id).inspectReadiness(null);
        return platform.videoGenerationEngines().require(id).inspectReadiness(null);
    }

    private static Set<String> parse(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value != null) for (String item : value.split(",")) if (!item.isBlank()) result.add(item.strip());
        return Set.copyOf(result);
    }

    private static String configuredPrompt() throws Exception {
        String promptFile = System.getProperty("docupodcast.realCapabilitiesSmoke.promptFile", "").strip();
        if (!promptFile.isBlank()) return Files.readString(Path.of(promptFile)).strip();
        return System.getProperty("docupodcast.realCapabilitiesSmoke.prompt",
                "Ilustracion simple de un libro morado sobre fondo blanco").strip();
    }
}
