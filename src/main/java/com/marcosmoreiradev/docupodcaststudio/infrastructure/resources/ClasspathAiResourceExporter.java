package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceExporter;
import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceProductizationPolicy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Exports classpath AI resources and generates an index with importability metadata. */
public final class ClasspathAiResourceExporter implements AiResourceExporter {
    private final AiResourceCatalog catalog;
    private final AiResourceProductizationPolicy productizationPolicy;

    public ClasspathAiResourceExporter(AiResourceCatalog catalog) {
        this(catalog, AiResourceProductizationPolicy.withoutMarkdownImporters());
    }

    public ClasspathAiResourceExporter(AiResourceCatalog catalog, AiResourceProductizationPolicy productizationPolicy) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.productizationPolicy = Objects.requireNonNull(productizationPolicy, "productizationPolicy");
    }

    @Override
    public AiResourceExportResult export(Path targetDirectory) throws IOException {
        Objects.requireNonNull(targetDirectory, "targetDirectory");
        Files.createDirectories(targetDirectory);
        int count = 0;
        for (AiResourceDescriptor descriptor : catalog.descriptors()) {
            Path target = targetDirectory.resolve(descriptor.targetRelativePath()).normalize();
            if (!target.startsWith(targetDirectory.normalize())) {
                throw new IOException("Unsafe AI resource target: " + descriptor.targetRelativePath());
            }
            Files.createDirectories(target.getParent());
            try (InputStream stream = ClasspathAiResourceExporter.class.getResourceAsStream(descriptor.classpathLocation())) {
                if (stream == null) {
                    throw new IOException("Missing AI resource: " + descriptor.classpathLocation());
                }
                Files.write(target, stream.readAllBytes());
                count++;
            }
        }
        Path index = targetDirectory.resolve("00_indice_recursos_ia.md");
        Files.writeString(index, buildIndex(), StandardCharsets.UTF_8);
        return new AiResourceExportResult(targetDirectory, index, count + 1);
    }

    private String buildIndex() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Índice de recursos IA — DocuPodcast Studio\n\n");
        builder.append("Estos recursos ayudan a revisar y preparar documentos de estudio antes de abrirlos en DocuPodcast. ")
                .append("Markdown se trata como documento fuente normal, no como contrato importable especial. ")
                .append("El proyecto editable completo vive en `.docupodcast.json`.\n\n")
                .append("> Estado de producto: no hay importador de contratos Markdown especiales en la experiencia de usuario. ")
                .append("Los recursos IA son guías, referencias, plantillas o ejemplos documentales no importables como contrato especial.\n\n");
        for (AiResourceDescriptor descriptor : catalog.descriptors()) {
            builder.append("## ").append(descriptor.targetRelativePath()).append("\n\n")
                    .append("- Tipo de recurso: ").append(descriptor.kind()).append("\n")
                    .append("- Importable por la aplicación: ").append(productizationPolicy.supportsImport(descriptor) ? "sí" : "no").append("\n")
                    .append("- Estado de importación: ").append(productizationPolicy.importStatus(descriptor)).append("\n")
                    .append("- Contrato: ").append(descriptor.contract().isBlank() ? "no aplica" : descriptor.contract()).append("\n")
                    .append("- Uso recomendado: ").append(descriptor.recommendedUse()).append("\n")
                    .append("- Descripción: ").append(descriptor.description()).append("\n\n");
        }
        return builder.toString();
    }
}
