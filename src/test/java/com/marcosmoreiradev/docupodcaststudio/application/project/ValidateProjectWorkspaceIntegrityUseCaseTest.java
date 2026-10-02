package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValidateProjectWorkspaceIntegrityUseCaseTest {
    private final ValidateProjectWorkspaceIntegrityUseCase useCase = new ValidateProjectWorkspaceIntegrityUseCase();

    @TempDir
    Path tempDir;

    @Test
    void emptyProjectDoesNotRequireMaterializedArtifacts() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");

        ProjectValidationResult result = useCase.validate(
                DocuPodcastProject.createNew("Vacío"),
                projectFile,
                ProjectWorkspaceHydration.empty()
        );

        assertTrue(result.valid());
    }

    @Test
    void reportsMissingPhysicalAssetFile() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        DocuPodcastProject project = DocuPodcastProject.createNew("Notas")
                .withAsset(asset("DOC-001", ProjectAssetKind.IMPORTED_DOCUMENT, "document/document.json"));

        ProjectValidationResult result = useCase.validate(project, projectFile, ProjectWorkspaceHydration.empty());

        assertFalse(result.valid());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("Falta el archivo del asset DOC-001")));
    }

    @Test
    void missingRecoverableDeliveryAssetsDegradeButDoNotInvalidateProject() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        DocuPodcastProject project = DocuPodcastProject.createNew("Degradado")
                .withAsset(asset("FINAL", ProjectAssetKind.AUDIO_FINAL,
                        "jobs/JOB/final/podcast.wav"))
                .withAsset(asset("MANIFEST", ProjectAssetKind.AUDIO_MANIFEST,
                        "jobs/JOB/audio-manifest.json"))
                .withAsset(asset("MP4", ProjectAssetKind.EXPORT,
                        "exports/final.mp4"))
                .withAsset(asset("THUMB", ProjectAssetKind.THUMBNAIL,
                        "document/thumb.png"))
                .withAsset(asset("CROP", ProjectAssetKind.STUDY_SOURCE_CROP,
                        "document/crop.png"));

        assertTrue(useCase.validate(project, projectFile,
                ProjectWorkspaceHydration.empty()).valid());
        ProjectDerivedAssetReport report = new InspectMissingDerivedAssetsUseCase()
                .inspect(project, projectFile);
        assertTrue(report.degraded());
        assertEquals(5, report.warnings().size());
    }

    @Test
    void missingCanonicalSourceStillRejectsOpen() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        DocuPodcastProject project = DocuPodcastProject.createNew("Fuente")
                .withAsset(asset("SOURCE", ProjectAssetKind.SOURCE_DOCUMENT,
                        "source/document.pdf"));
        ProjectValidationResult result = useCase.validate(project, projectFile,
                ProjectWorkspaceHydration.empty());
        assertFalse(result.valid());
        assertTrue(result.messages().stream().anyMatch(value -> value.contains("SOURCE")));
    }

    @Test
    void narrationScriptProjectRequiresRehydratedScript() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("script"));
        Files.writeString(tempDir.resolve("script/narration-script.json"), "{}");
        DocuPodcastProject base = DocuPodcastProject.createNew("Guion");
        DocuPodcastProject project = base
                .withMetadata(base.metadata().withKind(ProjectKind.NARRATION_SCRIPT))
                .withAsset(asset("SCRIPT-001", ProjectAssetKind.NARRATION_SCRIPT, "script/narration-script.json"));

        ProjectValidationResult result = useCase.validate(project, projectFile, ProjectWorkspaceHydration.empty());

        assertFalse(result.valid());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("script/narration-script.json")));
    }

    @Test
    void storyboardBindingsMustReferenceKnownImageAssetsAndScriptSegments() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("script"));
        Files.createDirectories(tempDir.resolve("storyboard"));
        Files.writeString(tempDir.resolve("script/narration-script.json"), "{}");
        Files.writeString(tempDir.resolve("storyboard/storyboard.json"), "{}");
        DocuPodcastProject base = DocuPodcastProject.createNew("Storyboard");
        DocuPodcastProject project = base
                .withMetadata(base.metadata().withKind(ProjectKind.STORYBOARD))
                .withAsset(asset("SCRIPT-001", ProjectAssetKind.NARRATION_SCRIPT, "script/narration-script.json"))
                .withAsset(asset("STORYBOARD-001", ProjectAssetKind.STORYBOARD_MANIFEST, "storyboard/storyboard.json"));
        ProjectWorkspaceHydration hydration = new ProjectWorkspaceHydration(
                Optional.empty(),
                Optional.of(scriptWithSegment("SEG-001")),
                Optional.of(storyboardWithBinding("SEG-MISSING", "IMG-MISSING"))
        );

        ProjectValidationResult result = useCase.validate(project, projectFile, hydration);

        assertFalse(result.valid());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("segmento inexistente")));
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("asset de imagen inexistente")));
    }

    @Test
    void acceptsCoherentScriptAndStoryboardArtifacts() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("script"));
        Files.createDirectories(tempDir.resolve("storyboard"));
        Files.createDirectories(tempDir.resolve("assets/images"));
        Files.writeString(tempDir.resolve("script/narration-script.json"), "{}");
        Files.writeString(tempDir.resolve("storyboard/storyboard.json"), "{}");
        Files.writeString(tempDir.resolve("assets/images/scene.png"), "img");
        DocuPodcastProject base = DocuPodcastProject.createNew("Storyboard");
        DocuPodcastProject project = base
                .withMetadata(base.metadata().withKind(ProjectKind.STORYBOARD))
                .withAsset(asset("SCRIPT-001", ProjectAssetKind.NARRATION_SCRIPT, "script/narration-script.json"))
                .withAsset(asset("STORYBOARD-001", ProjectAssetKind.STORYBOARD_MANIFEST, "storyboard/storyboard.json"))
                .withAsset(asset("IMG-001", ProjectAssetKind.IMAGE, "assets/images/scene.png"));
        ProjectWorkspaceHydration hydration = new ProjectWorkspaceHydration(
                Optional.empty(),
                Optional.of(scriptWithSegment("SEG-001")),
                Optional.of(storyboardWithBinding("SEG-001", "IMG-001"))
        );

        ProjectValidationResult result = useCase.validate(project, projectFile, hydration);

        assertTrue(result.valid());
    }

    private static ProjectAssetReference asset(String id, ProjectAssetKind kind, String relativePath) {
        return new ProjectAssetReference(id, kind, id, relativePath, "application/json", "Prueba", "", "");
    }

    private static NarrationScriptDocument scriptWithSegment(String segmentId) {
        return new NarrationScriptDocument(
                "SCRIPT-001",
                "Guion",
                "es",
                "Documento",
                List.of(NarrationSegment.of(segmentId, NarrationSegmentType.PARAGRAPH, "Intro", "Hola mundo", List.of("BLK-001"))),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
    }

    private static StoryboardDocument storyboardWithBinding(String segmentId, String imageAssetId) {
        return new StoryboardDocument(
                "STORYBOARD-001",
                "Storyboard",
                "SCRIPT-001",
                List.of(new StoryboardBinding("BIND-001", segmentId, imageAssetId, StoryboardDisplayMode.FIT_CONTAIN, "", Map.of())),
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
    }
}
