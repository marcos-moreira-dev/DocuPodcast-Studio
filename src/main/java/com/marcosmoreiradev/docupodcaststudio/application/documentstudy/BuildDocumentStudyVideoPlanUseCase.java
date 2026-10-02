package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioCoverageSnapshotAssembler;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ReusableAudioCoverage;
import com.marcosmoreiradev.docupodcaststudio.application.audio.PcmWavDurationProbe;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfContentAnchor;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationFocusMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationFragmentBindingMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializePdfDocumentContentAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializeWordDocumentContentAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds documentary frames in the format-neutral source order. */
public final class BuildDocumentStudyVideoPlanUseCase {
    private static final String FRAME_DIRECTORY = "exports/document-study-frames";
    private final DocumentStudyVideoContentResolver contentResolver;
    private final DocumentStudySlideCompositor compositor;
    private final MaterializePdfDocumentContentAssetUseCase materializePdfContentAsset;
    private final MaterializeWordDocumentContentAssetUseCase materializeWordContentAsset;
    private final PcmWavDurationProbe wavDurationProbe = new PcmWavDurationProbe();

    public BuildDocumentStudyVideoPlanUseCase() {
        this(new DocumentStudyVideoContentResolver(), new DocumentStudySlideCompositor(), null, null);
    }

    public BuildDocumentStudyVideoPlanUseCase(DocumentStudyVideoContentResolver contentResolver,
                                              DocumentStudySlideCompositor compositor) {
        this(contentResolver, compositor, null, null);
    }

    public BuildDocumentStudyVideoPlanUseCase(DocumentStudyVideoContentResolver contentResolver,
                                              DocumentStudySlideCompositor compositor,
                                              MaterializePdfDocumentContentAssetUseCase materializePdfContentAsset) {
        this(contentResolver, compositor, materializePdfContentAsset, null);
    }

    public BuildDocumentStudyVideoPlanUseCase(DocumentStudyVideoContentResolver contentResolver,
                                              DocumentStudySlideCompositor compositor,
                                              MaterializePdfDocumentContentAssetUseCase materializePdfContentAsset,
                                              MaterializeWordDocumentContentAssetUseCase materializeWordContentAsset) {
        this.contentResolver = contentResolver;
        this.compositor = compositor;
        this.materializePdfContentAsset = materializePdfContentAsset;
        this.materializeWordContentAsset = materializeWordContentAsset;
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 ReadableDocument document,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        if (project == null) throw new IllegalArgumentException("Se requiere proyecto documental.");
        if (document == null) throw new IllegalArgumentException("Se requiere el Word/DOCX abierto.");
        return buildResolved(project, document.title(), document.sourcePath(),
                contentResolver.resolve(document,
                        project.study().documentaryVideoConfiguration(),
                        project.documentListeningPreferences()
                                .secondarySemanticPolicy()),
                script, jobs, projectDirectory, options);
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 DocumentContentProjection projection,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        if (project == null) throw new IllegalArgumentException("Se requiere proyecto documental.");
        if (projection == null) throw new IllegalArgumentException("Se requiere la proyeccion documental.");
        return buildResolved(project, projection.title(), projection.sourcePath(),
                contentResolver.resolve(projection,
                        project.study().documentaryVideoConfiguration(),
                        project.documentListeningPreferences()
                                .secondarySemanticPolicy()),
                script, jobs, projectDirectory, options);
    }

    /**
     * Production export boundary. Coverage/composition has already authorized
     * these exact files against the effective acoustic request, so the video plan
     * consumes that resolution instead of reconstructing another fingerprint.
     */
    public SimpleVideoPlan build(DocuPodcastProject project,
                                 DocumentContentProjection projection,
                                 NarrationScriptDocument script,
                                 AudioCoverageSnapshotAssembler.Result resolvedAudio,
                                 Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        return build(project, projection, script, resolvedAudio, projectDirectory, options, Map.of());
    }

