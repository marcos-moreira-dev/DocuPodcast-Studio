package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;

/** Result of a full managed local image engine smoke flow. */
public record ImageEngineSmokeReport(
        boolean success,
        ImageEngineRuntimeState state,
        ImageEngineSmokeStage stage,
        Path outputImage,
        int width,
        int height,
        String outputProfile,
        String aspectRatio,
        String userMessage,
        String diagnostic
) {
    public ImageEngineSmokeReport(boolean success,
                                  ImageEngineRuntimeState state,
                                  ImageEngineSmokeStage stage,
                                  Path outputImage,
                                  String userMessage,
                                  String diagnostic) {
        this(success, state, stage, outputImage, 0, 0, "", "", userMessage, diagnostic);
    }

    public ImageEngineSmokeReport(boolean success,
                                  ImageEngineRuntimeState state,
                                  String userMessage,
                                  String diagnostic) {
        this(success, state, success ? ImageEngineSmokeStage.COMPLETE : ImageEngineSmokeStage.FAILED,
                null, 0, 0, "", "", userMessage, diagnostic);
    }

    public ImageEngineSmokeReport {
        state = state == null ? ImageEngineRuntimeState.ERROR : state;
        stage = stage == null ? (success ? ImageEngineSmokeStage.COMPLETE : ImageEngineSmokeStage.FAILED) : stage;
        outputProfile = outputProfile == null ? "" : outputProfile.strip();
        aspectRatio = aspectRatio == null ? "" : aspectRatio.strip();
        userMessage = userMessage == null ? "" : userMessage.strip();
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
    }
}
