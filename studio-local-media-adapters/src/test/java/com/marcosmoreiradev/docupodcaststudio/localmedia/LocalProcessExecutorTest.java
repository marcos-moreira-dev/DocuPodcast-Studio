package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LocalProcessExecutorTest {
    @TempDir Path temporary;
    @Test
    void drainsVerboseProcessWhileItIsRunningAndKeepsOnlyBoundedTail() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));

        LocalProcessExecutor.Result result = new LocalProcessExecutor().run(List.of(
                "cmd.exe", "/d", "/c",
                "for /L %i in (1,1,20000) do @echo %i-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
                null, ExecutionContext.defaults("verbose-process-test"));

        assertEquals(0, result.exitCode());
        assertTrue(result.output().length() <= 64 * 1024);
        assertTrue(result.output().contains("20000-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    }

    @Test
    void neverTerminatesAnUnownedExternalProcess() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Process external = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                "Start-Sleep -Seconds 30").start();
        try {
            LocalProcessExecutor.Result result = new LocalProcessExecutor().run(List.of(
                    "cmd.exe", "/d", "/c", "exit 0"), null,
                    ExecutionContext.defaults("ownership-isolation-test"));
            assertEquals(0, result.exitCode());
            assertTrue(external.isAlive(), "an unrelated process must never be killed");
        } finally {
            external.destroyForcibly();
            external.waitFor();
        }
    }

    @Test
    void cancellationTerminatesTheExactOwnedOneShotProcess() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        Path pidFile = temporary.resolve("owned.pid");
        AtomicBoolean cancelled = new AtomicBoolean();
        ExecutionContext context = new ExecutionContext("cancel-owned-test", cancelled::get,
                ProgressSink.NONE, new ExecutionPolicy(Duration.ofSeconds(30), 1), ResourceLease.NONE);
        List<String> command = List.of("powershell.exe", "-NoProfile", "-Command",
                "$PID | Set-Content -LiteralPath '" + pidFile.toString().replace("'", "''")
                        + "'; Start-Sleep -Seconds 30");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = executor.submit(() -> {
                try {
                    new LocalProcessExecutor().run(command, temporary, context);
                    return false;
                } catch (InterruptedException expected) {
                    return true;
                }
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!Files.isRegularFile(pidFile) && System.nanoTime() < deadline) {
                Thread.sleep(25L);
            }
            assertTrue(Files.isRegularFile(pidFile));
            long ownedPid = Long.parseLong(Files.readString(pidFile).strip());
            cancelled.set(true);
            assertTrue(result.get(5, TimeUnit.SECONDS));
            long deathDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (ProcessHandle.of(ownedPid).map(ProcessHandle::isAlive).orElse(false)
                    && System.nanoTime() < deathDeadline) Thread.sleep(25L);
            assertFalse(ProcessHandle.of(ownedPid).map(ProcessHandle::isAlive).orElse(false));
        }
    }
}
