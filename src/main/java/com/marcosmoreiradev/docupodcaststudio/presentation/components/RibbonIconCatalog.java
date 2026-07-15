package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;

/** Semantic PNG icon mapping used by RibbonButton. */
public final class RibbonIconCatalog {
    private RibbonIconCatalog() {
    }

    public static AppIcon iconFor(AppCommandId commandId) {
        if (commandId == null) {
            return AppIcon.DEFAULT;
        }
        return switch (commandId) {
            case SHOW_WELCOME -> AppIcon.TEXT;
            case OPEN_SOURCE_DOCUMENT -> AppIcon.OPEN_SOURCE;
            case OPEN_THEATRE_SCRIPT -> AppIcon.STORYBOARD;
            case OPEN_THEATRE_IMAGE_GENERATION -> AppIcon.IMAGE;
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> AppIcon.IMAGE;
            case IMPORT_THEATRE_GRAMMAR -> AppIcon.UPLOAD;
            case EXPORT_THEATRE_GRAMMAR_TEMPLATE -> AppIcon.TEXT;
            case IMPORT_NARRATIVE_VIDEO_GRAMMAR -> AppIcon.UPLOAD;
            case EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE -> AppIcon.TEXT;
            case LISTEN_DOCUMENT, PLAY_SELECTION -> AppIcon.LISTEN;
            case NEW_PROJECT -> AppIcon.NEW_PROJECT;
            case OPEN_PROJECT -> AppIcon.OPEN_PROJECT;
            case SAVE_PROJECT, SAVE_PROJECT_AS -> AppIcon.SAVE;
            case PREPARE_DOCUMENT_READING, PREPARE_TECHNICAL_PROBLEM -> AppIcon.PREPARE;
            case GENERATE_AUDIO -> AppIcon.GENERATE_AUDIO;
            case CANCEL_AUDIO_JOB -> AppIcon.CANCEL;
            case OPEN_VOICE_LIBRARY -> AppIcon.VOICE;
            case IMPORT_VOICE_SAMPLE -> AppIcon.UPLOAD;
            case TOGGLE_RIGHT_RAIL -> AppIcon.RAIL;
            case IMPORT_BRIDGE_IMAGE_FOR_SELECTION -> AppIcon.IMAGE;
            case CREATE_STORYBOARD -> AppIcon.STORYBOARD;
            case TOGGLE_FULLSCREEN -> AppIcon.FULLSCREEN;
            case OPEN_SETTINGS -> AppIcon.SETTINGS;
            case OPEN_GUIDE -> AppIcon.HELP;
            case OPEN_EXAMPLE_PROJECT -> AppIcon.EXAMPLES;
            case EXPORT_PODCAST_WAV -> AppIcon.WAV;
            case EXPORT_PROJECT_BUNDLE -> AppIcon.BUNDLE;
            case EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO, EXPORT_SIMPLE_VIDEO_PACKAGE, EXPORT_THEATRE_WORK, EXPORT_THEATRE_PORTION -> AppIcon.VIDEO;
            case EXPORT_THEATRE_SPATIAL_VIEW -> AppIcon.FULLSCREEN;
            case OPEN_EXPORT_CENTER -> AppIcon.CHECK;
            case INSPECT_EXPORT_READINESS -> AppIcon.CHECK;
            case OPEN_EXPORTS_FOLDER, OPEN_PROJECT_FOLDER -> AppIcon.FOLDER;
            case TOGGLE_RIBBON_COLLAPSED -> AppIcon.COLLAPSE;
            default -> AppIcon.DEFAULT;
        };
    }
}
