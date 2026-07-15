package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** MANAGED-DOWNLOAD-RF1: downloads are represented by transversal application contracts. */
final class ManagedDownloadRf1SourceTest {
    @Test
    void managedDownloadContractsExistInApplicationLayer() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/download/ManagedDownloadService.java")));
        String request = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/download/DownloadRequest.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/download/DownloadResumePolicy.java"));

        assertTrue(request.contains("resumeAllowed"));
        assertTrue(policy.contains("Files.size"));
    }
}
