package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import javafx.concurrent.Task;

import java.util.Objects;

/** Starts JavaFX tasks on daemon worker threads with one presentation-level policy. */
public final class FxBackgroundTaskRunner {
    public <T> Task<T> start(String threadName, Task<T> task) {
        Objects.requireNonNull(task, "task");
        Thread worker = new Thread(task, safeThreadName(threadName));
        worker.setDaemon(true);
        worker.start();
        return task;
    }

    public Thread start(String threadName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        Thread worker = new Thread(runnable, safeThreadName(threadName));
        worker.setDaemon(true);
        worker.start();
        return worker;
    }

    private static String safeThreadName(String threadName) {
        String normalized = threadName == null ? "" : threadName.strip();
        return normalized.isBlank() ? "docupodcast-background-task" : normalized;
    }
}
