package com.marcosmoreiradev.docupodcaststudio.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleasePackagingScriptsSourceTest {
    @Test
    void releaseScriptsExistAndResolveProjectRoot() throws Exception {
        assertRootSafe("scripts/13-revalidacion-local-completa.bat");
        assertRootSafe("scripts/14-app-image-completa.bat");
        assertRootSafe("scripts/15-msi-completo.bat");
        assertRootSafe("scripts/16-release-candidate.bat");
        assertRootSafe("scripts/31-generar-javadoc.bat");
    }

    @Test
    void testScriptReportsSuccessWhenMavenPasses() throws Exception {
        String script = Files.readString(Path.of("scripts/02-ejecutar-tests.bat"));
        assertTrue(script.contains("call mvn test"), "El script de tests debe ejecutar Maven desde la raiz.");
        assertTrue(script.contains("Tests OK"), "El script de tests debe mostrar una confirmacion visible si no hay fallos.");
        assertTrue(script.contains("target\\surefire-reports"), "El script debe orientar al reporte de Surefire si falla.");
    }

    @Test
    void packagingScriptsUseJpackageAndCreateManifests() throws Exception {
        String appImage = Files.readString(Path.of("scripts/14-app-image-completa.bat"));
        String msi = Files.readString(Path.of("scripts/15-msi-completo.bat"));
        String rc = Files.readString(Path.of("scripts/16-release-candidate.bat"));

        assertTrue(appImage.contains("jpackage"));
        assertTrue(appImage.contains("APP_IMAGE_MANIFEST.txt"));
        assertTrue(msi.contains("--type msi"));
        assertTrue(msi.contains("MSI_MANIFEST.txt"));
        assertTrue(rc.contains("RELEASE_CANDIDATE_MANIFEST.txt"));
        assertTrue(rc.contains("13-revalidacion-local-completa.bat"));
    }

    private static void assertRootSafe(String scriptPath) throws Exception {
        String script = Files.readString(Path.of(scriptPath));
        assertTrue(Files.exists(Path.of(scriptPath)), scriptPath + " debe existir.");
        assertTrue(script.contains("set \"SCRIPT_DIR=%~dp0\""), scriptPath + " debe resolver su carpeta.");
        assertTrue(script.contains("pushd \"%SCRIPT_DIR%..\""), scriptPath + " debe moverse a la raiz del repo.");
    }
}
