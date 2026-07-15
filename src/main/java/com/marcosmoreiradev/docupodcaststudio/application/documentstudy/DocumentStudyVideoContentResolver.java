package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Resolves the DOCX-order visual sequence used by documentary video. */
public final class DocumentStudyVideoContentResolver {
    public enum Kind { COVER, PARAGRAPH, TABLE, CLOSING }

    public record Item(Kind kind, DocumentBlock block,
                       DocumentParagraphVisualAssignment paragraphVisual,
                       double durationSeconds,
                       boolean enabled) { }

    public List<Item> resolve(ReadableDocument document, DocumentStudyVideoConfiguration configuration) {
        if (document == null) return List.of();
        DocumentStudyVideoConfiguration safe = configuration == null
                ? DocumentStudyVideoConfiguration.empty() : configuration;
        ArrayList<Item> result = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            if (block == null) continue;
            if (block.type() == DocumentBlockType.PARAGRAPH) {
                result.add(new Item(Kind.PARAGRAPH, block,
                        safe.paragraph(block.id()).orElse(DocumentParagraphVisualAssignment.empty(block.id())), 0.0,
                        safe.blockEnabled(block.id())));
            } else if (block.type() == DocumentBlockType.TABLE_NOTICE) {
                result.add(new Item(Kind.TABLE, block, null, safe.tableDuration(block.id()),
                        safe.blockEnabled(block.id())));
            } else if (block.type() == DocumentBlockType.TITLE
                    || block.type() == DocumentBlockType.HEADING
                    || block.type() == DocumentBlockType.SUBHEADING) {
                result.add(new Item(Kind.COVER, block, null, 0.0, safe.blockEnabled(block.id())));
            }
        }
        for (DocumentStudyClosingSlide slide : safe.closingSlides()) {
            DocumentBlock block = DocumentBlock.of(
                    slide.id(),
                    DocumentBlockType.PARAGRAPH,
                    slide.title(),
                    "documentary-closing-slide",
                    Map.of("documentaryClosingSlide", "true"));
            DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                    slide.id(), "", slide.imageAssetId(), "", "",
                    slide.imageAssetId().isBlank() ? DocumentVisualSource.NONE : DocumentVisualSource.IMPORTED,
                    "", com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition.BOTTOM_RIGHT);
            result.add(new Item(Kind.CLOSING, block, visual, slide.durationSeconds(),
                    safe.blockEnabled(slide.id())));
        }
        return List.copyOf(result);
    }
}
