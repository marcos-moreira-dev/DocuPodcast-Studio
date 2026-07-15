package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocxVisualBlocksTi4Test {
    @TempDir
    Path tempDir;

    @Test
    void detectsTablesAndMathAsNonNarratableSourceVisualBlocks() throws Exception {
        Path docx = tempDir.resolve("visuales.docx");
        writeMinimalDocx(docx, """
                <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
                <w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"
                            xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\">
                  <w:body>
                    <w:p><w:r><w:t>Texto narrable.</w:t></w:r></w:p>
                    <w:tbl><w:tr><w:tc><w:p><w:r><w:t>Ventas</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
                    <w:p><m:oMath><m:r><m:t>x + y = z</m:t></m:r></m:oMath></w:p>
                  </w:body>
                </w:document>
                """);

        var document = new DocxDocumentImporter().importDocument(docx);

        assertEquals(1, document.narratableBlockCount());
        assertEquals(1, document.tableNoticeCount());
        assertEquals(1, document.mathNoticeCount());
        assertEquals(2, document.sourceVisualBlockCount());
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.TABLE_NOTICE && !block.narratable()));
        assertTrue(document.blocks().stream().anyMatch(block -> block.type() == DocumentBlockType.MATH_NOTICE && !block.narratable()));
        assertTrue(document.issues().stream().anyMatch(issue -> issue.code().equals("MATH_BLOCK_DETECTED")));
        assertFalse(document.blocks().stream().filter(block -> block.type() == DocumentBlockType.MATH_NOTICE)
                .findFirst().orElseThrow().text().isBlank());
    }

    private static void writeMinimalDocx(Path target, String documentXml) throws Exception {
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
