package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.AnalysisVisualInput;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Generates grounded Qwen Q8 descriptions for directly embedded Word images. */
public final class PrepareWordSemanticImagesUseCase {
    public static final String MODEL = DocumentListeningPreferences.QUALITY_MODEL;
    public static final String VERSION = "word-image-grounded-q8-v2";
    private static final System.Logger LOG = System.getLogger(
            PrepareWordSemanticImagesUseCase.class.getName());
    private static final String SCHEMA = """
            {"type":"object","properties":{"status":{"type":"string","enum":["OK","INSUFFICIENT_EVIDENCE"]},
            "objectId":{"type":"string"},"narrationText":{"type":"string"},"claims":{"type":"array","items":{
            "type":"object","properties":{"text":{"type":"string"},"evidenceIds":{"type":"array","items":{"type":"string"}}},
            "required":["text","evidenceIds"]}},"recognizedLabels":{"type":"array","items":{"type":"string"}},
            "uncertainties":{"type":"array","items":{"type":"string"}},"confidence":{"type":"number","minimum":0,"maximum":1}},
            "required":["status","objectId","narrationText","claims","recognizedLabels","uncertainties","confidence"]}
            """;

    private final MediaCapabilityService media;
    private final MaterializeWordDocumentContentAssetUseCase materializer;

    public PrepareWordSemanticImagesUseCase(MediaCapabilityService media,
                                             MaterializeWordDocumentContentAssetUseCase materializer) {
        this.media = Objects.requireNonNull(media, "media");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
    }

    public Result execute(ReadableDocument document, Path projectDirectory,
                          SecondarySemanticReadingPolicy policy, String language)
            throws InterruptedException {
        return execute(document, projectDirectory, policy, language, ignored -> { });
    }

    public Result execute(ReadableDocument document, Path projectDirectory,
                          SecondarySemanticReadingPolicy policy, String language,
                          ProgressListener progressListener)
            throws InterruptedException {
        return execute(document, projectDirectory, policy, language, null, progressListener);
    }

