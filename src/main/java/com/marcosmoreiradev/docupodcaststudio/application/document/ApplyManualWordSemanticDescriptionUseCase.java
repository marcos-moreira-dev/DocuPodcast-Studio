package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Persists a manual Word semantic description in the project layer, never in the DOCX. */
public final class ApplyManualWordSemanticDescriptionUseCase {
    public ReadableDocument apply(ReadableDocument document, String blockId,
                                  String description, Instant now) {
        Objects.requireNonNull(document, "document");
        String safeDescription = description == null ? "" : description.strip();
        if (safeDescription.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía.");
        }
        DocumentBlock block = document.blockById(blockId)
                .filter(ApplyManualWordSemanticDescriptionUseCase::semanticComponent)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La selección no es un componente semántico editable."));
        LinkedHashMap<String, String> metadata =
                new LinkedHashMap<>(block.metadata());
        metadata.put("description", safeDescription);
        metadata.put("descriptionState", "APPROVED");
        metadata.put("descriptionSource", "manual-user");
        metadata.put("descriptionEngineVersion", "1");
        metadata.put("descriptionSourceFingerprint", visualFingerprint(block));
        metadata.put("descriptionGeneratedAt",
                Objects.requireNonNullElse(now, Instant.EPOCH).toString());
        metadata.put("manual", "true");
        metadata.put("reusableFor", "narration,document-video");
        return document.replaceBlock(DocumentBlock.of(block.id(), block.type(),
                block.text(), block.originalStyle(), metadata));
    }

    private static boolean semanticComponent(DocumentBlock block) {
        return block.type() == DocumentBlockType.IMAGE_NOTICE
                || block.type() == DocumentBlockType.TABLE_NOTICE;
    }

    static String visualFingerprint(DocumentBlock block) {
        String evidence = block.metadata().getOrDefault(
                "embeddedImageBase64", block.text());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(evidence.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
