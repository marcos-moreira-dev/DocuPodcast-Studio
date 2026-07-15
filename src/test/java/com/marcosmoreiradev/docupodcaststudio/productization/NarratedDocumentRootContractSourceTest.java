package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarratedDocumentRootContractSourceTest {
    @Test
    void narratedDocumentIsDeclaredAsV1RootAndScriptIsInternalProjection() throws Exception {
        String contract = read("docs/productizacion/CONTRATO_DOCUMENTO_NARRABLE_RAIZ_V1.md");
        String tanda = read("docs/86_TANDA_60_AUDITORIA_CEREBRO_DOCUMENTO_RAIZ.md");
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");

        assertTrue(contract.contains("El objeto padre es el **Documento narrable**"));
        assertTrue(contract.contains("guion no debe presentarse como un segundo producto padre obligatorio"));
        assertTrue(contract.contains("proyección interna o avanzada de narración"));
        assertTrue(contract.contains("Abrir documento → leer/escuchar → ajustar capas opcionales → exportar"));
        assertTrue(contract.contains("la imagen dura lo que dure la lectura hablada del texto asociado"));
        assertTrue(contract.contains("Word/DOCX"));
        assertTrue(contract.contains("PDF"));
        assertTrue(contract.contains("Markdown/MD"));
        assertTrue(contract.contains("TXT"));

        assertTrue(tanda.contains("Documento narrable raíz"));
        assertTrue(tanda.contains("El guion sigue existiendo como proyección interna/avanzada"));

        assertTrue(readme.contains("Documento narrable"));
        assertTrue(handoff.contains("Documento narrable como raíz V1"));
        assertFalse(readme.contains("Word/DOCX → documento narrable → guion"),
                "El README no debe presentar guion como paso obligatorio posterior al documento.");
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