    public Result execute(ReadableDocument document, Path projectDirectory,
                          SecondarySemanticReadingPolicy policy, String language,
                          EngineId selectedEngine, ProgressListener progressListener)
            throws InterruptedException {
        Objects.requireNonNull(document, "document");
        ProgressListener progress = progressListener == null ? ignored -> { }
                : progressListener;
        if (document.format() != SourceDocumentFormat.DOCX
                || policy == null
                || !policy.includes(SecondarySemanticComponentKind.IMAGE)) {
            return new Result(document, 0, 0, List.of(), false);
        }
        ArrayList<DocumentBlock> updated = new ArrayList<>(document.blocks());
        ArrayList<String> failures = new ArrayList<>();
        int generated = 0;
        int reused = 0;
        int completed = 0;
        boolean cancelled = false;
        int total = (int) updated.stream()
                .filter(block -> block.type() == DocumentBlockType.IMAGE_NOTICE)
                .count();
        long startedNanos = System.nanoTime();
        for (int index = 0; index < updated.size(); index++) {
            if (Thread.currentThread().isInterrupted()) {
                cancelled = true;
                break;
            }
            DocumentBlock block = updated.get(index);
            if (block.type() != DocumentBlockType.IMAGE_NOTICE) continue;
            progress.onProgress(Progress.of(completed, total, block.id(),
                    generated, reused, failures.size(), startedNanos));
            try {
            String current = block.metadata().getOrDefault("description", "").strip();
            String currentSource = block.metadata().getOrDefault("descriptionSource", "").strip();
            if (!current.isBlank() && !MODEL.equals(currentSource)) {
                reused++;
                continue;
            }
            DocumentContentItem content = content(block);
            if (!materializer.canMaterialize(content)) {
                failures.add(block.id() + ": imagen incrustada no disponible");
                continue;
            }
            try {
                var asset = materializer.materialize(content, projectDirectory);
                if (!current.isBlank() && asset.visualFingerprint().equals(
                        block.metadata().getOrDefault("descriptionSourceFingerprint", ""))) {
                    reused++;
                    continue;
                }
                String objectId = "WORD-IMAGE-" + block.id();
                String nearby = nearbyContext(updated, index);
                LinkedHashMap<String, String> options = new LinkedHashMap<>();
                options.put("model", MODEL);
                options.put("objectId", objectId);
                options.put("semanticKind", "IMAGE");
                options.put("semanticPolicy", policy.name());
                options.put("maxOutputTokens", "384");
                options.put("contextWindowTokens", "8192");
                AnalysisVisualInput visualInput = new AnalysisVisualInput(asset.path(),
                        "high-resolution-word-image", block.id());
                ContentAnalysisResult analysis = media.analyzeContent(selectedEngine,
                        new ContentAnalysisRequest(ContentAnalysisOperation.IMAGE_DESCRIPTION,
                                List.of(visualInput),
                                "Describe exclusivamente la imagen " + objectId
                                        + " para una lectura universitaria en voz alta. Redacta de dos a cuatro "
                                        + "frases naturales, normalmente entre 45 y 90 palabras. Explica los "
                                        + "elementos y relaciones visuales relevantes y su funcion probable respecto al "
                                        + "contexto cercano. El contexto es solo una pista narrativa: la imagen es la "
                                        + "evidencia y prevalece si existe cualquier contradiccion. Una ilustracion "
                                        + "estilizada es evidencia suficiente para "
                                        + "describir sus personajes, acciones y contraste; no transcribas formulas "
                                        + "decorativas o ilegibles. No resumas el documento ni inventes detalles. "
                                        + "Usa INSUFFICIENT_EVIDENCE solo si el visual no se puede leer o carece de "
                                        + "contenido significativo.",
                                nearby, language == null ? "es" : language, SCHEMA, options),
                        ExecutionContext.defaults("word-image-description-" + block.id()));
                String status = property(analysis.structuredJson(), "status");
                String returnedObject = property(analysis.structuredJson(), "objectId");
                String description = analysis.text().strip();
                if (!"OK".equals(status) || !safeDescription(description)) {
                    LinkedHashMap<String, String> recoveryOptions =
                            new LinkedHashMap<>(options);
                    recoveryOptions.put("recoveryStrategy", "VISUAL_ONLY_NO_CONTEXT");
                    analysis = media.analyzeContent(selectedEngine,
                            new ContentAnalysisRequest(
                                    ContentAnalysisOperation.IMAGE_DESCRIPTION,
                                    List.of(visualInput),
                                    "Describe solamente lo que se ve con seguridad en la imagen "
                                            + objectId + ". Es una ilustracion valida: identifica "
                                            + "personajes, acciones, objetos y contrastes visibles "
                                            + "en dos a cuatro frases naturales. No interpretes su "
                                            + "intencion, no leas formulas decorativas y usa OK si "
                                            + "el contenido visual es claro.",
                                    "", language == null ? "es" : language,
                                    SCHEMA, recoveryOptions),
                            ExecutionContext.defaults("word-image-description-"
                                    + block.id() + "-visual-only"));
                    status = property(analysis.structuredJson(), "status");
                    returnedObject = property(analysis.structuredJson(), "objectId");
                    description = analysis.text().strip();
                }
                LOG.log(System.Logger.Level.INFO,
                        "word-image.semantic-result block={0} status={1} returnedObject={2} words={3} confidence={4}",
                        block.id(), status, returnedObject, words(description), analysis.confidence());
                if (!"OK".equals(status) || !safeDescription(description)) {
                    failures.add(block.id() + ": evidencia o salida insuficiente"
                            + " (status=" + status + ", palabras=" + words(description) + ")");
                    continue;
                }
                LinkedHashMap<String, String> metadata = new LinkedHashMap<>(block.metadata());
                metadata.put("description", trimWords(description, 100));
                metadata.put("descriptionState", "DRAFT");
                metadata.put("descriptionSource", MODEL);
                metadata.put("descriptionEngineVersion", VERSION);
                metadata.put("descriptionSourceFingerprint", asset.visualFingerprint());
                metadata.put("descriptionGeneratedAt", Instant.now().toString());
                metadata.put("semanticPolicy", policy.name());
                metadata.put("ttsSafetyValidated", "true");
                metadata.put("automaticAdmission", "DRAFT_POLICY_VALIDATED");
                // Java owns the canonical block/content identity. The model echo is
                // diagnostic correlation only and must not veto a grounded result
                // from this single-image request.
                metadata.put("descriptionReturnedObjectId", returnedObject);
                updated.set(index, DocumentBlock.of(block.id(), block.type(), block.text(),
                        block.originalStyle(), metadata));
                generated++;
            } catch (InterruptedException cancelledFailure) {
                cancelled = true;
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException failure) {
                failures.add(block.id() + ": " + rootMessage(failure));
            }
            } finally {
                completed++;
                progress.onProgress(Progress.of(completed, total, block.id(),
                        generated, reused, failures.size(), startedNanos));
            }
            if (cancelled) break;
        }
        return new Result(document.withBlocks(updated), generated, reused, failures,
                cancelled);
    }

