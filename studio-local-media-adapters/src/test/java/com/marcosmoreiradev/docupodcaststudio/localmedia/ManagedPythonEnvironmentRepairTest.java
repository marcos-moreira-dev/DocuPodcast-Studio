package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedPythonEnvironmentRepairTest {
    @TempDir Path temporary;

    @Test
    void relocatesEnvironmentMetadataToManagedPythonWithoutCopyingTheEnvironment() throws Exception {
        Path managedHome = temporary.resolve("tools/python/python-3.10/tools");
        Files.createDirectories(managedHome);
        Files.write(managedHome.resolve("python.exe"), new byte[]{1});
        Path environment = temporary.resolve("tools/voice/.venv");
        Path environmentPython = environment.resolve("Scripts/python.exe");
        Files.createDirectories(environmentPython.getParent());
        Files.write(environmentPython, new byte[]{1});
        Path configuration = environment.resolve("pyvenv.cfg");
        Files.writeString(configuration,
                "home = C:\\Users\\someone\\Downloads\\old-copy\\tools\\python\n"
                        + "include-system-site-packages = false\nversion = 3.10.11\n",
                StandardCharsets.UTF_8);

        String report = ManagedPythonEnvironmentRepair.repair(
                temporary, environmentPython, "voz de prueba");

        String repaired = Files.readString(configuration, StandardCharsets.UTF_8);
        assertTrue(report.contains("Se reparó"));
        assertTrue(repaired.contains("home = " + managedHome));
        assertFalse(repaired.contains("old-copy"));
        assertFalse(Files.exists(configuration.resolveSibling("pyvenv.cfg.staging")));
    }
}