    public SimpleVideoPlan build(DocuPodcastProject project, DocumentContentProjection projection,
            NarrationScriptDocument script, AudioCoverageSnapshotAssembler.Result resolvedAudio,
            Path projectDirectory, DocumentTextVideoOptions options, Map<String, Path> illustrations) throws IOException {
        if (project == null) throw new IllegalArgumentException("Se requiere proyecto documental.");
        if (projection == null) throw new IllegalArgumentException("Se requiere la proyeccion documental.");
        if (resolvedAudio == null || !resolvedAudio.readyForPreflight()) {
            throw new IllegalArgumentException("Se requiere audio reconciliado y vigente.");
        }
        return buildResolved(project, projection.title(), projection.sourcePath(),
                contentResolver.resolve(projection,
                        project.study().documentaryVideoConfiguration(),
                        project.documentListeningPreferences().secondarySemanticPolicy()),
                script, resolvedAudio.exportJobs(), projectDirectory, options,
                resolvedAudio.coverage(), illustrations);
    }

    private SimpleVideoPlan buildResolved(DocuPodcastProject project,
                                          String documentTitle,
                                          Path sourcePath,
                                          List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                          NarrationScriptDocument script,
                                          List<AudioJobSnapshot> jobs,
                                          Path projectDirectory,
                                          DocumentTextVideoOptions options) throws IOException {
        return buildResolved(project, documentTitle, sourcePath, resolvedItems, script,
                jobs, projectDirectory, options, false);
    }

    private SimpleVideoPlan buildResolved(DocuPodcastProject project,
                                          String documentTitle,
                                          Path sourcePath,
                                          List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                          NarrationScriptDocument script,
                                          List<AudioJobSnapshot> jobs,
                                          Path projectDirectory,
                                          DocumentTextVideoOptions options,
                                          boolean reconciledAudio) throws IOException {
        return buildResolved(project, documentTitle, sourcePath, resolvedItems, script,
                jobs, projectDirectory, options, null);
    }

    private SimpleVideoPlan buildResolved(DocuPodcastProject project,
                                          String documentTitle,
                                          Path sourcePath,
                                          List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                          NarrationScriptDocument script,
                                          List<AudioJobSnapshot> jobs,
                                          Path projectDirectory,
                                          DocumentTextVideoOptions options,
                                          ReusableAudioCoverage.Report unitCoverage) throws IOException {
        return buildResolved(project, documentTitle, sourcePath, resolvedItems, script, jobs,
                projectDirectory, options, unitCoverage, Map.of());
    }