    public boolean requiresPreparation(ReadableDocument document,
                                       SecondarySemanticReadingPolicy policy) {
        if (document == null || document.format() != SourceDocumentFormat.DOCX
                || policy == null
                || !policy.includes(SecondarySemanticComponentKind.IMAGE)) return false;
        return document.blocks().stream()
                .filter(block -> block.type() == DocumentBlockType.IMAGE_NOTICE)
                .filter(block -> !block.metadata().getOrDefault(
                        "embeddedImageBase64", "").isBlank())
                .anyMatch(block -> {
                    String description = block.metadata().getOrDefault("description", "").strip();
                    if (description.isBlank()) return true;
                    if (!MODEL.equals(block.metadata().getOrDefault("descriptionSource", ""))) {
                        return false;
                    }
                    return !embeddedFingerprint(block).equals(block.metadata()
                            .getOrDefault("descriptionSourceFingerprint", ""));
                });
    }

    private static DocumentContentItem content(DocumentBlock block) {
        return new DocumentContentItem(block.id(), DocumentContentKind.IMAGE,
                block.text(), block.metadata().getOrDefault("description", ""), List.of(),
                List.of(block.id()), BuildDocumentContentProjectionUseCase.sha256(
                block.id() + "|" + block.metadata().getOrDefault("embeddedImagePath", "")),
                1L, new WordContentAnchor(block.id(), block.metadata()));
    }

    static String nearbyContext(List<DocumentBlock> blocks, int index) {
        ArrayList<DocumentBlock> previous = new ArrayList<>();
        for (int current = index - 1; current >= 0 && previous.size() < 3; current--) {
            DocumentBlock block = blocks.get(current);
            if (isContextText(block)) previous.add(0, block);
        }
        DocumentBlock following = null;
        for (int current = index + 1; current < blocks.size(); current++) {
            DocumentBlock block = blocks.get(current);
            if (isContextText(block)) {
                following = block;
                break;
            }
        }
        StringBuilder result = new StringBuilder(
                "CONTEXTO ORIENTATIVO; NO ES EVIDENCIA VISUAL.\n");
        for (DocumentBlock block : previous) {
            result.append("ANTERIOR ").append(block.id()).append(": ")
                    .append(block.text()).append('\n');
        }
        if (following != null) {
            result.append("POSTERIOR ").append(following.id()).append(": ")
                    .append(following.text()).append('\n');
        }
        String context = result.toString().strip();
        return context.length() <= 2400 ? context : context.substring(0, 2400);
    }

    private static boolean isContextText(DocumentBlock block) {
        return block.type() != DocumentBlockType.IMAGE_NOTICE && !block.text().isBlank();
    }

    private static boolean safeDescription(String text) {
        int words = words(text);
        if (words < 12 || words > 110) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("insufficient_evidence")
                && !lower.contains("```") && !lower.contains("<math");
    }

    private static int words(String text) {
        return text == null || text.isBlank() ? 0 : text.strip().split("\\s+").length;
    }

    private static String trimWords(String text, int maximum) {
        String[] words = text.strip().split("\\s+");
        return words.length <= maximum ? text.strip()
                : String.join(" ", java.util.Arrays.copyOf(words, maximum));
    }

    private static String property(String json, String name) {
        if (json == null) return "";
        var matcher = java.util.regex.Pattern.compile("\\\""
                + java.util.regex.Pattern.quote(name) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
                .matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName() : message;
    }

    private static String embeddedFingerprint(DocumentBlock block) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(
                    block.metadata().getOrDefault("embeddedImageBase64", ""));
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest
                    .getInstance("SHA-256").digest(bytes));
        } catch (IllegalArgumentException | java.security.NoSuchAlgorithmException invalid) {
            return "";
        }
    }

    public record Result(ReadableDocument document, int generated, int reused,
                         List<String> failures, boolean cancelled) {
        public Result(ReadableDocument document, int generated, int reused,
                      List<String> failures) {
            this(document, generated, reused, failures, false);
        }

        public Result {
            Objects.requireNonNull(document, "document");
            failures = failures == null ? List.of() : List.copyOf(failures);
        }

        public boolean changed() { return generated > 0; }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(Progress progress);
    }

    public record Progress(int completed, int total, String currentBlockId,
                           int generated, int reused, int failed,
                           long elapsedSeconds, long estimatedRemainingSeconds) {
        static Progress of(int completed, int total, String blockId, int generated,
                           int reused, int failed, long startedNanos) {
            long elapsed = Math.max(0L,
                    (System.nanoTime() - startedNanos) / 1_000_000_000L);
            long remaining = completed <= 0 ? -1L
                    : Math.max(0L, Math.round((elapsed / (double) completed)
                    * Math.max(0, total - completed)));
            return new Progress(completed, total,
                    blockId == null ? "" : blockId, generated, reused, failed,
                    elapsed, remaining);
        }
    }
}
