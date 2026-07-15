package com.marcosmoreiradev.docupodcaststudio.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptsRootSafeSourceTest {
    @Test
    void mavenScriptsResolveRepositoryRootBeforeRunningMaven() throws Exception {
        assertRootSafeMavenScript("scripts/00-verificar-entorno.bat");
        assertRootSafeMavenScript("scripts/01-ejecutar-app.bat");
        assertRootSafeMavenScript("scripts/02-ejecutar-tests.bat");
        assertRootSafeMavenScript("scripts/03-verificar-toolchain.bat");
    }

    @Test
    void scriptsCallMavenBatWithoutStealingBatchControlFlow() throws Exception {
        assertUsesCallForMaven("scripts/00-verificar-entorno.bat");
        assertUsesCallForMaven("scripts/01-ejecutar-app.bat");
        assertUsesCallForMaven("scripts/02-ejecutar-tests.bat");
        assertUsesCallForMaven("scripts/03-verificar-toolchain.bat");
    }

    @Test
    void powershellScriptsRemainAsciiSafeForWindowsPowerShell51() throws Exception {
        try (Stream<Path> scripts = Files.walk(Path.of("scripts"))) {
            for (Path scriptPath : scripts
                    .filter(path -> path.toString().endsWith(".ps1"))
                    .toList()) {
                String script = Files.readString(scriptPath);
                for (int index = 0; index < script.length(); index++) {
                    char value = script.charAt(index);
                    assertTrue(value <= 127,
                            scriptPath + " debe ser ASCII-safe para evitar parseos corruptos en Windows PowerShell 5.1 sin BOM.");
                }
            }
        }
    }

    @Test
    void powershellScriptsUseSingleQuotedMarkdownFences() throws Exception {
        try (Stream<Path> scripts = Files.walk(Path.of("scripts"))) {
            for (Path scriptPath : scripts
                    .filter(path -> path.toString().endsWith(".ps1"))
                    .toList()) {
                String script = Files.readString(scriptPath);
                assertFalse(script.contains("\"```"),
                        scriptPath + " no debe usar cercas Markdown con comillas dobles; el backtick es caracter de escape en PowerShell 5.1.");
            }
        }
    }

    private static void assertRootSafeMavenScript(String scriptPath) throws Exception {
        String script = Files.readString(Path.of(scriptPath));
        assertTrue(script.contains("set \"SCRIPT_DIR=%~dp0\""), scriptPath + " debe resolver su propia carpeta.");
        assertTrue(script.contains("pushd \"%SCRIPT_DIR%..\""), scriptPath + " debe moverse a la raiz del repo antes de invocar Maven.");
    }

    private static void assertUsesCallForMaven(String scriptPath) throws Exception {
        String script = Files.readString(Path.of(scriptPath));
        assertTrue(script.contains("call mvn"), scriptPath + " debe invocar mvn con call para que el .bat continue.");
    }
}
