package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationArtifactStaging;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Builds a derived spoken-language projection while preserving source identity. */
public final class AdaptNarrationLanguageUseCase {
    public static final String POLICY_VERSION = "spoken-translation-v1";
    public static final String MODEL_PROFILE = "qwen3-vl-4b-q8/local-translation-v1";
    private final MediaCapabilityService media;
    private final NarrationTranslationCache persistentCache;
    private final NarrationLanguageDetector detector = new NarrationLanguageDetector();
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AdaptNarrationLanguageUseCase(MediaCapabilityService media) {
        this(media, NarrationTranslationCache.NONE);
    }

    public AdaptNarrationLanguageUseCase(MediaCapabilityService media,
                                         NarrationTranslationCache persistentCache) {
        this.media = Objects.requireNonNull(media, "media");
        this.persistentCache = Objects.requireNonNullElse(
                persistentCache, NarrationTranslationCache.NONE);
    }

    public Result execute(NarrationScriptDocument source,
                          DocumentTranslationPreferences preferences,
                          java.nio.file.Path projectRoot,
                          CancellationToken cancellation,
                          ProgressSink progress)
            throws IOException, InterruptedException {
        return execute(source, preferences, projectRoot, cancellation, progress, "");
    }

    public Result execute(NarrationScriptDocument source,
                          DocumentTranslationPreferences preferences,
                          java.nio.file.Path projectRoot,
                          CancellationToken cancellation,
                          ProgressSink progress,
                          String correlationId)
            throws IOException, InterruptedException {
        Objects.requireNonNull(source, "source script");
        DocumentTranslationPreferences safe = Objects.requireNonNullElseGet(
                preferences, DocumentTranslationPreferences::defaults);
        if (!safe.enabled()) return new Result(sourceLanguageProjection(source),
                0, 0, source.segments().size());
        CancellationToken token = Objects.requireNonNullElse(cancellation, CancellationToken.NONE);
        ProgressSink sink = Objects.requireNonNullElse(progress, ProgressSink.NONE);
        String target = safe.listeningLanguage().tag();
        ArrayList<NarrationSegment> adapted = new ArrayList<>(source.segments().size());
        int[] calls = {0};
        int[] hits = {0};
        int[] passthrough = {0};
        ArrayList<NarrationTranslationException> failures = new ArrayList<>();
        ExecutionContext context = new ExecutionContext(
                correlationId == null || correlationId.isBlank()
                        ? "translate-narration-" + System.nanoTime()
                        : correlationId.strip(), token, sink,
                ExecutionPolicy.defaults(), ResourceLease.NONE,
                GenerationArtifactStaging.NONE);
        media.withContentAnalysisBatch(context,
                ContentAnalysisOperation.NARRATION_TRANSLATION,
                Map.of("contextWindowTokens", "4096", "maxOutputTokens", "768"), () -> {
                    int total = Math.max(1, source.segments().size());
                    for (int index = 0; index < source.segments().size(); index++) {
                        token.throwIfCancellationRequested();
                        NarrationSegment segment = source.segments().get(index);
                        String sourceLanguage = detector.detect(segment.narrationText(),
                                segment.metadata().getOrDefault("sourceLanguage", source.language()));
                        if (!segment.narratable() || sourceLanguage.equals(target)) {
                            passthrough[0]++;
                            adapted.add(withLanguageMetadata(segment, segment.narrationText(),
                                    sourceLanguage, target, "", false));
                        } else {
                            String fingerprint = fingerprint(segment.narrationText(),
                                    sourceLanguage, target);
                            String translated = cached(projectRoot, fingerprint, target);
                            boolean cacheHit = translated != null;
                            if (cacheHit) {
                                hits[0]++;
                            } else {
                                try {
                                    translated = translate(segment.narrationText(), sourceLanguage,
                                            target, context);
                                    if (!validTranslation(translated, target)) {
                                        throw new EngineExecutionException(
                                                EngineDiagnosticCode.INVALID_OUTPUT,
                                                "La salida no está escrita en el idioma de destino "
                                                        + target + ".",
                                                Map.of("segmentId", segment.id(),
                                                        "targetLanguage", target,
                                                        "rawOutput", translated));
                                    }
                                } catch (IOException failure) {
                                    NarrationTranslationException segmentFailure =
                                            new NarrationTranslationException(segment.id(),
                                                    fingerprint, failure);
                                    persistFailure(projectRoot, fingerprint, target,
                                            segmentFailure);
                                    if (!EngineDiagnosticCode.INVALID_OUTPUT.name()
                                            .equals(segmentFailure.reason())) {
                                        throw segmentFailure;
                                    }
                                    failures.add(segmentFailure);
                                    sink.report("NARRATION_TRANSLATION",
                                            (index + 1.0) / total,
                                            "Traducción inválida para " + segment.id()
                                                    + "; se conservarán los demás resultados.");
                                    continue;
                                }
                                try {
                                    persist(projectRoot, fingerprint, translated);
                                } catch (IOException persistenceFailure) {
                                    throw new NarrationTranslationException(segment.id(),
                                            fingerprint, persistenceFailure);
                                }
                                cache.put(fingerprint, translated);
                                clearFailure(projectRoot, fingerprint);
                                calls[0]++;
                            }
                            adapted.add(withLanguageMetadata(segment, translated,
                                    sourceLanguage, target, fingerprint, cacheHit));
                        }
                        sink.report("NARRATION_TRANSLATION", (index + 1.0) / total,
                                "Adaptando idioma de escucha " + (index + 1) + "/" + total + ".");
                    }
                    return null;
                });
        if (!failures.isEmpty()) {
            NarrationTranslationException first = failures.getFirst();
            failures.stream().skip(1).forEach(first::addSuppressed);
            throw first;
        }
        NarrationScriptDocument effective = new NarrationScriptDocument(
                source.id(), source.title(), target, source.sourceDocumentTitle(),
                adapted, source.createdAt(), Instant.now(), source.notes());
        return new Result(effective, calls[0], hits[0], passthrough[0]);
    }

