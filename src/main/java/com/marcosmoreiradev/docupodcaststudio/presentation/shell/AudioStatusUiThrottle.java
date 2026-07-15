package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Coalesces frequent audio job progress events before touching JavaFX.
 *
 * <p>Long documents can publish thousands of chunk updates. The audio job itself runs outside
 * JavaFX, but sending every intermediate status directly to {@code Platform.runLater} can still
 * saturate the application thread and make the progress overlay feel frozen. This throttle keeps
 * the latest status, delivers terminal states immediately and lets the UI stay operable.</p>
 */
public final class AudioStatusUiThrottle {
    private static final long RUNNING_STATUS_INTERVAL_MS = 140L;

    private final Consumer<AudioJobStatusDto> receiver;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "docupodcast-audio-status-ui-throttle");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();
    private AudioJobStatusDto pending;
    private boolean scheduled;

    public AudioStatusUiThrottle(Consumer<AudioJobStatusDto> receiver) {
        this.receiver = Objects.requireNonNull(receiver, "receiver");
    }

    public void submit(AudioJobStatusDto status) {
        if (status == null) {
            return;
        }
        long delay = status.running() ? RUNNING_STATUS_INTERVAL_MS : 0L;
        synchronized (lock) {
            pending = status;
            if (scheduled && status.running()) {
                return;
            }
            scheduled = true;
        }
        executor.schedule(this::flushLatest, delay, TimeUnit.MILLISECONDS);
    }

    private void flushLatest() {
        AudioJobStatusDto snapshot;
        synchronized (lock) {
            snapshot = pending;
            pending = null;
            scheduled = false;
        }
        if (snapshot == null) {
            return;
        }
        Platform.runLater(() -> receiver.accept(snapshot));
    }
}
