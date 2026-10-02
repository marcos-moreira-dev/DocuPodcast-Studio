package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Physical MathCAT certification; the rules remain bundled and offline. */
final class MathCatEngineAdministration extends AbstractLocalEngineAdministration {
    private static final String SAMPLE_MATHML = """
            <math xmlns="http://www.w3.org/1998/Math/MathML">
              <mrow><msup><mi>x</mi><mn>2</mn></msup><mo>+</mo><mn>1</mn></mrow>
            </math>
            """;
    private final EngineCertificationStore certifications;

    MathCatEngineAdministration(MathCatSpeechEngine engine, RuntimeAssetCatalog assets,
                                EngineCertificationStore certifications) {
        super(engine, assets, List.of(action(EngineActionId.SMOKE_TEST,
                "Probar lectura matemática",
                "Convierte una fórmula MathML controlada a habla local.", false)));
        this.certifications = java.util.Objects.requireNonNullElse(
                certifications, EngineCertificationStore.none());
    }

    @Override
    protected List<GenerationArtifact> perform(EngineActionRequest request,
                                               ExecutionContext context)
            throws IOException, InterruptedException {
        if (!EngineActionId.SMOKE_TEST.equals(request.actionId())) return List.of();
        ContentAnalysisResult result = ((MathCatSpeechEngine) engine).analyze(
                new ContentAnalysisRequest(ContentAnalysisOperation.MATH_SPEECH,
                        List.of(), "Lee la fórmula.", "", "es", "",
                        Map.of("mathMl", SAMPLE_MATHML)), context);
        if (result.text().isBlank() || !result.text().matches(".*[xX].*")) {
            throw new EngineExecutionException(EngineDiagnosticCode.INVALID_OUTPUT,
                    "MathCAT no produjo una lectura comprobable.");
        }
        certifications.save(new EngineCertificationRecord(MathCatSpeechEngine.ID,
                "0.7.2", "MathCAT",
                EngineHardwareFingerprint.current(context.computePreference()),
                "mathcat-real-formula-smoke", Instant.now(), false,
                Map.of("spokenText", result.text(), "language", "es")));
        return List.of();
    }
}
