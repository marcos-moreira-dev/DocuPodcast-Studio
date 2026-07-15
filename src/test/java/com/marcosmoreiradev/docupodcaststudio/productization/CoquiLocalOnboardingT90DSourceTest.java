package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CoquiLocalOnboardingT90DSourceTest {
    @Test
    void onboardingScriptsCreateAndVerifyOnlyLocalRuntime() throws Exception {
        String setup = Files.readString(Path.of("scripts/tts/setup-xtts-portable-python.ps1"));
        String verify = Files.readString(Path.of("scripts/22-verificar-coqui-xtts-local.bat"));
        String tryVoice = Files.readString(Path.of("scripts/21-probar-coqui-xtts.bat"));
        assertTrue(setup.contains("tools\\python"));
        assertTrue(setup.contains("tools\\xtts-wrapper\\.venv"));
        assertTrue(setup.contains("nuget.org/api/v2/package/python"));
        assertTrue(verify.contains("tools\\xtts-wrapper\\.venv\\Scripts\\python.exe"));
        assertTrue(tryVoice.contains("tools\\xtts-wrapper\\.venv\\Scripts\\python.exe"));
    }

    @Test
    void t90dDocumentsSelfContainedCoquiSetup() throws Exception {
        assertTrue(Files.isRegularFile(Path.of("docs/productizacion/T90D_ONBOARDING_COQUI_XTTS_LOCAL.md")));
        String doc = Files.readString(Path.of("docs/productizacion/T90D_ONBOARDING_COQUI_XTTS_LOCAL.md"));
        assertTrue(doc.contains("scripts\\20-preparar-python-portable-coqui.bat"));
        assertTrue(doc.contains("scripts\\21-probar-coqui-xtts.bat"));
        assertTrue(doc.contains("sin Python global"));
    }
}
