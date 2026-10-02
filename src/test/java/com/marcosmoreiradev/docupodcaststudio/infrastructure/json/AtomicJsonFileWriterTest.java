package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AtomicJsonFileWriterTest {
    @TempDir Path temp;

    @Test
    void validatesBeforeReplacingCanonicalFile() throws Exception {
        Path target = temp.resolve("state.json");
        Files.writeString(target, "{\"previous\":true}");
        AtomicJsonFileWriter writer = new AtomicJsonFileWriter();

        assertThrows(IOException.class, () -> writer.write(target, "{broken"));

        assertEquals("{\"previous\":true}", Files.readString(target));
    }
}
