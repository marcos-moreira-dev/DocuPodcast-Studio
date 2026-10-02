package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.reading.BuildPreparedReadingProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.MaterializeNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.UpdateNarrationSegmentTextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.ValidateNarrationScriptUseCase;

/** Internal prepared-reading compatibility services backed by the legacy segment payload. */
public record ScriptApplicationServices(
        BuildPreparedReadingProjectionUseCase buildPreparedReadingProjection,
        BuildNarrationScriptUseCase buildNarrationScript,
        BuildPreparedPdfNarrationUseCase buildPreparedPdfNarration,
        ValidateNarrationScriptUseCase validateNarrationScript,
        UpdateNarrationSegmentTextUseCase updateNarrationSegmentText,
        MaterializeNarrationScriptUseCase materializeNarrationScript
) {
}
