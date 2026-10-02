package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngine;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfigurationSchema;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineCertificationStore;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import onl.mdw.mathcat4j.api.MathCatLoader;
import onl.mdw.mathcat4j.api.MathCatManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Transversal deterministic MathML-to-speech engine backed by the bundled MathCAT runtime. */
final class MathCatSpeechEngine implements ContentAnalysisEngine {
    static final EngineId ID = new EngineId("mathcat-local");
    private final MathCatRulesMaterializer rules;
    private final EngineCertificationStore certifications;
    private volatile MathCatManager manager;

    MathCatSpeechEngine(java.nio.file.Path runtimeRoot) {
        this(runtimeRoot, EngineCertificationStore.none());
    }

    MathCatSpeechEngine(java.nio.file.Path runtimeRoot,
                        EngineCertificationStore certifications) {
        rules = new MathCatRulesMaterializer(runtimeRoot);
        this.certifications = java.util.Objects.requireNonNullElse(
                certifications, EngineCertificationStore.none());
    }

    @Override
    public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.MATH_SPEECH,
                "Lectura matemática local", "MathCAT 0.7.2", "embedded-jni",
                Set.of(new EngineFeature("offline-execution")), false);
    }

    @Override
    public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of());
    }

    @Override
    public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
        try {
            manager();
            String hardware = EngineHardwareFingerprint.current(
                    com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference.automatic());
            if (certifications.find(ID, "MathCAT")
                    .filter(record -> record.matches("0.7.2", "MathCAT", hardware, false))
                    .isEmpty()) {
                return new EngineReadiness(ID,
                        com.marcosmoreiradev.docupodcaststudio.media.api.ReadinessState.DEGRADED,
                        "MathCAT está instalado, pero necesita una prueba de lectura.",
                        List.of("Falta certificación física vigente."),
                        List.of("Probar lectura matemática desde Motores y dependencias."),
                        "hardware=" + hardware);
            }
            return EngineReadiness.ready(ID, "MathCAT está listo y certificado para narrar MathML.");
        } catch (RuntimeException | IOException failure) {
            return new EngineReadiness(ID,
                    com.marcosmoreiradev.docupodcaststudio.media.api.ReadinessState.UNAVAILABLE,
                    "La lectura matemática integrada necesita reparación.",
                    List.of(failure.getMessage() == null
                            ? failure.getClass().getSimpleName() : failure.getMessage()),
                    List.of("Reparar la instalación de DocuPodcast Studio."),
                    failure.toString());
        }
    }

    @Override
    public Set<ContentAnalysisOperation> operations() {
        return Set.of(ContentAnalysisOperation.MATH_SPEECH);
    }

    @Override
    public ContentAnalysisResult analyze(ContentAnalysisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (request.operation() != ContentAnalysisOperation.MATH_SPEECH) {
            throw new IllegalArgumentException("MathCAT no implementa " + request.operation());
        }
        String mathMl = request.options().getOrDefault("mathMl", "").strip();
        if (mathMl.isBlank()) {
            throw new IOException("La lectura matemática requiere MathML validado.");
        }
        validateMathMl(mathMl);
        String language = normalizedLanguage(request.language());
        context.cancellation().throwIfCancellationRequested();
        context.progress().report("MATH_SPEECH", 0.2, "Preparando lectura matemática local.");
        String spoken;
        try {
            spoken = manager().run(mathCat -> {
                mathCat.setPreference("Language", language);
                mathCat.setPreference("SpeechStyle",
                        request.options().getOrDefault("speechStyle", "ClearSpeak"));
                mathCat.setMathml(mathMl);
                return mathCat.getSpokenText();
            });
        } catch (RuntimeException failure) {
            throw new IOException("MathCAT no pudo convertir la fórmula a habla.", failure);
        }
        if (spoken == null || spoken.isBlank()) {
            throw new IOException("MathCAT devolvió una lectura vacía.");
        }
        context.progress().report("READY", 1.0, "Lectura matemática local terminada.");
        return new ContentAnalysisResult(spoken, "",
                1.0, List.of(), Map.of(
                "engineId", ID.value(),
                "model", "MathCAT",
                "language", language,
                "local", "true"));
    }

    private MathCatManager manager() throws IOException {
        MathCatManager current = manager;
        if (current != null) return current;
        synchronized (this) {
            if (manager != null) return manager;
            java.nio.file.Path rulesDirectory = rules.ensureRules();
            System.setProperty("onl.mdw.mathcat4j.rulesDir", rulesDirectory.toString());
            manager = MathCatLoader.INSTANCE.getMathCatFactory()
                    .orElseThrow(() -> new IOException(
                            "No se encontró la implementación JNI integrada de MathCAT."))
                    .create();
            return manager;
        }
    }

    private static String normalizedLanguage(String language) {
        String value = language == null ? "" : language.strip().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("es") ? "es" : "en";
    }

    private static void validateMathMl(String mathMl) throws IOException {
        if (mathMl.length() > 1_000_000 || mathMl.contains("<!DOCTYPE")
                || mathMl.contains("<!ENTITY")) {
            throw new IOException("El MathML no superó la validación de seguridad.");
        }
        try {
            javax.xml.parsers.DocumentBuilderFactory factory =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(
                    new org.xml.sax.InputSource(new java.io.StringReader(mathMl)));
            String root = document.getDocumentElement().getLocalName();
            if (root == null) root = document.getDocumentElement().getNodeName();
            if (!"math".equals(root)) {
                throw new IOException("El contenido no tiene una raíz MathML <math>.");
            }
        } catch (IOException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IOException("El MathML no es XML válido.", failure);
        }
    }
}
