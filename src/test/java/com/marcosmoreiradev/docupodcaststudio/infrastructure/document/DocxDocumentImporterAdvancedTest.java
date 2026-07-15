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

import static org.junit.jupiter.api.Assertions.*;

class DocxDocumentImporterAdvancedTest {
    @TempDir
    Path tempDir;

    @Test
    void preservesBodyOrderDetectsListItemsAndEmitsWarnings() throws Exception {
        Path docx = tempDir.resolve("notas-avanzadas.docx");
        writeMinimalDocx(docx,
                """
                <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                <w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">
                  <w:style w:styleId=\"Ttulo1\"><w:name w:val=\"Título 1\"/></w:style>
                </w:styles>
                """,
                """
                <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                            xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\">
                  <w:body>
                    <w:p><w:pPr><w:pStyle w:val=\"Ttulo1\"/></w:pPr><w:r><w:t>Capítulo 1</w:t></w:r></w:p>
                    <w:p><w:pPr><w:numPr><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Primer punto</w:t></w:r></w:p>
                    <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Celda A</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
                    <w:p><w:drawing><wp:inline><wp:docPr id=\"1\" /></wp:inline></w:drawing></w:p>
                  </w:body>
                </w:document>
                """);

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        assertEquals(DocumentBlockType.HEADING, document.blocks().get(0).type());
        assertEquals(DocumentBlockType.LIST_ITEM, document.blocks().get(1).type());
        assertEquals(DocumentBlockType.TABLE_NOTICE, document.blocks().get(2).type());
        assertEquals(DocumentBlockType.IMAGE_NOTICE, document.blocks().get(3).type());
        assertTrue(document.issues().stream().anyMatch(issue -> issue.message().contains("Tabla detectada")));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.message().contains("Imagen sin descripción")));
    }

    @Test
    void detectsWordMathAsNonNarratableSourceVisualBlock() throws Exception {
        Path docx = tempDir.resolve("formula.docx");
        writeMinimalDocx(docx,
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>
                """,
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                            xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math">
                  <w:body>
                    <w:p>
                      <m:oMathPara><m:oMath><m:r><m:t>x + y = z</m:t></m:r></m:oMath></m:oMathPara>
                    </w:p>
                  </w:body>
                </w:document>
                """);

        ReadableDocument document = new DocxDocumentImporter().importDocument(docx);

        var math = document.blocks().stream()
                .filter(block -> block.type() == DocumentBlockType.MATH_NOTICE)
                .findFirst()
                .orElseThrow();
        assertFalse(math.narratable());
        assertEquals("true", math.metadata().get("visualBlock"));
        assertEquals("identified-only", math.metadata().get("renderPolicy"));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("MATH_BLOCK_DETECTED")));
    }

    private static void writeMinimalDocx(Path target, String stylesXml, String documentXml) throws IOException {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("word/styles.xml"));
            zip.write(stylesXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
