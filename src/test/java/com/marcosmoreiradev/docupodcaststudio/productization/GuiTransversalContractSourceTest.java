package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiTransversalContractSourceTest {
    @Test
    void productDocumentationDeclaresSharedGuiComponentContract() throws Exception {
        String contract = read("docs/productizacion/CONTRATO_COMPONENTES_GUI_TRANSVERSALES.md");
        String tanda = read("docs/82_TANDA_59A_COMPONENTES_GUI_TRANSVERSALES.md");
        assertTrue(contract.contains("ActionButtonFactory"));
        assertTrue(contract.contains("Workspace = intención de usuario"));
        assertTrue(contract.contains("no debe construir botoneras repetidas"));
        assertTrue(tanda.contains("Componentes GUI transversales"));
        assertTrue(tanda.contains("T59B debe limpiar la pantalla Documento"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
