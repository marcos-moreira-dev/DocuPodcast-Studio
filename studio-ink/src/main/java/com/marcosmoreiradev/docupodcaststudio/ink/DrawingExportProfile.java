package com.marcosmoreiradev.docupodcaststudio.ink;

public record DrawingExportProfile(int scale, boolean cropToContent, boolean includePlacedImages) {
    public DrawingExportProfile {
        scale = Math.max(1, Math.min(8, scale));
    }
}