    public java.util.Optional<NarrationScriptDocument> fromCache(
            NarrationScriptDocument source, DocumentTranslationPreferences preferences,
            java.nio.file.Path projectRoot) {
        return inspectCache(source, preferences, projectRoot).effectiveScript();
    }

    /**
     * Read-only translation readiness inspection. It never invokes an engine, writes the
     * persistent cache or changes the process-local cache.
     */
    public TranslationCacheInspection inspectCache(
            NarrationScriptDocument source, DocumentTranslationPreferences preferences,
            java.nio.file.Path projectRoot) {
        Objects.requireNonNull(source, "source script");
        DocumentTranslationPreferences safe = Objects.requireNonNullElseGet(
                preferences, DocumentTranslationPreferences::defaults);
        if (!safe.enabled()) {
            return new TranslationCacheInspection(
                    java.util.Optional.of(sourceLanguageProjection(source)),
                    List.of(), List.of(), 0, source.segments().size());
        }
        String target = safe.listeningLanguage().tag();
        List<NarrationSegment> adapted = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        int cacheHits = 0;
        int passthrough = 0;
        for (NarrationSegment segment : source.segments()) {
            String sourceLanguage = detector.detect(segment.narrationText(),
                    segment.metadata().getOrDefault("sourceLanguage", source.language()));
            if (!segment.narratable() || sourceLanguage.equals(target)) {
                adapted.add(withLanguageMetadata(segment, segment.narrationText(),
                        sourceLanguage, target, "", false));
                passthrough++;
                continue;
            }
            String fingerprint = fingerprint(segment.narrationText(), sourceLanguage, target);
            CacheProbe probe = inspectCached(projectRoot, fingerprint, target);
            if (!probe.valid()) {
                if (probe.present()) invalid.add(segment.id()); else missing.add(segment.id());
                continue;
            }
            cacheHits++;
            adapted.add(withLanguageMetadata(segment, probe.value(), sourceLanguage,
                    target, fingerprint, true));
        }
        java.util.Optional<NarrationScriptDocument> effective = missing.isEmpty()
                && invalid.isEmpty()
                ? java.util.Optional.of(new NarrationScriptDocument(source.id(), source.title(),
                target, source.sourceDocumentTitle(), adapted, source.createdAt(),
                Instant.now(), source.notes()))
                : java.util.Optional.empty();
        return new TranslationCacheInspection(effective, missing, invalid,
                cacheHits, passthrough);
    }

