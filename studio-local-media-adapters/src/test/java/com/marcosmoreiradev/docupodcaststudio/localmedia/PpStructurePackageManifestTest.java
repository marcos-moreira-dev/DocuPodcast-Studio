package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PpStructurePackageManifestTest {
    @TempDir Path root;

    @Test
    void acceptsRelocatableVerifiedPackageAndRejectsMovedVirtualEnvironment() throws Exception {
        file("python/python.exe", "portable-python");
        file("bridge/pp_structure_v3.py", "print('offline')");
        String[] modelKeys = {
                "layout_detection_model_dir",
                "text_detection_model_dir",
                "text_recognition_model_dir",
                "table_classification_model_dir",
                "wired_table_structure_recognition_model_dir",
                "wireless_table_structure_recognition_model_dir",
                "wired_table_cells_detection_model_dir",
                "wireless_table_cells_detection_model_dir",
                "table_orientation_classify_model_dir",
                "formula_recognition_model_dir"
        };
        StringBuilder constructor = new StringBuilder();
        for (int index = 0; index < modelKeys.length; index++) {
            if (index > 0) constructor.append(',');
            String directory = "model-" + index;
            constructor.append('"').append(modelKeys[index]).append("\":\"")
                    .append(directory).append('"');
            file("models/" + directory + "/inference.pdmodel",
                    "model-" + index);
        }
        file("models/manifest.json",
                "{\"constructor\":{" + constructor + "}}");
        java.util.List<String> packageFiles = new java.util.ArrayList<>(
                java.util.List.of("python/python.exe",
                        "bridge/pp_structure_v3.py", "models/manifest.json"));
        for (int index = 0; index < modelKeys.length; index++) {
            packageFiles.add("models/model-" + index + "/inference.pdmodel");
        }
        String manifest = """
                {
                  "schemaVersion":1,
                  "pythonVersion":"3.12.11",
                  "paddleVersion":"3.3.0",
                  "paddleOcrVersion":"3.7.0",
                  "profile":"gpu-cu118",
                  "license":"Apache-2.0",
                  "sources":["https://www.paddleocr.ai/"],
                  "files":[
                    %s
                  ]
                }
                """.formatted(String.join(",\n",
                packageFiles.stream().map(this::uncheckedEntry).toList()));
        Files.writeString(root.resolve(PpStructurePackageManifest.FILE_NAME),
                manifest, StandardCharsets.UTF_8);

        assertTrue(PpStructurePackageManifest.verify(root).valid());

        Files.createDirectories(root.resolve(".venv"));
        var invalid = PpStructurePackageManifest.verify(root);
        assertFalse(invalid.valid());
        assertTrue(invalid.issues().contains("non-portable-virtualenv"));
    }

    private void file(String relative, String content) throws Exception {
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private String entry(String relative) throws Exception {
        Path file = root.resolve(relative);
        return """
                {"path":"%s","size":%d,"sha256":"%s"}
                """.formatted(relative, Files.size(file),
                ManagedDownloadPreflightInspector.sha256(file)).strip();
    }

    private String uncheckedEntry(String relative) {
        try {
            return entry(relative);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
