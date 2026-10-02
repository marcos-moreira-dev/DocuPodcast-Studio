package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTranslationVisualAuthorityTest {
    @Test
    void pdfRenderingAndGroundingRemainBoundToSourceText() throws Exception {
        String videoPlan = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/application/documentstudy/BuildDocumentStudyVideoPlanUseCase.java"));
        String adaptation = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/application/reading/AdaptNarrationLanguageUseCase.java"));

        assertTrue(videoPlan.contains("fragment.sourceText()"),
                "PDF TEXT_RENDER must retain source fragment text");
        assertTrue(videoPlan.contains("expectedSourceText(item, pdf, primary)"),
                "PDF visual validation must remain source-grounded");
        assertTrue(adaptation.contains("source.sourceBlockIds()"),
                "language adaptation must preserve source binding identity");
        assertFalse(adaptation.contains("pdfBbox") || adaptation.contains("cropBBox")
                        || adaptation.contains("highlight"),
                "translation fingerprint must not depend on PDF geometry or styling");
    }

    @Test
    void changingListeningLanguageDoesNotInvalidatePdfSemantics() throws Exception {
        String viewModel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        int start = viewModel.indexOf("public void setDocumentTranslationPreferences(");
        int end = viewModel.indexOf("public ReadOnlyObjectProperty<PlaybackCursor>", start);
        String setter = viewModel.substring(start, end);
        assertFalse(setter.contains("currentScript.set(null)"));
        assertFalse(setter.contains("bumpDocumentMediaRevision"));
        assertFalse(setter.contains("preparePdf"));
    }
}
