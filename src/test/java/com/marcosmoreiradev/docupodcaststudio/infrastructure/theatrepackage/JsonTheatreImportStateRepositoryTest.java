package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreImportState;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTheatreImportStateRepositoryTest {
    @TempDir Path temp;

    @Test
    void roundTripsGeneratedStateAtomically() throws Exception {
        var entry = new TheatrePackageEntry("backdrop:plaza", TheatrePackageAssetKind.BACKDROP,
                "assets/fondos/plaza.png", "a".repeat(64), 12, Map.of("backdropId", "plaza"));
        var inventory = TheatrePackageInventory.create(1, "obra", "2", "D:/obra", List.of(entry));
        var state = TheatreImportState.fromInventory(inventory, Instant.parse("2026-09-20T10:00:00Z"), null);
        var repository = new JsonTheatreImportStateRepository();

        repository.save(state, temp);
        var reopened = repository.open(temp).orElseThrow();

        assertEquals(state, reopened);
        assertTrue(Files.isRegularFile(temp.resolve(JsonTheatreImportStateRepository.FILE_NAME)));
        try (var files = Files.list(temp)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void rejectsCorruptedGeneratedStateWithAnIOException() throws Exception {
        Files.writeString(temp.resolve(JsonTheatreImportStateRepository.FILE_NAME), "{ broken");

        var error = assertThrows(java.io.IOException.class,
                () -> new JsonTheatreImportStateRepository().open(temp));

        assertTrue(error.getMessage().contains("JSON inválido"));
    }
}
