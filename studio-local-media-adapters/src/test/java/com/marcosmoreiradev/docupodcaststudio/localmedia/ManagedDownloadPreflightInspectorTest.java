package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagedDownloadPreflightInspectorTest {
    @TempDir Path temporary;

    @Test
    void distinguishesMissingValidInvalidAndPartialWithoutNetwork() throws Exception {
        Path target = temporary.resolve("models/resource.bin");
        Path partial = temporary.resolve("staging/resource.bin.partial");
        byte[] official = "official-resource".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path checksumSource = Files.write(temporary.resolve("checksum.bin"), official);
        String sha = ManagedDownloadPreflightInspector.sha256(checksumSource);

        assertEquals(ManagedDownloadState.MISSING, inspect(target, partial, official.length, sha).state());

        Files.createDirectories(partial.getParent());
        Files.writeString(partial, "partial");
        assertEquals(ManagedDownloadState.PARTIAL, inspect(target, partial, official.length, sha).state());

        Files.createDirectories(target.getParent());
        Files.writeString(target, "corrupt");
        assertEquals(ManagedDownloadState.INVALID, inspect(target, partial, official.length, sha).state());

        Files.write(target, official);
        var valid = inspect(target, partial, official.length, sha);
        assertEquals(ManagedDownloadState.VALID, valid.state());
        assertEquals(official.length, valid.actualBytes());
        assertEquals(sha, valid.actualSha256());
    }

    private static com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadPreflight inspect(
            Path target, Path partial, long bytes, String sha) throws Exception {
        return ManagedDownloadPreflightInspector.inspect(
                "resource", target, partial, bytes, sha, "license", "https://example.invalid/resource");
    }
}
