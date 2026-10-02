package com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonTheatrePackageScannerTest {
    @TempDir Path temp;

    @Test
    void combinesManifestAssetsWithConventionBasedAdditions() throws Exception {
        Files.writeString(temp.resolve("docupodcast-theatre.json"), """
                {
                  "schemaVersion": 1,
                  "packageId": "como-sera-la-patria",
                  "packageVersion": "2026.09",
                  "assets": [
                    {"logicalId":"character:narrador:view:frontal","kind":"CHARACTER_IMAGE",
                     "path":"assets/personajes/narrador/frontal.png","characterId":"narrador","view":"frontal"}
                  ]
                }
                """);
        write("assets/personajes/narrador/frontal.png", "narrador");
        write("assets/personajes/concha/frontal.png", "concha");
        write("assets/audio/c08/i_00842.wav", "audio");

        var inventory = new JsonTheatrePackageScanner().scan(temp);

        assertEquals("como-sera-la-patria", inventory.packageId());
        assertEquals(3, inventory.entries().size());
        assertTrue(inventory.entries().stream().anyMatch(entry -> entry.logicalId().equals("character:concha:view:frontal")));
        assertTrue(inventory.entries().stream().anyMatch(entry -> entry.kind() == TheatrePackageAssetKind.HUMAN_AUDIO
                && entry.metadata("interventionId").equals("INTERVENCION-842")));
    }

    @Test
    void rejectsTraversalOutsideThePackage() throws Exception {
        Path outside = temp.getParent().resolve("outside.png");
        Files.writeString(outside, "outside");
        Files.writeString(temp.resolve("docupodcast-theatre.json"), """
                {"schemaVersion":1,"packageId":"obra","assets":[
                  {"logicalId":"backdrop:x","kind":"BACKDROP","path":"../outside.png"}
                ]}
                """);

        var error = assertThrows(java.io.IOException.class, () -> new JsonTheatrePackageScanner().scan(temp));
        assertTrue(error.getMessage().contains("fuera"));
    }

    @Test
    void reportsMalformedManifestAsAControlledInputError() throws Exception {
        Files.writeString(temp.resolve("docupodcast-theatre.json"), "{ not-json");

        var error = assertThrows(java.io.IOException.class,
                () -> new JsonTheatrePackageScanner().scan(temp));

        assertTrue(error.getMessage().contains("JSON inválido"));
    }

    private void write(String relative, String content) throws Exception {
        Path target = temp.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
