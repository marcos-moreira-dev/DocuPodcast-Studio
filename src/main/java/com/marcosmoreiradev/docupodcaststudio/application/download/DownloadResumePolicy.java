package com.marcosmoreiradev.docupodcaststudio.application.download;

import java.nio.file.Files;
import java.nio.file.Path;

/** Small policy for deciding whether a partial local file can be resumed. */
public final class DownloadResumePolicy {
    public boolean canResume(DownloadRequest request) {
        if (request == null || !request.resumeAllowed()) {
            return false;
        }
        Path target = request.targetFile();
        try {
            return target != null && Files.isRegularFile(target) && Files.size(target) > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
