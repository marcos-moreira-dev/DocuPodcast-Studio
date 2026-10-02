package com.marcosmoreiradev.docupodcaststudio.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

final class LocalSocketDirectoryTest {
    @TempDir Path temporary;

    @Test void windowsUsesItsOwnRuntimeDirectory() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("os.name", "Windows 11");
        Path root = temporary.resolve("runtime with spaces");
        LocalSocketDirectory.configure(properties, root);
        Path directory = root.resolve("runtime/ipc").toAbsolutePath().normalize();
        assertEquals(directory.toString(), properties.getProperty(LocalSocketDirectory.PROPERTY));
        assertTrue(Files.isDirectory(directory));
    }

    @Test void explicitOverrideIsPreserved() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("os.name", "Windows 11");
        properties.setProperty(LocalSocketDirectory.PROPERTY, "custom");
        LocalSocketDirectory.configure(properties, temporary);
        assertEquals("custom", properties.getProperty(LocalSocketDirectory.PROPERTY));
        assertFalse(Files.exists(temporary.resolve("runtime")));
    }

    @Test void otherPlatformsAreUnchanged() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("os.name", "Linux");
        LocalSocketDirectory.configure(properties, temporary);
        assertNull(properties.getProperty(LocalSocketDirectory.PROPERTY));
        assertFalse(Files.exists(temporary.resolve("runtime")));
    }
}
