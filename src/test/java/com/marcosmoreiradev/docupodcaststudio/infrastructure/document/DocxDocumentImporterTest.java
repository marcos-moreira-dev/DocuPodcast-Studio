package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocxDocumentImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsParagraphsHeadingsListsImagesAndTablesInDocumentOrder() throws Exception {
        Path docx = tempDir.resolve("notas.docx");
        writeDocx(docx, Map.of(
                "word/styles.xml", """
                        <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                        <w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">
                          <w:style w:type=\"paragraph\" w:styleId=\"Ttulo1\"><w:name w:val=\"Título 1\"/></w:style>
                          <w:style w:type=\"paragraph\" w:styleId=\"SubTema\"><w:name w:val=\"Título 2\"/></w:style>
                        </w:styles>
                        """,
                "docProps/core.xml", """
                        <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                        <cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">
                          <dc:title>Notas de dinosaurios</dc:title>
                        </cp:coreProperties>
                        """,
                "word/document.xml", """
                        <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                        <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                                    xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\">
                          <w:body>
                            <w:p><w:pPr><w:pStyle w:val=\"Ttulo1\"/></w:pPr><w:r><w:t>Introducción</w:t></w:r></w:p>
                            <w:p><w:r><w:t>Este es un párrafo de prueba.</w:t></w:r></w:p>
                            <w:p><w:pPr><w:numPr><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Primer punto de lista</w:t></w:r></w:p>
                            <w:p><w:drawing><wp:inline><wp:docPr id=\"1\" name=\"Imagen 1\" descr=\"Dinosaurio en el museo\"/></wp:inline></w:drawing></w:p>
                            <w:p><w:drawing><wp:inline><wp:docPr id=\"2\" name=\"Imagen 2\"/></wp:inline></w:drawing></w:p>
                            <w:tbl>
                              <w:tr><w:tc><w:p><w:r><w:t>Columna A</w:t></w:r></w:p></w:tc></w:tr>
                              <w:tr><w:tc><w:p><w:r><w:t>Valor A</w:t></w:r></w:p></w:tc></w:tr>
                            </w:tbl>
                          </w:body>
                        </w:document>
                        """,
                "word/media/image1.png", "fake-png-bytes"
        ));

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        assertEquals("Notas de dinosaurios", document.title());
        assertEquals(DocumentBlockType.HEADING, document.blocks().get(0).type());
        assertTrue(document.blocks().stream().anyMatch(block -> block.text().contains("párrafo de prueba")));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.LIST_ITEM));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.IMAGE_NOTICE && block.text().contains("Dinosaurio")));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.IMAGE_NOTICE
                && block.metadata().containsKey("embeddedImageBase64")
                && block.metadata().containsKey("embeddedImageMimeType")));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.IMAGE_NOTICE && block.text().contains("sin descripción")));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.TABLE_NOTICE && block.text().contains("2 filas")));
        var table = document.blocks().stream().filter(block -> block.type() == DocumentBlockType.TABLE_NOTICE).findFirst().orElseThrow();
        assertEquals("2", table.metadata().get("table.rowCount"));
        assertEquals("1", table.metadata().get("table.columnCount"));
        assertEquals("Columna A", table.metadata().get("table.header.0"));
        assertEquals("Valor A", table.metadata().get("table.cell.1.0"));
        assertTrue(document.importReport().hasWarnings());
        assertTrue(document.importReport().issues().stream().anyMatch(issue -> issue.code().equals("IMAGE_WITHOUT_DESCRIPTION")));
    }

    @Test
    void reportsNoHeadingsWhenDocumentLooksLikeFlatNotes() throws Exception {
        Path docx = tempDir.resolve("plano.docx");
        StringBuilder body = new StringBuilder();
        for (int i = 1; i <= 7; i++) {
            body.append("<w:p><w:r><w:t>Párrafo ").append(i).append("</w:t></w:r></w:p>");
        }
        writeDocx(docx, Map.of("word/document.xml", """
                <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>%s</w:body></w:document>
                """.formatted(body)));

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        assertTrue(document.importReport().issues().stream().anyMatch(issue -> issue.code().equals("DOCX_NO_HEADINGS")));
    }

    private static void writeDocx(Path target, Map<String, String> entries) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