    private CacheProbe inspectCached(java.nio.file.Path projectRoot, String fingerprint,
                                     String targetLanguage) {
        String inMemory = cache.get(fingerprint);
        if (inMemory != null && validTranslation(inMemory, targetLanguage)) {
            return new CacheProbe(true, true, inMemory);
        }
        if (projectRoot == null) return new CacheProbe(inMemory != null, false, "");
        try {
            String persisted = persistentCache.find(projectRoot, fingerprint).orElse(null);
            if (persisted == null) {
                boolean failed = persistentCache.findFailure(projectRoot, fingerprint)
                        .isPresent();
                return new CacheProbe(inMemory != null || failed, false, "");
            }
            return new CacheProbe(true, validTranslation(persisted, targetLanguage), persisted);
        } catch (IOException ignored) {
            return new CacheProbe(false, false, "");
        }
    }

    public Result execute(NarrationScriptDocument source,
                          DocumentTranslationPreferences preferences,
                          CancellationToken cancellation,
                          ProgressSink progress) throws IOException, InterruptedException {
        return execute(source, preferences, null, cancellation, progress);
    }

    public java.util.Optional<NarrationScriptDocument> fromCache(
            NarrationScriptDocument source, DocumentTranslationPreferences preferences) {
        return fromCache(source, preferences, null);
    }

