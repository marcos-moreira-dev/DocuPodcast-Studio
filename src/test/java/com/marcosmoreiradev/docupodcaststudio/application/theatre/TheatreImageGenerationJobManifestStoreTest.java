package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreImageGenerationJobManifestStoreTest {
    @TempDir
    Path projectRoot;

    @Test
    void persistsAppliedReferencesAndRecoversInterruptedJob() throws Exception {
        TheatreImageGenerationJobManifestStore store = new TheatreImageGenerationJobManifestStore();
        TheatreImageGenerationJob job = new TheatreImageGenerationJob(
                "JOB-1",
                TheatreFrameGenerationScope.scene("SC-1"),
                FrameGenerationMode.SINGLE,
                TheatreImageGenerationPreset.TEST_4GB_SD15,
                projectRoot.resolve("generated"),
                List.of(),
                List.of("personaje: Capitan", "boceto: Frame SEG-1"),
                "Generando",
                true);

        store.save(projectRoot, List.of(job));
        List<TheatreImageGenerationJob> restored = store.load(projectRoot);

        assertEquals(1, restored.size());
        assertEquals(List.of("personaje: Capitan", "boceto: Frame SEG-1"), restored.getFirst().appliedReferences());
        assertFalse(restored.getFirst().running());
        assertTrue(restored.getFirst().status().contains("interrumpido"));
    }
}
