package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Auxiliary JVM used by the abrupt-parent integration test. */
public final class WindowsJobObjectAbruptExitHelper {
    private WindowsJobObjectAbruptExitHelper() { }

    public static void main(String[] arguments) throws Exception {
        Process child = new ProcessBuilder("cmd", "/c",
                "ping 127.0.0.1 -n 120 > nul").start();
        ManagedProcessContainment containment = ManagedProcessContainment.create();
        if (!containment.attach(child)) {
            child.destroyForcibly();
            throw new IllegalStateException(containment.diagnostics());
        }
        Files.writeString(Path.of(arguments[0]), Long.toString(child.pid()),
                StandardCharsets.UTF_8);
        Runtime.getRuntime().halt(0);
    }
}
