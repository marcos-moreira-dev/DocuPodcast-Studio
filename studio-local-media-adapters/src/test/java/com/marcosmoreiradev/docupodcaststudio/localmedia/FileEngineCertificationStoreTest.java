package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineCertificationRecord;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class FileEngineCertificationStoreTest {
    @TempDir
    Path temporary;

    @Test
    void persistsOutsideProjectsAndInvalidatesOneModelOnly() throws Exception {
        FileEngineCertificationStore store =
                new FileEngineCertificationStore(temporary.resolve("state"));
        EngineId engine = new EngineId("qwen3-vl-local");
        EngineCertificationRecord q8 = new EngineCertificationRecord(
                engine, "0.32.5", "q8", "hardware", "visual-smoke",
                Instant.parse("2026-07-28T20:00:00Z"), true,
                Map.of("device", "gpu", "ram", "available"));
        EngineCertificationRecord q4 = new EngineCertificationRecord(
                engine, "0.32.5", "q4", "hardware", "visual-smoke",
                Instant.parse("2026-07-28T20:01:00Z"), true, Map.of());

        store.save(q8);
        store.save(q4);

        assertEquals(q8, store.find(engine, "q8").orElseThrow());
        assertEquals(q4, store.find(engine, "q4").orElseThrow());
        try (var paths = Files.walk(temporary)) {
            assertTrue(paths.noneMatch(path -> path.toString().contains("project")));
        }

        store.invalidate(engine, "q8");

        assertTrue(store.find(engine, "q8").isEmpty());
        assertEquals(q4, store.find(engine, "q4").orElseThrow());
    }

    @Test
    void ignoresCorruptCertificationInsteadOfDeclaringReady() throws Exception {
        FileEngineCertificationStore store =
                new FileEngineCertificationStore(temporary);
        Files.writeString(temporary.resolve("engine--model.properties"),
                "certifiedAt=not-an-instant\nvisualInputVerified=true\n");

        assertTrue(store.find(new EngineId("engine"), "model").isEmpty());
    }
}
