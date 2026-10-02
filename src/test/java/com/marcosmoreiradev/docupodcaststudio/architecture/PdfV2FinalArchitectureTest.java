package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Final guardrails preventing the removed PDF-to-block compatibility layer from returning. */
final class PdfV2FinalArchitectureTest {
    private static final List<String> FORBIDDEN = List.of(
            "BuildPreparedPdfDocumentProjectionUseCase",
            "BuildPdfResolvedTextLayerUseCase",
            "ResolvePdfNarratableDocumentUseCase",
            "PdfDocumentImporter",
            "\"pdfV2Region\"",
            "\"pdfNarratability\"",
            "\"extractionMode\""
    );

    @Test
    void mainSourcesContainNoRemovedPdfProjectionOrBlockMetadataContract() throws Exception {
        Path root = Path.of("src/main/java").toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                for (String forbidden : FORBIDDEN) {
                    if (source.contains(forbidden)) {
                        violations.add(root.relativize(path) + " contiene " + forbidden);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    @Test
    void onlyCompositionAndSchedulerOwnTheUnitPreparationPipeline() throws Exception {
        Path root = Path.of("src/main/java").toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String normalized = path.toString().replace('\\', '/');
                if (normalized.endsWith("/PreparePdfPageUseCase.java")
                        || normalized.endsWith("/PdfPagePreparationScheduler.java")
                        || normalized.endsWith("/WorkspaceCompositionFactory.java")) {
                    continue;
                }
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (source.contains("preparePdfPage.execute(")
                        || source.contains("preparePdfPage::execute")) {
                    violations.add(root.relativize(path).toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "Preparación unitaria expuesta fuera del scheduler: " + violations);
    }

    @Test
    void pdfConsumersCannotImportBlockDocumentContracts() throws Exception {
        Path root = Path.of("src/main/java").toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String fileName = path.getFileName().toString();
                if (!fileName.toLowerCase(java.util.Locale.ROOT).contains("pdf")) continue;
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (source.contains("import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;")
                        || source.contains("import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;")) {
                    violations.add(root.relativize(path).toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "Consumidores PDF acoplados a documentos por bloques: " + violations);
    }

    @Test
    void localAiAdaptersAreTransversalAndCannotImportPdfDomainTypes() throws Exception {
        Path root = Path.of("studio-local-media-adapters/src/main/java")
                .toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (source.contains("domain.document.pdf")
                        || source.contains("application.document.Pdf")) {
                    violations.add(root.relativize(path).toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "Adaptadores IA acoplados a PDF: " + violations);
    }

    @Test
    void transversalAiApiDoesNotExposeDocumentAnalysisContracts() throws Exception {
        Path root = Path.of("studio-media-api/src/main/java")
                .toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (source.contains("DocumentAnalysisEngine")
                        || source.contains("DocumentAnalysisRequest")) {
                    violations.add(root.relativize(path).toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "La API transversal conserva contratos acoplados: " + violations);
    }

    @Test
    void transversalAiCapabilityIdsAreNotNamedAfterPdfOrDocuments() throws Exception {
        Path root = Path.of("studio-media-api/src/main/java")
                .toAbsolutePath().normalize();
        ArrayList<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                if (source.contains("\"document-layout-analysis\"")
                        || source.contains("\"document-math-recognition\"")
                        || source.contains("\"document-image-description\"")
                        || source.contains("\"document-context-correction\"")
                        || source.contains("\"pdf-")) {
                    violations.add(root.relativize(path).toString());
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "La API IA transversal conserva nombres acoplados a PDF/documentos: "
                        + violations);
    }
}
