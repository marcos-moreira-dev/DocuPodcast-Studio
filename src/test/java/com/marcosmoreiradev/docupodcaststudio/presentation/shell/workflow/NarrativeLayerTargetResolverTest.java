package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeLayerTargetResolverTest {
    @Test
    void voiceUsesRealProjectVoiceAndImageRequiresRealAsset() {
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Capas"));
        NarrativeLayerCoordinator coordinator = new NarrativeLayerCoordinator();
        ScriptTextRange range = new ScriptTextRange("SEG-001", 0, 12);
        DocumentTextRange documentRange = new DocumentTextRange("BLK-001", 0, 12);

        NarrativeLayerCoordinator.AssignmentOutcome voice = coordinator.assign(
                session, NarrativeLayerKind.VOICE, range, documentRange, "BLK-001", "Texto");

        assertTrue(voice.assigned());
        assertEquals("VOC-NARRATOR", voice.assignment().targetId());

        NarrativeLayerCoordinator.AssignmentOutcome missingImage = coordinator.assign(
                session, NarrativeLayerKind.IMAGE, range, documentRange, "BLK-001", "Texto");

        assertFalse(missingImage.assigned());
        assertTrue(missingImage.message().contains("imagen real"));

        ProjectAssetReference image = new ProjectAssetReference(
                "IMG-PORTADA",
                ProjectAssetKind.IMAGE,
                "Portada",
                "media/images/portada.png",
                "image/png",
                "Storyboard",
                "",
                "");
        session.replaceProject(session.project().withAsset(image), true);

        NarrativeLayerCoordinator.AssignmentOutcome realImage = coordinator.assign(
                session, NarrativeLayerKind.IMAGE, range, documentRange, "BLK-001", "Texto", "IMG-PORTADA");

        assertTrue(realImage.assigned());
        assertEquals("IMG-PORTADA", realImage.assignment().targetId());
        assertEquals("Portada", realImage.assignment().displayName());
    }
}
