package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Import-only administration: Paddle is never allowed to download models behind the UI. */
final class PpStructureV3Administration extends AbstractLocalEngineAdministration {
    private static final String RUNTIME_DIRECTORY = "tools/document-ai/pp-structure";
    private final EngineCertificationStore certifications;

    PpStructureV3Administration(PpStructureV3Engine engine, RuntimeAssetCatalog assets) {
        this(engine, assets, EngineCertificationStore.none());
    }

    PpStructureV3Administration(PpStructureV3Engine engine, RuntimeAssetCatalog assets,
                                EngineCertificationStore certifications) {
        super(engine, assets, List.of(
                action(EngineActionId.IMPORT, "Importar análisis avanzado de contenido",
                        "Importa un paquete local preparado con runtime, manifest y modelos PP-StructureV3 reutilizable por cualquier módulo.",
                        true, directory("runtimeDirectory", "Paquete PP-StructureV3", true)),
                action(EngineActionId.SMOKE_TEST, "Probar análisis avanzado",
                        "Ejecuta layout real sobre una imagen de página.", false,
                        file("testImage", "Página de prueba", true))));
        this.certifications = java.util.Objects.requireNonNullElse(
                certifications, EngineCertificationStore.none());
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.IMPORT.equals(request.actionId())) {
            Path source = inputPath(request, "runtimeDirectory");
            verify(source);
            runtime.importDirectory(source, RUNTIME_DIRECTORY, context);
            verify(runtime.target(RUNTIME_DIRECTORY));
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            PpStructureV3Engine pp = (PpStructureV3Engine) engine;
            ContentAnalysisResult result = pp.analyze(new ContentAnalysisRequest(
                    ContentAnalysisOperation.LAYOUT_ANALYSIS,
                    List.of(new AnalysisVisualInput(inputPath(request, "testImage"), "page", "")),
                    "Analiza el layout.", "", "und", "", java.util.Map.of()), context);
            if (result.structuredJson().isBlank()) {
                throw new EngineExecutionException(EngineDiagnosticCode.INVALID_OUTPUT,
                        "PP-StructureV3 no produjo geometría verificable.");
            }
            String model = "pp-structure-v3:" + pp.verificationProfile();
            certifications.save(new EngineCertificationRecord(
                    PpStructureV3Engine.ID,
                    PpStructurePackageManifest.PADDLE_OCR_VERSION,
                    model, EngineHardwareFingerprint.current(context.computePreference()),
                    "pp-structure-real-layout-smoke", java.time.Instant.now(), true,
                    result.diagnostics()));
        }
        return List.of();
    }

    private static void verify(Path root) throws IOException {
        PpStructurePackageManifest.Verification verification =
                PpStructurePackageManifest.verify(root);
        if (!verification.valid()) {
            throw new EngineExecutionException(EngineDiagnosticCode.INVALID_RESOURCE,
                    "El paquete PP-StructureV3 no es portable o está incompleto.",
                    java.util.Map.of("root", root.toAbsolutePath().normalize().toString(),
                            "issues", String.join(", ", verification.issues())));
        }
    }
}
