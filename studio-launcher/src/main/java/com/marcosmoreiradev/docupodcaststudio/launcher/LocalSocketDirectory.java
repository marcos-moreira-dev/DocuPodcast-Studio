package com.marcosmoreiradev.docupodcaststudio.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configures JDK selector sockets before any HTTP client initializes NIO. */
final class LocalSocketDirectory {
    static final String PROPERTY = "jdk.net.unixdomain.tmpdir";

    private LocalSocketDirectory() { }

    static void configure(Properties properties, Path runtimeRoot) throws IOException {
        if (!properties.getProperty("os.name", "").startsWith("Windows")
                || properties.containsKey(PROPERTY)) {
            return;
        }
        // Some Windows TEMP locations accept bind but reject AF_UNIX connect.
        // Keep these ephemeral sockets in our writable runtime instead. The JDK
        // retains its TCP fallback when the filesystem cannot bind AF_UNIX.
        Path directory = runtimeRoot.resolve("runtime/ipc").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        properties.setProperty(PROPERTY, directory.toString());
    }
}
