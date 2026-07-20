package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class LocalProcessExecutor {
    Result run(List<String> command, Path workingDirectory, ExecutionContext context)
            throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) throw new IOException("external command is empty");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (workingDirectory != null) builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        Duration timeout = context.policy().timeout();
        long deadline = System.nanoTime() + timeout.toNanos();
        while (process.isAlive()) {
            if (context.cancellation().cancellationRequested()) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
                throw new InterruptedException("media operation cancelled");
            }
            if (System.nanoTime() >= deadline) {
                process.destroyForcibly();
                throw new IOException("external command timed out after " + timeout.toSeconds() + " seconds");
            }
            process.waitFor(100, TimeUnit.MILLISECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.exitValue(), output);
    }

    record Result(int exitCode, String output) { }
}
