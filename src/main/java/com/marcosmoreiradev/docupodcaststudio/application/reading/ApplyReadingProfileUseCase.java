package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/** Applies a reading profile to imported document blocks without touching manual user overrides. */
public final class ApplyReadingProfileUseCase {
    public ReadableDocument apply(ReadableDocument document, ReadingProfile profile) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(profile, "profile");
        ArrayList<DocumentBlock> next = new ArrayList<>(document.blocks().size());
        for (DocumentBlock block : document.blocks()) {
            if (block.manuallyOverridden()) {
                next.add(block);
                continue;
            }
            next.add(classify(block, profile));
        }
        return document.withBlocks(next);
    }

    private static DocumentBlock classify(DocumentBlock block, ReadingProfile profile) {
        if (block.type() == DocumentBlockType.IMAGE_NOTICE) {
            return block.withProfileType(DocumentBlockType.IMAGE_NOTICE, profile.name());
        }
        if (block.type() == DocumentBlockType.TABLE_NOTICE) {
            return block.withProfileType(DocumentBlockType.TABLE_NOTICE, profile.name());
        }
        if (block.type() == DocumentBlockType.MATH_NOTICE) {
            return block.withProfileType(DocumentBlockType.MATH_NOTICE, profile.name());
        }
        if (block.type() == DocumentBlockType.EMPTY || block.text().isBlank()) {
            return block.withProfileType(DocumentBlockType.EMPTY, profile.name());
        }
        if (isList(block)) {
            return block.withProfileType(DocumentBlockType.LIST_ITEM, profile.name());
        }
        String styleText = (block.originalStyle() + " "
                + block.metadataValue("styleId").orElse("") + " "
                + block.metadataValue("styleName").orElse("")).toLowerCase(Locale.ROOT);
        if (profile.headingRules().matchesHeadingStyle(styleText)) {
            return block.withProfileType(DocumentBlockType.HEADING, profile.name());
        }
        if (profile.headingRules().matchesSubheadingStyle(styleText)) {
            return block.withProfileType(DocumentBlockType.SUBHEADING, profile.name());
        }
        if (profile.headingRules().matchesTitleStyle(styleText)) {
            return block.withProfileType(DocumentBlockType.TITLE, profile.name());
        }
        if (profile.headingRules().treatShortBoldParagraphAsSubheading()
                && block.metadataValue("bold").map(Boolean::parseBoolean).orElse(false)
                && block.wordCount() <= profile.headingRules().maxShortBoldWords()) {
            return block.withProfileType(DocumentBlockType.SUBHEADING, profile.name());
        }
        return block.withProfileType(DocumentBlockType.PARAGRAPH, profile.name());
    }

    private static boolean isList(DocumentBlock block) {
        return block.type() == DocumentBlockType.LIST_ITEM
                || block.metadataValue("list").map(Boolean::parseBoolean).orElse(false);
    }
}
