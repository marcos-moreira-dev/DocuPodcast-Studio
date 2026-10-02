package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
final class WindowsJobObjectAbruptExitTest {
    @TempDir Path root;

    @Test
    void abruptParentExitKillsContainedChild() throws Exception {
        Path pidFile = root.resolve("child.pid");
        String javaExecutable = Path.of(
                System.getProperty("java.home"), "bin", "java.exe").toString();
        Process helper = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"),
                WindowsJobObjectAbruptExitHelper.class.getName(), pidFile.toString()).start();
        assertTrue(helper.waitFor(20, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(Files.isRegularFile(pidFile));
        long childPid = Long.parseLong(Files.readString(pidFile, StandardCharsets.UTF_8).strip());
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false)
                && System.nanoTime() < deadline) {
            Thread.sleep(100L);
        }
        assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false),
                "KILL_ON_JOB_CLOSE must terminate the managed child after abrupt parent exit");
    }
}
