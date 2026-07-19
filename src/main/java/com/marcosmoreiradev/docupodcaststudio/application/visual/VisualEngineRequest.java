package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.nio.file.Path;
import java.util.List;

/** Request sent to a local visual engine to produce one PNG candidate. */
public record VisualEngineRequest(
        String prompt,
        String negativePrompt,
        String checkpointName,
        int steps,
        double cfg,
        int batchSize,
        int targetWidth,
        int targetHeight,
        Path outputDirectory,
        String filenamePrefix,
        List<VisualConditioningReference> conditioningReferences,
        long seed
) {
    public VisualEngineRequest {
        prompt = normalize(prompt);
        negativePrompt = normalize(negativePrompt);
        checkpointName = normalize(checkpointName);
        steps = Math.max(1, Math.min(150, steps));
        cfg = cfg <= 0 ? 7.0 : cfg;
        batchSize = Math.max(1, batchSize);
        targetWidth = roundToEight(targetWidth <= 0 ? 1024 : targetWidth);
        targetHeight = roundToEight(targetHeight <= 0 ? 1024 : targetHeight);
        String normalizedPrefix = safe(normalize(filenamePrefix).isBlank() ? "docupodcast-visual" : filenamePrefix);
        filenamePrefix = normalizedPrefix.isBlank() ? "docupodcast-visual" : normalizedPrefix;
        conditioningReferences = conditioningReferences == null
                ? List.of()
                : conditioningReferences.stream().filter(java.util.Objects::nonNull).toList();
        seed = Math.max(0L, seed);
    }

    public VisualEngineRequest(String prompt,
                               String negativePrompt,
                               String checkpointName,
                               int steps,
                               double cfg,
                               int batchSize,
                               int targetWidth,
                               int targetHeight,
                               Path outputDirectory,
                               String filenamePrefix,
                               List<VisualConditioningReference> conditioningReferences) {
        this(prompt, negativePrompt, checkpointName, steps, cfg, batchSize, targetWidth, targetHeight,
                outputDirectory, filenamePrefix, conditioningReferences, positiveSeed());
    }

    public VisualEngineRequest(String prompt,
                               String negativePrompt,
                               String checkpointName,
                               int steps,
                               double cfg,
                               int batchSize,
                               int targetWidth,
                               int targetHeight,
                               Path outputDirectory,
                               String filenamePrefix) {
        this(prompt, negativePrompt, checkpointName, steps, cfg, batchSize, targetWidth, targetHeight,
                outputDirectory, filenamePrefix, List.of(), positiveSeed());
    }

    public boolean requiresIdentityConditioning() {
        return conditioningReferences.stream()
                .anyMatch(reference -> reference.role() == VisualConditioningRole.IDENTITY);
    }

    public boolean hasCameraGuide() {
        return conditioningReferences.stream()
                .anyMatch(reference -> reference.role() == VisualConditioningRole.CAMERA_GUIDE);
    }

    public boolean hasDrawnGuide() {
        return conditioningReferences.stream()
                .anyMatch(reference -> reference.role() == VisualConditioningRole.DRAWN_GUIDE);
    }

    public boolean hasStructureGuide() {
        return hasCameraGuide() || hasDrawnGuide();
    }

    public VisualEngineRequest withConditioningReferences(List<VisualConditioningReference> references) {
        return new VisualEngineRequest(prompt, negativePrompt, checkpointName, steps, cfg, batchSize,
                targetWidth, targetHeight, outputDirectory, filenamePrefix, references, seed);
    }

    public VisualEngineRequest withPrompt(String prompt) {
        return new VisualEngineRequest(prompt, negativePrompt, checkpointName, steps, cfg, batchSize,
                targetWidth, targetHeight, outputDirectory, filenamePrefix, conditioningReferences, seed);
    }

    public int generationWidth() {
        return generationSize(targetWidth, targetHeight).width();
    }

    public int generationHeight() {
        return generationSize(targetWidth, targetHeight).height();
    }

    private static Size generationSize(int targetWidth, int targetHeight) {
        int maxLongEdge = 768;
        int longest = Math.max(targetWidth, targetHeight);
        if (longest <= maxLongEdge) {
            return new Size(roundToEight(targetWidth), roundToEight(targetHeight));
        }
        double scale = maxLongEdge / (double) longest;
        int width = Math.max(256, roundToEight((int) Math.round(targetWidth * scale)));
        int height = Math.max(256, roundToEight((int) Math.round(targetHeight * scale)));
        return new Size(width, height);
    }

    private static int roundToEight(int value) {
        return Math.max(8, Math.max(1, Math.round(value / 8.0f)) * 8);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String safe(String value) {
        return normalize(value).toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
    }

    private static long positiveSeed() {
        return java.util.concurrent.ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    private record Size(int width, int height) {
    }
}
