package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceKind;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceProductizationPolicy;

import java.util.List;

/** Official DocuPodcast AI resources bundled with the application. */
public final class OfficialAiResourceCatalog implements AiResourceCatalog {
    private static final AiResourceProductizationPolicy PRODUCTIZATION_POLICY =
            AiResourceProductizationPolicy.withoutMarkdownImporters();

    private final List<AiResourceDescriptor> descriptors;

    public OfficialAiResourceCatalog() {
        this.descriptors = List.of(
                descriptor("prompt-document-reading", AiResourceKind.PROMPT_GUIDE, "/ai-resources/01_prompt_preparar_documento_lectura.md", "01_prompt_preparar_documento_lectura.md", false, "", "Usar con un documento Word/PDF/Markdown/TXT para mejorar claridad antes de abrirlo como fuente documental.", "Prompt de apoyo para preparar documentos de estudio sin convertirlos en libreto."),
                descriptor("checklist-document", AiResourceKind.REFERENCE, "/ai-resources/02_checklist_documento_lectura.md", "02_checklist_documento_lectura.md", false, "", "Revisar antes de importar un documento fuente para lectura.", "Checklist de legibilidad y estructura documental."),
                descriptor("template-study-notes", AiResourceKind.AI_TEMPLATE, "/ai-resources/03_plantilla_notas_documento.md", "03_plantilla_notas_documento.md", false, "", "Copiar y completar si se quiere preparar notas de estudio fuera de la app; no es importable como contrato especial.", "Plantilla documental no importable para notas de estudio."),
                descriptor("example-study-document", AiResourceKind.MINIMAL_EXAMPLE, "/ai-resources/official-markdown/ejemplos/documento_academico_minimo.md", "official-markdown/ejemplos/documento_academico_minimo.md", false, "", "Ejemplo de Markdown abierto como documento fuente normal.", "Ejemplo de documento académico mínimo."),
                descriptor("reference-voices", AiResourceKind.REFERENCE, "/ai-resources/official-markdown/referencias/voces_autorizadas.md", "official-markdown/referencias/voces_autorizadas.md", false, "", "Leer antes de importar voces propias o autorizadas.", "Referencia ética y operativa sobre voces.")
        );
        List<String> violations = PRODUCTIZATION_POLICY.validate(descriptors);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Invalid AI resource catalog: " + String.join("; ", violations));
        }
    }

    @Override
    public List<AiResourceDescriptor> descriptors() {
        return descriptors;
    }

    private static AiResourceDescriptor descriptor(String id, AiResourceKind kind, String classpathLocation, String targetRelativePath,
                                                   boolean importable, String contract, String use, String description) {
        return new AiResourceDescriptor(id, kind, classpathLocation, targetRelativePath, importable, contract, use, description);
    }
}
