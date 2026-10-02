package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VisualBlocksNotNarratedTi4Test {
    @Test
    void secondaryWordComponentsStayExcludedByDefault() {
        ReadableDocument document = new ReadableDocument("Visuales",
                SourceDocumentFormat.DOCX, Path.of("visuales.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH,
                        "Texto que si se habla.", ""),
                DocumentBlock.of("B002", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen detectada", "", Map.of("visualBlock", "true")),
                DocumentBlock.of("B003", DocumentBlockType.TABLE_NOTICE,
                        "Tabla detectada", "", Map.of("visualBlock", "true")),
                DocumentBlock.of("B004", DocumentBlockType.MATH_NOTICE,
                        "Formula detectada", "", Map.of("visualBlock", "true",
                                "sourceMathText", "x al cuadrado mas uno"))));

        var script = new BuildNarrationScriptUseCase().build(document, "es");

        assertEquals(1, script.segmentCount());
        assertEquals(List.of("B001"), script.segments().getFirst().sourceBlockIds());
    }

    @Test
    void wordMathFollowsTheSameInclusionBoundaryAsTables() {
        ReadableDocument document = new ReadableDocument("Visuales",
                SourceDocumentFormat.DOCX, Path.of("visuales.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.MATH_NOTICE,
                        "Formula detectada", "", Map.of(
                                "sourceMathText", "x al cuadrado mas uno"))));

        ReadingProfile included = new ReadingProfile("word-components", "Word",
                "", ReadingProfile.academicDefaults().headingRules(),
                ReadingProfile.academicDefaults().imagePolicy(),
                com.marcosmoreiradev.docupodcaststudio.domain.reading
                        .TableNarrationPolicy.SUMMARIZE);

        var script = new BuildNarrationScriptUseCase().build(
                document, "es", false, included);

        assertEquals(1, script.segmentCount());
        assertEquals("x al cuadrado mas uno",
                script.segments().getFirst().narrationText());
    }

    @Test
    void onlyValidatedQ8DraftImageDescriptionCanEnterAutomaticWordReading() {
        Map<String, String> valid = Map.of(
                "description", "Diagrama del proceso experimental y sus tres etapas.",
                "descriptionState", "DRAFT",
                "descriptionSource", DocumentListeningPreferences.QUALITY_MODEL,
                "ttsSafetyValidated", "true",
                "automaticAdmission", "DRAFT_POLICY_VALIDATED");
        Map<String, String> foreign = Map.of(
                "description", "Descripcion de otro motor.",
                "descriptionState", "DRAFT",
                "descriptionSource", "otro-modelo",
                "ttsSafetyValidated", "true",
                "automaticAdmission", "DRAFT_POLICY_VALIDATED");
        ReadableDocument document = new ReadableDocument("Visuales",
                SourceDocumentFormat.DOCX, Path.of("visuales.docx"), List.of(
                DocumentBlock.of("IMG-OK", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen", "", valid),
                DocumentBlock.of("IMG-NO", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen", "", foreign)));

        var script = new BuildNarrationScriptUseCase().build(
                document, "es", false, ReadingProfile.academicDefaults());

        assertEquals(1, script.segmentCount());
        assertEquals(List.of("IMG-OK"), script.segments().getFirst().sourceBlockIds());
    }
}
