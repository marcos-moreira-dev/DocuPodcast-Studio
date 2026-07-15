package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxDocumentImporterDiagnosticsTest {
    @TempDir
    Path tempDir;

    @Test
    void preservesBodyOrderForParagraphsTablesAndLists() throws Exception {
        Path docx = tempDir.resolve("orden.docx");
        writeMinimalDocx(docx, """
                <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>Primero</w:t></w:r></w:p>
                    <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Celda A</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
                    <w:p><w:pPr><w:numPr><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Elemento de lista</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """);

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        assertEquals(DocumentBlockType.HEADING, document.blocks().get(0).type());
        assertEquals(DocumentBlockType.TABLE_NOTICE, document.blocks().get(1).type());
        assertEquals(DocumentBlockType.LIST_ITEM, document.blocks().get(2).type());
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("TABLE_DETECTED")));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("LIST_ITEM_DETECTED") || issue.message().contains("lista")));
    }

    @Test
    void warnsWhenImageHasNoDescriptionAndWhenNoHeadingsExist() throws Exception {
        Path docx = tempDir.resolve("imagen.docx");
        writeMinimalDocx(docx, """
                <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                            xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\">
                  <w:body>
                    <w:p><w:r><w:t>Párrafo normal.</w:t></w:r></w:p>
                    <w:p><w:drawing><wp:inline><wp:docPr id=\"1\" name=\"Imagen sin alt\"/></wp:inline></w:drawing></w:p>
                  </w:body>
                </w:document>
                """);

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("IMAGE_WITHOUT_DESCRIPTION")));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("DOCX_NO_HEADINGS")));
    }

    private static void writeMinimalDocx(Path target, String documentXml) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
