package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PpStructureFailureTest {
    @TempDir Path root;

    @Test
    void reportsGpuOomWithoutSilentlyRetryingOnCpu() throws Exception {
        Path runtime = root.resolve("runtime");
        Path python = file(runtime, "python/python.exe", "fixture");
        Path script = file(runtime, "bridge/pp_structure_v3.py", "fixture");
        String[] modelKeys = {"layout_detection_model_dir",
                "text_detection_model_dir", "text_recognition_model_dir",
                "table_classification_model_dir",
                "wired_table_structure_recognition_model_dir",
                "wireless_table_structure_recognition_model_dir",
                "wired_table_cells_detection_model_dir",
                "wireless_table_cells_detection_model_dir",
                "table_orientation_classify_model_dir",
                "formula_recognition_model_dir"};
        StringBuilder constructor = new StringBuilder();
        java.util.ArrayList<Path> files =
                new java.util.ArrayList<>(List.of(python, script));
        for (int index = 0; index < modelKeys.length; index++) {
            if (index > 0) constructor.append(',');
            String directory = "model-" + index;
            constructor.append('"').append(modelKeys[index]).append("\":\"")
                    .append(directory).append('"');
            files.add(file(runtime, "models/" + directory + "/model.bin",
                    "fixture-" + index));
        }
        Path modelsManifest = file(runtime, "models/manifest.json",
                "{\"constructor\":{" + constructor + "}}");
        files.add(modelsManifest);
        Files.writeString(runtime.resolve(PpStructurePackageManifest.FILE_NAME), """
                {"schemaVersion":1,"pythonVersion":"3.12.11","paddleVersion":"3.3.0",
                "paddleOcrVersion":"3.7.0","profile":"gpu-cu118",
                "license":"Apache-2.0","sources":["https://www.paddleocr.ai/"],
                "files":[%s]}
                """.formatted(String.join(",",
                files.stream().map(path -> uncheckedEntry(runtime, path)).toList())),
                StandardCharsets.UTF_8);
        Path image = file(root, "page.png", "png-fixture");
        PpStructureV3Engine engine = new PpStructureV3Engine(
                new EngineConfiguration(PpStructureV3Engine.ID, Map.of(
                        "runtime", runtime.toString(),
                        "python", python.toString(),
                        "script", script.toString(),
                        "models", runtime.resolve("models").toString())),
                EngineCertificationStore.none(),
                builder -> {
                    Files.writeString(builder.redirectOutput().file().toPath(),
                            "CUDA out of memory", StandardCharsets.UTF_8);
                    return new ProcessBuilder("cmd", "/c", "exit /b 1").start();
                });
        ExecutionContext context = new ExecutionContext("pp-oom", CancellationToken.NONE,
                ProgressSink.NONE, ExecutionPolicy.defaults(), ResourceLease.NONE,
                GenerationArtifactStaging.NONE, ComputePreference.preferGpu(true));

        EngineExecutionException failure = assertThrows(EngineExecutionException.class,
                () -> engine.analyze(new ContentAnalysisRequest(
                        ContentAnalysisOperation.LAYOUT_ANALYSIS,
                        List.of(new AnalysisVisualInput(image, "page", "")),
                        "layout", "", "es", "", Map.of()), context));

        assertEquals(EngineDiagnosticCode.OOM, failure.code());
        assertEquals("gpu", failure.diagnostics().get("requestedDevice"));
    }

    private Path file(Path base, String relative, String text) throws Exception {
        Path target = base.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, text, StandardCharsets.UTF_8);
        return target;
    }

    private String entry(Path runtime, Path file) throws Exception {
        return """
                {"path":"%s","size":%d,"sha256":"%s"}
                """.formatted(runtime.relativize(file).toString().replace('\\', '/'),
                Files.size(file), ManagedDownloadPreflightInspector.sha256(file)).strip();
    }

    private String uncheckedEntry(Path runtime, Path file) {
        try {
            return entry(runtime, file);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
