package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class DocuPodcastProjectJsonReaderTest {
    @Test
    void rejectsFutureFormatVersion() {
        String json = """
                {
                  "formatVersion": 999,
                  "project": {
                    "id": "PRJ-1",
                    "title": "Futuro",
                    "description": "",
                    "language": "es",
                    "kind": "EMPTY",
                    "status": "DRAFT",
                    "createdAt": "2026-05-29T00:00:00Z",
                    "updatedAt": "2026-05-29T00:00:00Z"
                  },
                  "assets": { "items": [] },
                  "view": {}
                }
                """;

        assertThrows(IOException.class, () -> new DocuPodcastProjectJsonReader().read(json));
    }
}
