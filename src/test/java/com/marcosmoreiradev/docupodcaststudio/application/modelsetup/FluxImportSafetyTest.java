package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class FluxImportSafetyTest {
    @TempDir Path temp;
    @Test void rejectsPlaceholderAndTruncatedTensorData() throws Exception {
        Path file = temp.resolve("ae.safetensors");
        Files.writeString(file, "placeholder");
        assertThrows(IOException.class, () -> FluxComponentImportUseCase.validateTensorContainer(file));
        tensor(file, 4, 1);
        assertThrows(IOException.class, () -> FluxComponentImportUseCase.validateTensorContainer(file));
        tensor(file, 4, 4);
        assertDoesNotThrow(() -> FluxComponentImportUseCase.validateTensorContainer(file));
    }
    @Test void licenseAcknowledgmentsAreSpecificToModel() throws Exception {
        var store = new FluxLicenseAcceptanceStore();
        store.accept(temp, ImageModelPackageProfile.HIGH_QUALITY_FLUX);
        assertTrue(store.accepted(temp, ImageModelPackageProfile.HIGH_QUALITY_FLUX));
        assertFalse(store.accepted(temp, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT));
        store.accept(temp, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT);
        assertTrue(store.accepted(temp, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT));
    }
    @Test void importKeepsOriginalReusesIdenticalFileAndRefusesOverwrite() throws Exception {
        Path source = temp.resolve("download/flux1-kontext-dev.safetensors");
        Files.createDirectories(source.getParent());
        tensor(source, 4, 4);
        byte[] original = Files.readAllBytes(source);
        Path app = temp.resolve("application");
        var importer = new FluxComponentImportUseCase(request -> { throw new AssertionError("No process needed"); });
        var result = importer.importFrom(source, app, null, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT);
        assertTrue(result.success());
        assertTrue(result.userMessage().contains("faltan"));
        assertArrayEquals(original, Files.readAllBytes(source));
        assertFalse(Files.isSameFile(source, app.resolve("models/image/flux1-kontext-dev.safetensors")));
        assertTrue(importer.importFrom(source, app, null, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT).success());
        tensor(source, 5, 5);
        assertFalse(importer.importFrom(source, app, null, ImageModelPackageProfile.ADVANCED_FLUX_KONTEXT).success());
        assertArrayEquals(original, Files.readAllBytes(app.resolve("models/image/flux1-kontext-dev.safetensors")));
    }
    private static void tensor(Path file, int declared, int actual) throws IOException {
        byte[] header = ("{\"x\":{\"dtype\":\"U8\",\"shape\":[" + declared
                + "],\"data_offsets\":[0," + declared + "]}}").getBytes(StandardCharsets.UTF_8);
        ByteBuffer data = ByteBuffer.allocate(8 + header.length + actual).order(ByteOrder.LITTLE_ENDIAN);
        data.putLong(header.length).put(header).put(new byte[actual]);
        Files.write(file, data.array());
    }
}
