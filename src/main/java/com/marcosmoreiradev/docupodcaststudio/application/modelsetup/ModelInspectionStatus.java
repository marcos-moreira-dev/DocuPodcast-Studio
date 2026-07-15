package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** Result category for checking a local model folder. */
public enum ModelInspectionStatus {
    READY,
    MISSING_FOLDER,
    MISSING_REQUIRED_FILES,
    CHECKSUM_NOT_PROVIDED
}
