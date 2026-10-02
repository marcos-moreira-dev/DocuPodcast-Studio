package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComfyUiRuntimePreparationTest {
    @TempDir Path temporary;

    @Test
    void repairsAllLocationDependentFilesAfterInstallationIsMoved() throws Exception {
        Path oldRoot = Path.of("C:/Users/Someone/Downloads/old-copy");
        Path python = temporary.resolve("tools/image/venv/Scripts/python.exe");
        Path main = temporary.resolve("tools/image/ComfyUI/main.py");
        Path models = temporary.resolve("models/image");
        Path extra = temporary.resolve("tools/image/extra_model_paths.yaml");
        Path managedPython = temporary.resolve("tools/python/python-nuget-3.10.11/tools/python.exe");
        Files.createDirectories(python.getParent());
        Files.createDirectories(main.getParent());
        Files.createDirectories(models);
        Files.createDirectories(managedPython.getParent());
        Files.write(python, new byte[]{0});
        Files.write(main, new byte[]{0});
        Files.write(managedPython, new byte[]{0});
        Files.writeString(extra, "base_path: \"" + oldRoot + "/models/image\"\n", StandardCharsets.UTF_8);
        Files.writeString(temporary.resolve("tools/image/venv/pyvenv.cfg"),
                "home = " + oldRoot.resolve("tools/python").toString() + "\n"
                        + "include-system-site-packages = false\nversion = 3.10.11\n",
                StandardCharsets.UTF_8);

        RuntimeAssetCatalog assets = new RuntimeAssetCatalog(temporary, Map.of(
                ComfyUiImageEngine.ID, Map.of(
                        "models", models,
                        "extraModelPaths", extra,
                        "python", python)));

        var changes = ComfyUiRuntimePreparation.prepare(assets);

        String yaml = Files.readString(extra, StandardCharsets.UTF_8);
        String pyvenv = Files.readString(temporary.resolve("tools/image/venv/pyvenv.cfg"),
                StandardCharsets.UTF_8);
        assertFalse(yaml.contains("old-copy"));
        assertTrue(yaml.contains(models.toAbsolutePath().toString().replace('\\', '/')));
        assertTrue(yaml.contains("text_encoders: text_encoders"));
        assertFalse(pyvenv.contains("old-copy"));
        assertTrue(pyvenv.contains(managedPython.getParent().toString()));
        assertEquals(2, changes.size());
        assertFalse(Files.exists(extra.resolveSibling("extra_model_paths.yaml.staging")));
    }
}
