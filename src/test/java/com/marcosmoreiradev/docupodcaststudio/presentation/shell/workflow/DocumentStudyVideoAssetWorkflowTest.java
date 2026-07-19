package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ink.InkImageCrop;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkPlacedImage;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceState;
import com.marcosmoreiradev.docupodcaststudio.application.ink.InkWorkspaceStateSerializer;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoAssetWorkflowTest {
    @TempDir
    Path temp;

    @Test
    void savesFlattenedImageEditableSidecarAndSourcesInsideProject() throws Exception {
        Path projectFile = temp.resolve("lesson.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path png = temp.resolve("flattened.png");
        Path source = temp.resolve("source.png");
        Files.write(png, new byte[]{1, 2, 3});
        Files.write(source, new byte[]{4, 5, 6});
        InkWorkspaceState state = InkWorkspaceState.create(1680, 432, "#ffffffff", List.of(),
                List.of(new InkPlacedImage("IMG-A", "", "", "inline", "inline-original",
                        10, 20, 300, 150, 10, 20, 300, InkImageCrop.none())), Map.of());
        ProjectSession session = ProjectSession.opened(DocuPodcastProject.empty("Lesson"), projectFile);

        DocumentStudyVideoAssetWorkflow.DrawingAsset result = new DocumentStudyVideoAssetWorkflow().saveDrawing(
                session, "B0002", png, InkWorkspaceStateSerializer.toJson(state), Map.of("IMG-A", source));

        Path sidecar = temp.resolve(result.stateRelativePath());
        InkWorkspaceState reopened = InkWorkspaceStateSerializer.fromJson(Files.readString(sidecar));
        InkPlacedImage placed = reopened.images().get(0);
        assertTrue(Files.isRegularFile(temp.resolve(result.asset().relativePath())));
        assertTrue(Files.isRegularFile(temp.resolve(placed.sourcePath())));
        assertTrue(placed.sourcePath().startsWith("media/images/document-study/illustrations/sources/"));
        assertFalse(placed.sourceAssetId().isBlank());
        assertTrue(session.project().assets().byId(placed.sourceAssetId()).isPresent());
        assertTrue(session.project().assets().byId(result.asset().id()).isPresent());
        assertEquals("inline", placed.inlineImageData());
    }

    @Test
    void missingSourceDoesNotRegisterDrawingOrLeaveStagingDirectory() throws Exception {
        Path projectFile = temp.resolve("lesson.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Path png = temp.resolve("flattened.png");
        Files.write(png, new byte[]{1});
        ProjectSession session = ProjectSession.opened(DocuPodcastProject.empty("Lesson"), projectFile);

        try {
            new DocumentStudyVideoAssetWorkflow().saveDrawing(session, "B0003", png, "{}",
                    Map.of("IMG-MISSING", temp.resolve("missing.png")));
        } catch (java.io.IOException expected) {
            // Expected preflight failure.
        }

        assertEquals(0, session.project().assets().references().size());
        Path working = temp.resolve("media/images/document-study");
        if (Files.isDirectory(working)) {
            try (var children = Files.list(working)) {
                assertTrue(children.noneMatch(path -> path.getFileName().toString().startsWith(".illustration-")));
            }
        }
    }
}
