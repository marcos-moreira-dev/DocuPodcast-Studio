package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsDownloadRobustnessHf9SourceTest {
    @Test
    void xttsDownloadUsesHttpHeadersAndFallbackMove() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadXttsOfficialModelUseCase.java"));
        assertTrue(source.contains("User-Agent"),
                "La descarga de Voz IA avanzada debe identificarse ante el servidor externo.");
        assertTrue(source.contains("Accept"),
                "La descarga debe pedir binarios sin depender de defaults del cliente HTTP.");
        assertTrue(source.contains("moveDownloadedFile"),
                "El movimiento del .download debe tener fallback cuando ATOMIC_MOVE no esta soportado en Windows/FS.");
    }
}