    private SimpleVideoPlan buildResolved(DocuPodcastProject project, String documentTitle, Path sourcePath,
            List<DocumentStudyVideoContentResolver.Item> resolvedItems, NarrationScriptDocument script,
            List<AudioJobSnapshot> jobs, Path projectDirectory, DocumentTextVideoOptions options,
            ReusableAudioCoverage.Report unitCoverage, Map<String, Path> illustrations) throws IOException {
        if (script == null || script.empty()) throw new IllegalArgumentException("Se requiere lectura preparada.");
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null) throw new IllegalArgumentException("Se requiere carpeta de proyecto.");
        Path frameDirectory = root.resolve(FRAME_DIRECTORY);
        Files.createDirectories(frameDirectory);
        DocumentTextVideoOptions effective = options == null ? DocumentTextVideoOptions.defaults() : options;
        DocumentStudyVideoConfiguration configuration = project.study().documentaryVideoConfiguration();
        Map<String, AudioSegmentSnapshot> audio = unitCoverage == null
                ? completedAudioBySegment(jobs, root)
                : completedAudioByUnit(unitCoverage);
        Map<String, String> audioTextByUnit = unitCoverage == null
                ? Map.of() : audioTextByUnit(unitCoverage);
        Map<String, List<NarrationSegment>> segments = segmentsByBlock(script);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        int frameIndex = 1;
        for (DocumentStudyVideoContentResolver.Item item : resolvedItems) {
            if (!item.enabled()) continue;
            Path imageFile = frameDirectory.resolve(fileName(item));
            String imageRelativePath = portableRelativePath(root, imageFile);
            if (item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE
                    && item.content().wordAnchor().isPresent()
                    && item.content().narrationSegmentIds().stream()
                    .noneMatch(id -> script.segmentById(id).isPresent())) {
                compositor.composeTable(imageFile, item.block(), configuration, effective);
                frames.add(new SimpleVideoFrame(
                        "DOC-TABLE-" + token(item.block().id()),
                        "TABLE-" + token(item.block().id()),
                        "Tabla",
                        "",
                        "",
                        imageRelativePath,
                        "",
                        0.0,
                        item.durationSeconds(),
                        true,
                        false,
                        true));
                frameIndex++;
                continue;
            }
            if (item.kind() == DocumentStudyVideoContentResolver.Kind.CLOSING) {
                var closing = configuration.closingSlide(item.block().id())
                        .orElseThrow(() -> new IOException("No existe la diapositiva final "
                                + item.block().id() + "."));
                compositor.composeClosing(imageFile, closing, project, root, effective);
                frames.add(new SimpleVideoFrame(
                        "DOC-CLOSING-" + token(closing.id()),
                        "CLOSING-" + token(closing.id()),
                        closing.title().isBlank() ? "Cierre" : closing.title(),
                        "",
                        "",
                        imageRelativePath,
                        "",
                        0.0,
                        closing.durationSeconds(),
                        true,
                        false,
                        true));
                frameIndex++;
                continue;
            }
            List<NarrationSegment> blockSegments = secondaryNarrationAllowed(project, item)
                    ? segmentsForItem(item, script, segments)
                    : List.of();
            if (blockSegments.isEmpty()
                    && item.content().secondarySemanticComponent()) {
                ResolvedSourceVisual sourceVisual = sourceVisual(item, resolvedItems, project, sourcePath, root);
                compositor.composeSourceCapture(imageFile, item.block(), sourceVisual.path(),
                        item.paragraphVisual(), configuration, project, root, effective);
                frames.add(new SimpleVideoFrame(
                        "DOC-SEMANTIC-" + String.format(Locale.ROOT, "%04d", frameIndex++),
                        "SEMANTIC-" + token(item.content().contentId()),
                        item.block().id(), "", "", imageRelativePath, "", 0.0,
                        item.durationSeconds(), true, false, true));
                continue;
            }
            if (blockSegments.isEmpty()) continue;
            ResolvedSourceVisual resolvedVisual = null;
            if (item.presentationMode() == DocumentPresentationMode.SOURCE_CAPTURE
                    && !(item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE
                    && item.content().wordAnchor().isPresent())) {
                resolvedVisual = sourceVisual(item, resolvedItems, project, sourcePath, root);
                compositor.composeSourceCapture(imageFile, item.block(), resolvedVisual.path(),
                        item.paragraphVisual(), configuration, project, root, effective);
            } else if (item.presentationMode() != DocumentPresentationMode.TEXT_RENDER) {
                if (item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH) {
                    compositor.composeParagraph(imageFile, item.block(), item.paragraphVisual(), configuration,
                            project, root, effective);
                } else if (item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE
                        && item.content().wordAnchor().isPresent()) {
                    compositor.composeTable(imageFile, item.block(), configuration, effective);
                } else {
                    compositor.composeCover(imageFile, item.block(), configuration, effective);
                }
            }
            for (NarrationSegment segment : blockSegments) {
                List<AudioSegmentSnapshot> clips = audioForSegment(audio, segment,
                        unitCoverage != null);
                if (clips.isEmpty()) clips = java.util.Collections.singletonList(null);
                for (AudioSegmentSnapshot clip : clips) {
                    String frameId = "DOC-FRAME-" + String.format(Locale.ROOT, "%04d", frameIndex++);
                    String frameSegmentId = clip == null ? segment.id() : clip.segmentId();
                    String audioPath = clip == null ? "" : clip.audioRelativePath();
                    double duration = clip == null ? 0.0 : actualWavDuration(root, clip);
                    Path frameImage = imageFile;
                    String renderedText = item.block().text();
                    if (item.presentationMode() == DocumentPresentationMode.TEXT_RENDER) {
                        var fragment = PdfNarrationFragmentBindingMetadata.resolve(
                                segment.metadata().get(PdfNarrationFragmentBindingMetadata.KEY),
                                frameSegmentId).orElse(null);
                        String unitText = audioTextByUnit.getOrDefault(frameSegmentId, "");
                        String narratedFragment = fragment != null && !fragment.sourceText().isBlank()
                                ? fragment.sourceText()
                                : !unitText.isBlank() ? unitText : segment.narrationText();
                        boolean wordParagraph = item.content().wordAnchor().isPresent()
                                && item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH;
                        renderedText = wordParagraph ? item.block().text() : narratedFragment;
                        DocumentBlock fragmentBlock = new DocumentBlock(
                                item.block().id() + "-" + token(frameSegmentId),
                                item.block().type(), renderedText, item.block().originalStyle(),
                                item.block().metadata());
                        frameImage = frameDirectory.resolve(fragmentFileName(item, frameSegmentId));
                        if (illustrations.containsKey(segment.id())
                                && item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH) {
                            compositor.composeIllustratedParagraph(frameImage, fragmentBlock, narratedFragment,
                                    item.paragraphVisual(), configuration, project, root, effective, illustrations.get(segment.id()));
                        } else if (wordParagraph) {
                            compositor.composeNarratedParagraph(frameImage, item.block(),
                                    narratedFragment, item.paragraphVisual(), configuration,
                                    project, root, effective);
                        } else if (item.kind() == DocumentStudyVideoContentResolver.Kind.PARAGRAPH) {
                            compositor.composeNarratedParagraph(frameImage, fragmentBlock,
                                    item.paragraphVisual(), configuration, project, root, effective);
                        } else if (item.kind() == DocumentStudyVideoContentResolver.Kind.TABLE
                                && item.content().wordAnchor().isPresent()) {
                            compositor.composeTable(frameImage, fragmentBlock, configuration, effective);
                        } else {
                            compositor.composeNarratedCover(frameImage, fragmentBlock, configuration, effective);
                        }
                    }
                    String frameImageRelativePath = portableRelativePath(root, frameImage);
                    NarratedFrameBinding binding = narratedBinding(frameId, frameSegmentId,
                            segment, item, resolvedVisual, frameImageRelativePath, audioPath,
                            duration, renderedText);
                    frames.add(new SimpleVideoFrame(
                            frameId,
                            frameSegmentId,
                            segment.title().isBlank() ? item.block().id() : segment.title(),
                            segment.narrationText(),
                            "",
                            frameImageRelativePath,
                            audioPath,
                            duration,
                            0.35,
                            true,
                            clip != null && clip.completed(),
                            false,
                            List.of(),
                            List.of(),
                            binding));
                }
            }
        }
        String title = configuration.videoTitle().isBlank() ? documentTitle : configuration.videoTitle();
        SimpleVideoPlan plan = new SimpleVideoPlan("Video documental - " + title, frames, 0.35, Instant.now());
        new NarratedFrameBindingManifestWriter().write(plan, root);
        new NarrationFragmentAuditWriter().write(plan, root);
        return plan;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioByUnit(
            ReusableAudioCoverage.Report coverage) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (coverage == null) return result;
        for (ReusableAudioCoverage.Entry entry : coverage.entries()) {
            if (entry.ready()) result.put(entry.unit().id(), entry.audio());
        }
        return result;
    }

    private static Map<String, String> audioTextByUnit(
            ReusableAudioCoverage.Report coverage) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (coverage == null) return result;
        for (ReusableAudioCoverage.Entry entry : coverage.entries()) {
            result.put(entry.unit().id(), entry.unit().text());
        }
        return result;
    }

    private double actualWavDuration(Path root, AudioSegmentSnapshot clip) throws IOException {
        Path wav = root.resolve(clip.audioRelativePath()).toAbsolutePath().normalize();
        if (!wav.startsWith(root)) throw new IOException("Ruta WAV fuera del proyecto: " + clip.segmentId());
        return wavDurationProbe.durationSeconds(wav);
    }

    private ResolvedSourceVisual materializePdfRoi(DocumentStudyVideoContentResolver.Item item,
                                                   List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                                   Path sourcePath, Path projectRoot) throws IOException {
        PdfContentAnchor anchor = item.content().pdfAnchor().orElse(null);
        if (anchor == null) return null;
        if (materializePdfContentAsset == null) {
            throw new IOException("No esta disponible el renderizador de ROI PDF para "
                    + item.content().contentId() + ".");
        }
        try {
            MaterializePdfDocumentContentAssetUseCase.Result result = materializePdfContentAsset.materialize(
                    item.content(), resolvedItems.stream()
                            .map(DocumentStudyVideoContentResolver.Item::content).toList(),
                    sourcePath, projectRoot);
            boolean pageFallback = result.cropDecision().cropSource().startsWith("page-fallback:");
            return new ResolvedSourceVisual(result.path(),
                    pageFallback ? NarratedFrameBinding.VisualSource.PAGE_FALLBACK
                            : NarratedFrameBinding.VisualSource.PDF_REGION_CROP,
                    result.cropDecision().sourceBounds(), result.cropDecision().finalCrop());
        } catch (IOException cropFailure) {
            return materializePdfPageFallback(item, sourcePath, projectRoot, anchor, cropFailure);
        }
    }

    private ResolvedSourceVisual materializePdfPageFallback(
            DocumentStudyVideoContentResolver.Item item, Path sourcePath, Path projectRoot,
            PdfContentAnchor anchor, IOException cropFailure) throws IOException {
        DocumentContentRectangle fullPage = new DocumentContentRectangle(
                0.0, 0.0, anchor.pageWidthPoints(), anchor.pageHeightPoints());
        PdfContentAnchor fallbackAnchor = new PdfContentAnchor(anchor.pageNumber(),
                anchor.pageWidthPoints(), anchor.pageHeightPoints(), fullPage,
                anchor.sourceRegionIds(), anchor.sourceRegionBounds(), anchor.sourceRegionTexts(),
                anchor.unreliableSourceRegionIds(), anchor.revision(),
                anchor.visualFingerprint() + "-page-fallback");
        DocumentContentItem fallback = new DocumentContentItem(item.content().contentId(),
                item.content().kind(), item.content().title(), item.content().narrationText(),
                item.content().narrationSegmentIds(), item.content().sourceIds(),
                item.content().legacyContentIds(), item.content().fingerprint(),
                item.content().revision(), item.content().presentationMode(), fallbackAnchor);
        try {
            MaterializePdfDocumentContentAssetUseCase.Result result = materializePdfContentAsset
                    .materialize(fallback, List.of(fallback), sourcePath, projectRoot);
            return new ResolvedSourceVisual(result.path(),
                    NarratedFrameBinding.VisualSource.PAGE_FALLBACK,
                    anchor.roi(), result.cropDecision().finalCrop());
        } catch (IOException pageFailure) {
            if (cropFailure != null) pageFailure.addSuppressed(cropFailure);
            throw pageFailure;
        }
    }

    private ResolvedSourceVisual materializeSourceVisual(DocumentStudyVideoContentResolver.Item item,
                                                         List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                                         Path sourcePath, Path projectRoot) throws IOException {
        if (item.content().pdfAnchor().isPresent()) {
            return materializePdfRoi(item, resolvedItems, sourcePath, projectRoot);
        }
        if (item.content().secondarySemanticComponent()
                && item.content().wordAnchor().isPresent()) {
            if (materializeWordContentAsset == null
                    || !materializeWordContentAsset.canMaterialize(item.content())) {
                return null;
            }
            Path path = materializeWordContentAsset.materialize(
                    item.content(), projectRoot).path();
            return new ResolvedSourceVisual(path,
                    NarratedFrameBinding.VisualSource.WORD_SOURCE_CAPTURE, null, null);
        }
        return null;
    }

    private ResolvedSourceVisual sourceVisual(DocumentStudyVideoContentResolver.Item item,
                                              List<DocumentStudyVideoContentResolver.Item> resolvedItems,
                                              DocuPodcastProject project,
                                              Path sourcePath,
                                              Path projectRoot) throws IOException {
        String assetId = project.study().documentaryVideoConfiguration()
                .content(item.content().contentId())
                .or(() -> ReconcileDocumentStudyVideoConfigurationUseCase
                        .configuredSlide(item.content(), project.study()
                                .documentaryVideoConfiguration()))
                .map(com.marcosmoreiradev.docupodcaststudio.domain.study
                        .DocumentVideoSlideConfiguration::sourceVisualAssetId)
                .orElse("");
        if (!assetId.isBlank()) {
            Path materialized = project.assets().byId(assetId)
                    .filter(com.marcosmoreiradev.docupodcaststudio.domain.assets
                            .ProjectAssetReference::isImage)
                    .map(asset -> projectRoot.resolve(asset.relativePath()).normalize())
                    .filter(path -> path.startsWith(projectRoot))
                    .filter(Files::isRegularFile)
                    .orElse(null);
            if (materialized != null) return new ResolvedSourceVisual(materialized,
                    NarratedFrameBinding.VisualSource.USER_SELECTED_ASSET, null, null);
        }
        return materializeSourceVisual(item, resolvedItems, sourcePath, projectRoot);
    }

    private static NarratedFrameBinding narratedBinding(
            String frameId, String frameSegmentId, NarrationSegment segment,
            DocumentStudyVideoContentResolver.Item item, ResolvedSourceVisual resolvedVisual,
            String imageRelativePath, String audioPath, double durationSeconds,
            String renderedText) throws IOException {
        String explicit = segment.metadata().getOrDefault("sourceBlockId", "").strip();
        String primary = explicit.isBlank()
                ? segment.sourceBlockIds().stream().findFirst().orElse("") : explicit;
        if (primary.isBlank()) {
            throw new IOException("El segmento " + segment.id() + " no declara sourceBlockId.");
        }
        if (!item.content().sourceIds().contains(primary)) {
            throw new IOException("Binding visual incorrecto para " + segment.id()
                    + ": el contenido resuelto no contiene " + primary + ".");
        }
        PdfContentAnchor pdf = item.content().pdfAnchor().orElse(null);
        DocumentContentRectangle expected = expectedSourceBounds(segment, item, pdf);
        var fragment = PdfNarrationFragmentBindingMetadata.resolve(segment.metadata().get(
                PdfNarrationFragmentBindingMetadata.KEY), frameSegmentId).orElse(null);
        List<DocumentContentRectangle> fragmentBoxes = fragment == null
                ? (expected == null ? List.of() : List.of(expected))
                : fragment.playbackBboxes().stream().map(box ->
                        new DocumentContentRectangle(box.xMin(), box.yMin(),
                                box.xMax(), box.yMax())).toList();
        if (fragmentBoxes.isEmpty() && expected != null) fragmentBoxes = List.of(expected);
        DocumentContentRectangle crop = resolvedVisual == null ? null : resolvedVisual.cropBBox();
        NarratedFrameBinding.VisualSource visualSource = item.presentationMode()
                == DocumentPresentationMode.TEXT_RENDER
                ? NarratedFrameBinding.VisualSource.GENERATED_TEXT_FRAME
                : resolvedVisual == null
                ? NarratedFrameBinding.VisualSource.WORD_SOURCE_CAPTURE
                : resolvedVisual.visualSource();
        NarratedFrameBinding.Quality quality;
        String reason;
        boolean requiresPdfContainment = resolvedVisual != null
                && (resolvedVisual.visualSource() == NarratedFrameBinding.VisualSource.PDF_REGION_CROP
                || resolvedVisual.visualSource() == NarratedFrameBinding.VisualSource.PAGE_FALLBACK);
        String fragmentSourceText = fragment == null
                ? item.presentationMode() == DocumentPresentationMode.TEXT_RENDER
                ? renderedText : expectedSourceText(item, pdf, primary)
                : fragment.sourceText();
        boolean fragmentCovered = item.presentationMode() == DocumentPresentationMode.TEXT_RENDER
                ? normalizedText(renderedText).equals(normalizedText(fragmentSourceText))
                : !requiresPdfContainment || crop != null && !fragmentBoxes.isEmpty()
                && fragmentBoxes.stream().allMatch(box -> coverage(box, crop) >= 0.80);
        if (item.presentationMode() == DocumentPresentationMode.SOURCE_CAPTURE
                && pdf != null && requiresPdfContainment && !fragmentCovered) {
            quality = NarratedFrameBinding.Quality.WRONG_VISUAL;
            reason = "WRONG_FRAGMENT_VISUAL: el crop no contiene el fragmento narrado.";
        } else if (item.presentationMode() == DocumentPresentationMode.TEXT_RENDER
                && !fragmentCovered) {
            quality = NarratedFrameBinding.Quality.WRONG_VISUAL;
            reason = "WRONG_FRAGMENT_VISUAL: el texto renderizado no es el fragmento actual.";
        } else if (visualSource == NarratedFrameBinding.VisualSource.PAGE_FALLBACK) {
            quality = NarratedFrameBinding.Quality.COARSE_BUT_VALID;
            reason = "La geometria primaria no era fiable; se conserva la pagina de la misma region.";
        } else if (item.presentationMode() == DocumentPresentationMode.SOURCE_CAPTURE
                && (item.content().sourceIds().size() > 1
                || expected != null && crop != null && area(crop) > area(expected) * 1.75)) {
            quality = NarratedFrameBinding.Quality.COARSE_BUT_VALID;
            reason = "El artefacto primario esta presente con contexto visual adicional.";
        } else {
            quality = NarratedFrameBinding.Quality.CORRECT;
            reason = "La identidad narrativa y la visual coinciden.";
        }
        if (quality == NarratedFrameBinding.Quality.WRONG_VISUAL) {
            throw new IOException("WRONG_VISUAL antes del render para " + frameId
                    + " (segment=" + segment.id() + ", sourceBlockId=" + primary + "). " + reason);
        }
        String renderedTextHash = item.presentationMode() == DocumentPresentationMode.TEXT_RENDER
                ? textHash(renderedText) : "";
        String expectedSourceTextHash = item.presentationMode() == DocumentPresentationMode.TEXT_RENDER
                ? textHash(fragmentSourceText) : "";
        return new NarratedFrameBinding(frameId, frameSegmentId, primary,
                segment.sourceBlockIds(), primary,
                segment.metadata().getOrDefault("sourceBlockType", ""),
                pdf == null ? 0 : pdf.pageNumber(), integerMetadata(segment, "pdfReadingOrder"),
                item.presentationMode(), visualSource, expected, crop,
                imageRelativePath, audioPath, Math.round(durationSeconds * 1000.0),
                renderedTextHash, expectedSourceTextHash,
                fragment == null ? 0 : fragment.sourceTextStart(),
                fragment == null ? fragmentSourceText.length() : fragment.sourceTextEnd(),
                preview(fragmentSourceText),
                fragment == null ? List.of() : fragment.sourceLineIndices(),
                fragment == null ? List.of() : fragment.sourceWordIndices(),
                fragmentBoxes, fragmentCovered, quality, reason);
    }

    private static String expectedSourceText(DocumentStudyVideoContentResolver.Item item,
                                             PdfContentAnchor pdf, String primary) {
        if (pdf != null) return pdf.sourceRegionTexts().getOrDefault(primary, "");
        return item.content().wordAnchor().isPresent() ? item.content().title() : "";
    }

    private static String textHash(String text) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((text == null ? "" : text.strip())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String normalizedText(String value) {
        return (value == null ? "" : value).strip().replaceAll("\\s+", " ");
    }

    private static String preview(String value) {
        String normalized = normalizedText(value);
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 157) + "...";
    }

    private static DocumentContentRectangle expectedSourceBounds(
            NarrationSegment segment, DocumentStudyVideoContentResolver.Item item,
            PdfContentAnchor pdf) {
        if (pdf == null) return null;
        String primary = segment.metadata().getOrDefault("sourceBlockId", "").strip();
        DocumentContentRectangle canonical = pdf.sourceRegionBounds().get(primary);
        if (canonical != null) return canonical;
        DocumentContentRectangle focus = PdfNarrationFocusMetadata.decode(
                        segment.metadata().get(PdfNarrationFocusMetadata.KEY))
                .filter(value -> value.sourceRegionIds().contains(primary))
                .flatMap(value -> value.boxes().stream()
                        .map(box -> new DocumentContentRectangle(box.xMin(), box.yMin(),
                                box.xMax(), box.yMax()))
                        .reduce(DocumentContentRectangle::union))
                .orElse(null);
        if (focus != null) return focus;
        return pdf.roi();
    }

    private static double coverage(DocumentContentRectangle expected,
                                   DocumentContentRectangle crop) {
        double width = Math.max(0.0, Math.min(expected.xMax(), crop.xMax())
                - Math.max(expected.xMin(), crop.xMin()));
        double height = Math.max(0.0, Math.min(expected.yMax(), crop.yMax())
                - Math.max(expected.yMin(), crop.yMin()));
        return area(expected) <= 0.0 ? 0.0 : width * height / area(expected);
    }

    private static double area(DocumentContentRectangle rectangle) {
        return (rectangle.xMax() - rectangle.xMin()) * (rectangle.yMax() - rectangle.yMin());
    }

    private static int integerMetadata(NarrationSegment segment, String key) {
        try {
            return Integer.parseInt(segment.metadata().getOrDefault(key, "-1"));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private record ResolvedSourceVisual(
            Path path, NarratedFrameBinding.VisualSource visualSource,
            DocumentContentRectangle sourceBBox, DocumentContentRectangle cropBBox) { }

    private static List<NarrationSegment> segmentsForItem(
            DocumentStudyVideoContentResolver.Item item,
            NarrationScriptDocument script,
            Map<String, List<NarrationSegment>> bySource) {
        List<NarrationSegment> direct = item.content().narrationSegmentIds().stream()
                .map(script::segmentById).flatMap(java.util.Optional::stream)
                .filter(NarrationSegment::narratable)
                .toList();
        if (!direct.isEmpty()) return direct;
        ArrayList<NarrationSegment> fallback = new ArrayList<>();
        for (String sourceId : item.content().sourceIds()) {
            fallback.addAll(bySource.getOrDefault(sourceId, List.of()));
        }
        if (fallback.isEmpty()) fallback.addAll(
                bySource.getOrDefault(item.content().contentId(), List.of()));
        return fallback.stream()
                .filter(NarrationSegment::narratable)
                .distinct()
                .toList();
    }

    /**
     * Keeps the visual inclusion decision separate from the spoken-content
     * decision.  A source visual may therefore survive as a silent slide even
     * when the current reading policy omits it or a Word image has no admitted
     * description.  Tables/equations keep their deterministic narration when
     * the active policy authorizes it.
     */
    private static boolean secondaryNarrationAllowed(
            DocuPodcastProject project,
            DocumentStudyVideoContentResolver.Item item) {
        if (!item.content().secondarySemanticComponent()) return true;
        SecondarySemanticComponentKind kind = switch (item.content().kind()) {
            case TABLE -> SecondarySemanticComponentKind.TABLE;
            case EQUATION -> SecondarySemanticComponentKind.EQUATION;
            case IMAGE -> SecondarySemanticComponentKind.IMAGE;
            case EXTRA -> SecondarySemanticComponentKind.EXTRA;
            default -> SecondarySemanticComponentKind.NONE;
        };
        if (!project.documentListeningPreferences().secondarySemanticPolicy()
                .includes(kind)) {
            return false;
        }
        // The projection is the authority for whether a description/treatment
        // actually produced narration.  Do not infer speech from the mere
        // presence of a source image, table or formula.
        return item.content().narratable();
    }

    private static String fileName(DocumentStudyVideoContentResolver.Item item) {
        String prefix = switch (item.kind()) {
            case COVER -> "cover-";
            case PARAGRAPH -> "paragraph-";
            case TABLE -> "table-";
            case EQUATION -> "equation-";
            case IMAGE -> "image-";
            case EXTRA -> "extra-";
            case CLOSING -> "closing-";
        };
        return prefix + item.block().id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-") + ".png";
    }

    private static String fragmentFileName(DocumentStudyVideoContentResolver.Item item,
                                           String frameSegmentId) {
        String base = fileName(item);
        return base.substring(0, base.length() - 4) + "-" + token(frameSegmentId) + ".png";
    }

    private static Map<String, List<NarrationSegment>> segmentsByBlock(NarrationScriptDocument script) {
        LinkedHashMap<String, ArrayList<NarrationSegment>> mutable = new LinkedHashMap<>();
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable() || secondaryReadUnit(segment)) continue;
            for (String blockId : segment.sourceBlockIds()) {
                mutable.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(segment);
            }
        }
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        mutable.forEach((blockId, segments) -> result.put(blockId, List.copyOf(segments)));
        return result;
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs, Path root) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null) return result;
        jobs.stream().filter(java.util.Objects::nonNull).sorted(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .forEach(job -> job.segments().stream().filter(segment -> usableAudio(root, segment))
                        .forEach(segment -> result.compute(segment.segmentId(), (id, previous) -> {
                            if (previous != null && manual(previous) && !manual(segment)) return previous;
                            return segment;
                        })));
        return result;
    }

    private static boolean usableAudio(Path root, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) return false;
        Path file = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        try {
            return file.startsWith(root) && Files.isRegularFile(file) && Files.size(file) > 44L;
        } catch (IOException ex) {
            return false;
        }
    }

    private static List<AudioSegmentSnapshot> audioForSegment(
            Map<String, AudioSegmentSnapshot> audio, NarrationSegment segment,
            boolean reconciledAudio) {
        ArrayList<AudioSegmentSnapshot> units = new ArrayList<>();
        String segmentId = segment.id();
        var expected = reconciledAudio ? null
                : AudioGenerationUnit.fromSegment(segment).sourceFingerprint();
        for (AudioSegmentSnapshot clip : audio.values()) {
            boolean compatible = reconciledAudio || manual(clip) || clip.reusableFor(expected);
            if (!compatible) continue;
            if (segmentId.equals(clip.segmentId())) {
                return units.isEmpty() ? List.of(clip) : List.copyOf(units);
            }
            if (clip.segmentId().startsWith(segmentId + "-")) units.add(clip);
        }
        return List.copyOf(units);
    }

    private static boolean manual(AudioSegmentSnapshot segment) {
        return segment != null && segment.audioRelativePath()
                .toLowerCase(Locale.ROOT).endsWith("-manual.wav");
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static String portableRelativePath(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String token(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "-");
    }
}
