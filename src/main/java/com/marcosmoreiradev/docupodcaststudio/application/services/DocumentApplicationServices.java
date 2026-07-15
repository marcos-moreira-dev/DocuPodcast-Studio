package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.document.ImportDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentSourceImportService;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildDocumentOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfEnhancedOutlineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfNativeTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfOcrTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfResolvedTextLayerUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfVisualReadingProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CapturePdfVisualRegionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.CreatePdfFromImageFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializeImportedDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.RefreshSourceDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ReconcileTextAnchorsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.RenderPdfVisualPageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfNarratableDocumentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.SearchPdfTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareDocumentListeningUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareListeningSessionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PrepareStudySourceCropsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.UpdateDocumentBlockTypeUseCase;

/** Document import, materialization and review use cases. */
public record DocumentApplicationServices(
        ImportDocumentUseCase importDocument,
        DocumentSourceImportService documentSourceImport,
        MaterializeImportedDocumentUseCase materializeImportedDocument,
        UpdateDocumentBlockTypeUseCase updateDocumentBlockType,
        RefreshSourceDocumentUseCase refreshSourceDocument,
        ReconcileTextAnchorsUseCase reconcileTextAnchors,
        PrepareDocumentListeningUseCase prepareDocumentListening,
        PrepareListeningSessionUseCase prepareListeningSession,
        BuildDocumentOutlineUseCase buildDocumentOutline,
        BuildPdfEnhancedOutlineUseCase buildPdfEnhancedOutline,
        BuildPdfNativeTextLayerUseCase buildPdfNativeTextLayer,
        BuildPdfResolvedTextLayerUseCase buildPdfResolvedTextLayer,
        BuildPdfVisualReadingProjectionUseCase buildPdfVisualReadingProjection,
        BuildPdfOcrTextLayerUseCase buildPdfOcrTextLayer,
        BuildPdfVisualDocumentUseCase buildPdfVisualDocument,
        RenderPdfVisualPageUseCase renderPdfVisualPage,
        SearchPdfTextUseCase searchPdfText,
        ResolvePdfNarratableDocumentUseCase resolvePdfNarratableDocument,
        CapturePdfVisualRegionUseCase capturePdfVisualRegion,
        CreatePdfFromImageFolderUseCase createPdfFromImageFolder,
        PrepareStudySourceCropsUseCase prepareStudySourceCrops
) {
}
