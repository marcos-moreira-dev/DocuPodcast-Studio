package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreDrawingVaultPersistenceTest {
    @TempDir Path temporary;

    @Test
    void vaultRoundTripsNamesAndVectorStateAndRemovesOnlyDiscardedEntries() throws Exception {
        Path projectFile = temporary.resolve("obra.docupodcast.json");
        UpsertTheatreStoryboardFrameVariantUseCase useCase = new UpsertTheatreStoryboardFrameVariantUseCase();
        TheatreDrawingVaultItem hero = new TheatreDrawingVaultItem("drawing-hero", "Héroe", "{\"inkStrokes\":[]}");
        TheatreDrawingVaultItem villain = new TheatreDrawingVaultItem("drawing-villain", "Villano enojado", "{\"inkStrokes\":[1]}");

        useCase.saveDrawingVault(projectFile, List.of(hero, villain));
        assertEquals(List.of(hero, villain), useCase.loadDrawingVault(projectFile));

        Path unrelated = temporary.resolve("storyboard/drawing-vault/LEEME.txt");
        Files.writeString(unrelated, "conservar");
        useCase.saveDrawingVault(projectFile, List.of(villain));

        assertEquals(List.of(villain), useCase.loadDrawingVault(projectFile));
        assertTrue(Files.isRegularFile(unrelated), "vault synchronization must preserve unrelated files");
    }
}
