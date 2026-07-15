package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Coordinates long-running writers with destructive maintenance of a project's jobs directory. */
final class AudioJobWorkspaceLockRegistry {
    private static final ConcurrentHashMap<Path, ReentrantReadWriteLock> LOCKS = new ConcurrentHashMap<>();

    private AudioJobWorkspaceLockRegistry() { }

    static Lock writer(Path projectDirectory) { return lock(projectDirectory).readLock(); }
    static Lock maintenance(Path projectDirectory) { return lock(projectDirectory).writeLock(); }

    private static ReentrantReadWriteLock lock(Path projectDirectory) {
        Path key = projectDirectory.toAbsolutePath().normalize();
        return LOCKS.computeIfAbsent(key, ignored -> new ReentrantReadWriteLock(true));
    }
}
