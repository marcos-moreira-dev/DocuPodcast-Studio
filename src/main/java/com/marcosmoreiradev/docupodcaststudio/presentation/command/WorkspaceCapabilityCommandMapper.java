package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapability;

import java.util.Objects;

/** Maps legacy workspace capabilities to the new command catalog. */
public final class WorkspaceCapabilityCommandMapper {
    public AppCommandId commandFor(WorkspaceCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return switch (capability) {
            case SHOW_WELCOME -> AppCommandId.SHOW_WELCOME;
            case CREATE_PROJECT -> AppCommandId.NEW_PROJECT;
            case OPEN_PROJECT -> AppCommandId.OPEN_PROJECT;
            case SAVE_PROJECT -> AppCommandId.SAVE_PROJECT;
            case IMPORT_WORD -> AppCommandId.OPEN_SOURCE_DOCUMENT;
            case OPEN_SCRIPT_WORKSPACE -> AppCommandId.PREPARE_DOCUMENT_READING;
            case OPEN_VOICE_LIBRARY -> AppCommandId.OPEN_VOICE_LIBRARY;
            case OPEN_AUDIO_WORKSPACE -> AppCommandId.OPEN_AUDIO_JOBS;
            case OPEN_STORYBOARD_WORKSPACE -> AppCommandId.OPEN_STORYBOARD;
            case CONFIGURE_READING_PROFILE -> AppCommandId.OPEN_SETTINGS;
            case LISTEN_DOCUMENT -> AppCommandId.LISTEN_DOCUMENT;
            case CREATE_SCRIPT -> AppCommandId.PREPARE_DOCUMENT_READING;
            case PREPARE_AI_VOICE -> AppCommandId.ASSIGN_AI_VOICE_TO_SELECTION;
            case PREPARE_HUMAN_VOICE -> AppCommandId.ASSIGN_HUMAN_RECORDING_TO_SELECTION;
            case IMPORT_VOICE_SAMPLE -> AppCommandId.IMPORT_VOICE_SAMPLE;
            case CREATE_STORYBOARD -> AppCommandId.CREATE_STORYBOARD;
            case IMPORT_STORYBOARD_IMAGE -> AppCommandId.IMPORT_IMAGE_FOR_SELECTION;
            case ASSOCIATE_STORYBOARD_IMAGE -> AppCommandId.ASSOCIATE_IMAGE_TO_SELECTION;
            case GENERATE_AUDIO -> AppCommandId.GENERATE_AUDIO;
            case CANCEL_AUDIO_JOB -> AppCommandId.CANCEL_AUDIO_JOB;
            case EXPORT_PROJECT_BUNDLE -> AppCommandId.EXPORT_PROJECT_BUNDLE;
            case EXPORT_PODCAST_WAV -> AppCommandId.EXPORT_PODCAST_WAV;
            case EXPORT_DIAGNOSTIC_REPORT -> AppCommandId.EXPORT_DIAGNOSTIC_REPORT;
            case PLAY_SELECTION -> AppCommandId.PLAY_SELECTION;
            case OPEN_GUIDE -> AppCommandId.OPEN_GUIDE;
            case OPEN_WORD_GUIDE -> AppCommandId.OPEN_WORD_GUIDE;
        };
    }
}
