package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class OllamaModelStoreInspectorTest {
    @TempDir Path temp;

    @Test
    void validatesEveryContentAddressedBlobAndDetectsCorruption() throws Exception {
        Path models = temp.resolve("models");
        byte[] content = "vision-model-layer".getBytes(StandardCharsets.UTF_8);
        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
        Path blob = models.resolve("blobs/sha256-" + digest);
        Files.createDirectories(blob.getParent());
        Files.write(blob, content);
        Path manifest = OllamaModelStoreInspector.manifest(
                models, QwenVisualAnalysisEngine.Q8_MODEL);
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, "{\"layers\":[{\"digest\":\"sha256:" + digest
                        + "\",\"size\":" + content.length + "}]}",
                StandardCharsets.UTF_8);

        assertTrue(OllamaModelStoreInspector.inspect(
                models, QwenVisualAnalysisEngine.Q8_MODEL).valid());
        assertTrue(OllamaModelStoreInspector.inspectFast(
                models, QwenVisualAnalysisEngine.Q8_MODEL).valid());

        byte[] corrupt = content.clone();
        corrupt[0] = (byte) (corrupt[0] + 1);
        Files.write(blob, corrupt);
        var invalid = OllamaModelStoreInspector.inspect(models, QwenVisualAnalysisEngine.Q8_MODEL);
        assertFalse(invalid.valid());
        assertTrue(invalid.issues().getFirst().startsWith("blob-corrupt:"));
        assertTrue(OllamaModelStoreInspector.inspectFast(
                models, QwenVisualAnalysisEngine.Q8_MODEL).valid(),
                "readiness uses size and the certified content-addressed digest; preflight rehashes");
    }

    @Test
    void persistsTheManualModelChoiceInsideTheManagedStore() throws Exception {
        Path root = temp.resolve("runtime");
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                root.resolve("ollama.exe"), root.resolve("models"), root.resolve("logs"));

        String legacyModel = "qwen3-vl:4b-instruct-q4_K_M";
        process.rememberPreferredModel(legacyModel);

        assertEquals(legacyModel, process.preferredModel());
        assertTrue(Files.readString(root.resolve("models/selected-visual-model.txt"),
                        StandardCharsets.UTF_8)
                .contains(legacyModel));
    }
}
