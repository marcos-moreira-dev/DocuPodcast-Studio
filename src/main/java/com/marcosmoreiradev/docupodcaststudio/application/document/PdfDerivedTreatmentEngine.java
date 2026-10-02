package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.util.List;
import java.io.IOException;

/** Local execution contract for one optional PDF interpretation. */
public interface PdfDerivedTreatmentEngine {
    String id();

    String version();

    PdfDerivedTreatmentKind kind();

    PdfDerivedTreatment generate(PreparedPdfPage page,
                                 List<PdfRegion> sourceRegions,
                                 PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException;
}
