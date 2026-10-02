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
            case SHOW_WELCOME -> AppIcon.PRODUCT_HOME;
            case OPEN_DOCUMENT_READER -> AppIcon.PRODUCT_DOCUMENT_FRAGMENT;
            case OPEN_SOURCE_DOCUMENT -> AppIcon.PRODUCT_SOURCE_DOCUMENT;
            case OPEN_THEATRE_SCRIPT -> AppIcon.PRODUCT_THEATRE_STRUCTURE;
            case OPEN_THEATRE_IMAGE_GENERATION -> AppIcon.PRODUCT_THEATRE_FRAME_STUDIO;
            case OPEN_NARRATIVE_VISUAL_PRODUCTION -> AppIcon.PRODUCT_DOCUMENTARY_VIDEO_CONTENT;
            case IMPORT_THEATRE_GRAMMAR -> AppIcon.PRODUCT_THEATRE_GRAMMAR_IMPORT;
            case REFRESH_THEATRE_PACKAGE -> AppIcon.REFRESH;
            case EXPORT_THEATRE_GRAMMAR_TEMPLATE -> AppIcon.PRODUCT_THEATRE_GRAMMAR_TEMPLATE;
            case IMPORT_NARRATIVE_VIDEO_GRAMMAR -> AppIcon.UPLOAD;
            case EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE -> AppIcon.TEXT;
            case LISTEN_DOCUMENT, PLAY_SELECTION -> AppIcon.LISTEN;
            case NEW_PROJECT -> AppIcon.PRODUCT_PROJECT_NEW;
            case CREATE_DOCUMENT_VIDEO_BATCH -> AppIcon.PRODUCT_DOCUMENTARY_VIDEO_CONTENT;
            case OPEN_PROJECT -> AppIcon.PRODUCT_PROJECT_OPEN;
            case SAVE_PROJECT, SAVE_PROJECT_AS -> AppIcon.PRODUCT_PROJECT_SAVE;
            case PREPARE_DOCUMENT_READING, PREPARE_TECHNICAL_PROBLEM -> AppIcon.PRODUCT_READING_PREPARE;
            case GENERATE_AUDIO -> AppIcon.PRODUCT_AUDIO_GENERATE;
            case CANCEL_AUDIO_JOB -> AppIcon.PRODUCT_AUDIO_CANCEL;
            case OPEN_VOICE_LIBRARY -> AppIcon.VOICE;
            case IMPORT_VOICE_SAMPLE -> AppIcon.UPLOAD;
            case TOGGLE_RIGHT_RAIL -> AppIcon.RAIL;
            case TOGGLE_DOCUMENT_PLAYBAR_DOCK -> AppIcon.PRODUCT_PLAYBAR_DOCK;
            case IMPORT_BRIDGE_IMAGE_FOR_SELECTION -> AppIcon.IMAGE;
            case CREATE_STORYBOARD -> AppIcon.STORYBOARD;
            case TOGGLE_FULLSCREEN -> AppIcon.FULLSCREEN;
            case OPEN_SETTINGS -> AppIcon.PRODUCT_SETTINGS;
            case OPEN_GUIDE -> AppIcon.PRODUCT_GUIDE;
            case OPEN_EXAMPLE_PROJECT -> AppIcon.PRODUCT_EXAMPLE_PROJECT;
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
