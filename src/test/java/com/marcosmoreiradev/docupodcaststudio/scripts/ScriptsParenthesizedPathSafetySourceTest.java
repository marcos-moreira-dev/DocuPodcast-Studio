package com.marcosmoreiradev.docupodcaststudio.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScriptsParenthesizedPathSafetySourceTest {
    private static final List<String> SCRIPTS = List.of(
            "scripts/00-verificar-entorno.bat",
            "scripts/01-ejecutar-app.bat",
            "scripts/02-ejecutar-tests.bat",
            "scripts/03-verificar-toolchain.bat",
            "scripts/04-verificar-tts-config.bat",
            "scripts/13-revalidacion-local-completa.bat",
            "scripts/14-app-image-completa.bat",
            "scripts/15-msi-completo.bat",
            "scripts/16-release-candidate.bat",
            "scripts/17-smoke-exploratorio-minimo.bat",
            "scripts/31-generar-javadoc.bat"
    );

    @Test
    void scriptsAvoidPercentExpandedScriptDirInsideParenthesizedErrorBlocks() throws Exception {
        for (String scriptPath : SCRIPTS) {
            String script = Files.readString(Path.of(scriptPath));
            assertTrue(script.contains("setlocal EnableExtensions EnableDelayedExpansion"),
                    scriptPath + " debe habilitar delayed expansion para rutas con parentesis como carpeta(1).");
            assertTrue(script.contains("echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!.."),
                    scriptPath + " debe usar !SCRIPT_DIR! en el bloque if errorlevel para evitar romper CMD con parentesis.");
            assertFalse(script.contains("echo ERROR: No se pudo resolver la raiz del proyecto desde %SCRIPT_DIR%.."),
                    scriptPath + " no debe expandir %SCRIPT_DIR% dentro de bloques parentizados.");
        }
    }
}