    private String cached(java.nio.file.Path projectRoot, String fingerprint,
                          String targetLanguage) {
        String inMemory = cache.get(fingerprint);
        if (inMemory != null && validTranslation(inMemory, targetLanguage)) return inMemory;
        if (inMemory != null) cache.remove(fingerprint, inMemory);
        if (projectRoot == null) return null;
        try {
            String persisted = persistentCache.find(projectRoot, fingerprint).orElse(null);
            if (persisted != null && validTranslation(persisted, targetLanguage)) {
                cache.put(fingerprint, persisted);
                return persisted;
            }
            String recovered = persistentCache.findFailure(projectRoot, fingerprint)
                    .map(NarrationTranslationCache.Failure::rawOutput)
                    .filter(value -> validTranslation(value, targetLanguage))
                    .orElse(null);
            if (recovered != null) {
                persistentCache.store(projectRoot, fingerprint, recovered);
                persistentCache.clearFailure(projectRoot, fingerprint);
                cache.put(fingerprint, recovered);
                return recovered;
            }
            return null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean validTranslation(String translated, String targetLanguage) {
        if (translated == null || translated.isBlank()) return false;
        String target = targetLanguage == null ? "" : targetLanguage.strip().toLowerCase();
        if (!(target.startsWith("en") || target.startsWith("es"))) return true;
        return detector.confidentlyMatches(translated,
                target.startsWith("en") ? "en" : "es")
                || detector.languageNeutralMathematicalNotation(translated);
    }

    private void persist(java.nio.file.Path projectRoot, String fingerprint,
                         String translated) throws IOException {
        if (projectRoot == null) return;
        persistentCache.store(projectRoot, fingerprint, translated);
    }

    private void persistFailure(java.nio.file.Path projectRoot, String fingerprint,
                                String targetLanguage,
                                NarrationTranslationException failure) {
        if (projectRoot == null) return;
        try {
            String rawOutput = failure.diagnostics().getOrDefault("rawOutput", "");
            persistentCache.storeFailure(projectRoot, fingerprint,
                    new NarrationTranslationCache.Failure(failure.segmentId(),
                            targetLanguage, failure.reason(), rawOutput, Instant.now(),
                            failure.diagnostics()));
        } catch (IOException ignored) {
            // Diagnostics are best effort; valid translations remain independently atomic.
        }
    }

    private void clearFailure(java.nio.file.Path projectRoot, String fingerprint) {
        if (projectRoot == null) return;
        try {
            persistentCache.clearFailure(projectRoot, fingerprint);
        } catch (IOException ignored) {
            // A stale diagnostic never overrides a valid cache entry.
        }
    }

    private String translate(String text, String sourceLanguage, String targetLanguage,
                             ExecutionContext context)
            throws IOException, InterruptedException {
        return media.translateNarration(text, sourceLanguage, targetLanguage,
                context);
    }

    private NarrationScriptDocument sourceLanguageProjection(
            NarrationScriptDocument source) {
        ArrayList<NarrationSegment> projected = new ArrayList<>(source.segments().size());
        String documentLanguage = "";
        for (NarrationSegment segment : source.segments()) {
            String language = detector.detect(segment.narrationText(),
                    segment.metadata().getOrDefault("sourceLanguage", source.language()));
            if (documentLanguage.isBlank() && segment.narratable()) documentLanguage = language;
            projected.add(withLanguageMetadata(segment, segment.narrationText(),
                    language, language, "", false));
        }
        if (documentLanguage.isBlank()) documentLanguage = source.language();
        return new NarrationScriptDocument(source.id(), source.title(), documentLanguage,
                source.sourceDocumentTitle(), projected, source.createdAt(),
                Instant.now(), source.notes());
    }

    private static NarrationSegment withLanguageMetadata(
            NarrationSegment source, String effectiveText, String sourceLanguage,
            String targetLanguage, String fingerprint, boolean cacheHit) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(source.metadata());
        metadata.put("sourceLanguage", sourceLanguage);
        metadata.put("effectiveLanguage", targetLanguage);
        metadata.put("narrationLanguageAdapted", Boolean.toString(
                !sourceLanguage.equals(targetLanguage)));
        if (!fingerprint.isBlank()) metadata.put("translationFingerprint", fingerprint);
        metadata.put("translationCacheHit", Boolean.toString(cacheHit));
        return new NarrationSegment(source.id(), source.type(), source.title(),
                effectiveText, source.sourceBlockIds(), source.characterId(),
                source.voiceProfileId(), source.performanceStyleId(), metadata);
    }

    public static String fingerprint(String sourceText, String sourceLanguage,
                                     String targetLanguage) {
        String material = hash(sourceText) + "\n" + sourceLanguage + "\n"
                + targetLanguage + "\n" + MODEL_PROFILE + "\n" + POLICY_VERSION;
        return hash(material);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Result(NarrationScriptDocument script, int translationCalls,
                         int cacheHits, int passthroughSegments) { }

    public record TranslationCacheInspection(
            java.util.Optional<NarrationScriptDocument> effectiveScript,
            List<String> missingSegmentIds,
            List<String> invalidSegmentIds,
            int cacheHits,
            int passthroughSegments) {
        public TranslationCacheInspection {
            effectiveScript = effectiveScript == null
                    ? java.util.Optional.empty() : effectiveScript;
            missingSegmentIds = missingSegmentIds == null
                    ? List.of() : List.copyOf(missingSegmentIds);
            invalidSegmentIds = invalidSegmentIds == null
                    ? List.of() : List.copyOf(invalidSegmentIds);
        }

        public boolean complete() {
            return effectiveScript.isPresent()
                    && missingSegmentIds.isEmpty() && invalidSegmentIds.isEmpty();
        }
    }

    private record CacheProbe(boolean present, boolean valid, String value) { }
}
