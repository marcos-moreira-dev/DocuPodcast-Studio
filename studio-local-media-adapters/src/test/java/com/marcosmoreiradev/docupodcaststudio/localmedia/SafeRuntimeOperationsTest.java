package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SafeRuntimeOperationsTest {
    @TempDir Path temporary;

    @Test
    void rejectsDestinationsOutsideRuntimeRoot() {
        SafeRuntimeOperations operations = new SafeRuntimeOperations(temporary.resolve("runtime"));
        assertThrows(IOException.class, () -> operations.target("../outside.bin"));
        assertThrows(IOException.class, () -> operations.target(""));
    }

    @Test
    void importsOnlyToAdapterSelectedDestination() throws Exception {
        Path runtime = temporary.resolve("runtime");
        Path source = Files.writeString(temporary.resolve("voice.onnx"), "model");
        SafeRuntimeOperations operations = new SafeRuntimeOperations(runtime);

        operations.importFile(source, "models/tts/piper/voices/default.onnx",
                ExecutionContext.defaults("safe-import"));

        assertEquals("model", Files.readString(runtime.resolve("models/tts/piper/voices/default.onnx")));
        assertFalse(Files.exists(temporary.resolve("outside.onnx")));
    }

    @Test
    void validatesImportedModelBeforeAtomicPublicationAndPreservesPreviousModelOnFailure() throws Exception {
        Path runtime = temporary.resolve("runtime");
        Path target = runtime.resolve("models/upscale/model.pth");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "modelo-válido-anterior");
        Path corrupt = Files.writeString(temporary.resolve("corrupt.pth"), "corrupto");
        SafeRuntimeOperations operations = new SafeRuntimeOperations(runtime);

        assertThrows(IOException.class, () -> operations.importFileVerified(
                corrupt,
                "models/upscale/model.pth",
                ExecutionContext.defaults("verified-import"),
                staged -> {
                    throw new IOException("integridad inválida");
                }));

        assertEquals("modelo-válido-anterior", Files.readString(target));

        Path valid = Files.writeString(temporary.resolve("valid.pth"), "modelo-nuevo");
        operations.importFileVerified(
                valid,
                "models/upscale/model.pth",
                ExecutionContext.defaults("verified-import"),
                staged -> assertEquals("modelo-nuevo", Files.readString(staged)));

        assertEquals("modelo-nuevo", Files.readString(target));
    }

    @Test
    void validInstalledResourceIsReusedWithoutAnyHttpRequest() throws Exception {
        Path runtime = temporary.resolve("runtime");
        Path target = runtime.resolve("models/upscale/model.pth");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "official");
        SafeRuntimeOperations operations = new SafeRuntimeOperations(runtime);

        operations.downloadVerified(
                "http://127.0.0.1:1/must-not-be-called",
                "models/upscale/model.pth",
                "upscaler",
                ExecutionContext.defaults("reuse"),
                ManagedDownloadDecision.USE_EXISTING,
                staged -> {
                    if (!"official".equals(Files.readString(staged))) {
                        throw new IOException("invalid");
                    }
                });

        assertEquals("official", Files.readString(target));
    }

    @Test
    void invalidInstalledResourceCannotBeReplacedWithoutExplicitRedownload() throws Exception {
        Path runtime = temporary.resolve("runtime");
        Path target = runtime.resolve("models/upscale/model.pth");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "corrupt");
        SafeRuntimeOperations operations = new SafeRuntimeOperations(runtime);

        IOException failure = assertThrows(IOException.class, () -> operations.downloadVerified(
                "http://127.0.0.1:1/must-not-be-called",
                "models/upscale/model.pth",
                "upscaler",
                ExecutionContext.defaults("guard"),
                ManagedDownloadDecision.USE_EXISTING,
                staged -> {
                    if (!"official".equals(Files.readString(staged))) {
                        throw new IOException("invalid");
                    }
                }));

        assertTrue(failure.getMessage().contains("decisión explícita"));
        assertEquals("corrupt", Files.readString(target));
    }

    @Test
    void overlaysComplementaryRuntimeZipsAndPublishesOnlyAfterValidation() throws Exception {
        Path runtime = temporary.resolve("runtime");
        Path binaryZip = zip("binary.zip", "llama-tts.exe", "exe", "llama.dll", "llama");
        Path cudaZip = zip("cuda.zip", "cudart64_12.dll", "cuda", "cublas64_12.dll", "blas");
        SafeRuntimeOperations operations = new SafeRuntimeOperations(runtime);

        operations.installZipOverlayDirectoryVerified(List.of(binaryZip, cudaZip),
                "tools/qwen3-tts/llama.cpp", "qwen3-tts-local",
                ExecutionContext.defaults("overlay"), staged -> {
                    assertTrue(Files.isRegularFile(staged.resolve("llama-tts.exe")));
                    assertTrue(Files.isRegularFile(staged.resolve("cudart64_12.dll")));
                });

        Path installed = runtime.resolve("tools/qwen3-tts/llama.cpp");
        assertEquals("exe", Files.readString(installed.resolve("llama-tts.exe")));
        assertEquals("cuda", Files.readString(installed.resolve("cudart64_12.dll")));
    }

    private Path zip(String name, String... entries) throws Exception {
        Path archive = temporary.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (int index = 0; index < entries.length; index += 2) {
                zip.putNextEntry(new ZipEntry(entries[index]));
                zip.write(entries[index + 1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }
}
