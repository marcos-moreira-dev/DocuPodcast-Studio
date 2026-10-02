package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ValidateProjectPayloadUseCaseTest {
    private final ValidateProjectPayloadUseCase useCase = new ValidateProjectPayloadUseCase();

    @Test
    void emptyProjectIsValidWithoutPayload() {
        assertTrue(useCase.validate(DocuPodcastProject.createNew("Vacío")).valid());
    }

    @Test
    void documentOnlyRequiresSourceOrImportedDocument() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Notas")
                .withMetadata(DocuPodcastProject.createNew("Notas").metadata().withKind(ProjectKind.DOCUMENT_ONLY));

        assertFalse(useCase.validate(project).valid());
    }

    @Test
    void documentOnlyIsValidWithSourceDocument() {
        DocuPodcastProject base = DocuPodcastProject.createNew("Notas");
        DocuPodcastProject project = base.withMetadata(base.metadata().withKind(ProjectKind.DOCUMENT_ONLY))
                .withAsset(new ProjectAssetReference("SRC-001", ProjectAssetKind.SOURCE_DOCUMENT,
                        "Notas", "source/notas.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Fuente", "", ""));

        assertTrue(useCase.validate(project).valid());
    }

    @Test
    void audioProjectRemainsOpenableAfterGeneratedAudioWasDeleted() {
        DocuPodcastProject base = DocuPodcastProject.createNew("Lectura pendiente de regenerar");
        DocuPodcastProject project = base
                .withMetadata(base.metadata().withKind(ProjectKind.AUDIO_PROJECT))
                .withAsset(new ProjectAssetReference("SCRIPT-001", ProjectAssetKind.NARRATION_SCRIPT,
                        "Lectura", "script/narration-script.json", "application/json", "Guion", "", ""));

        assertTrue(useCase.validate(project).valid());
    }
}
