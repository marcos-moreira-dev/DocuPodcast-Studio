package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Writes a lightweight Markdown diagnostic report for the active project. */
public final class ExportDiagnosticReportUseCase {
    public Path export(DocuPodcastProject project, NarrationScriptDocument script, StoryboardDocument storyboard,
                       List<AudioJobSnapshot> jobs, Path target) throws IOException {
        Objects.requireNonNull(project, "project");
        Path normalized = ExportTargetPathPolicy.ensureMarkdownExtension(target);
        if (normalized.getParent() != null) {
            Files.createDirectories(normalized.getParent());
        }
        Files.writeString(normalized, report(project, script, storyboard, jobs), StandardCharsets.UTF_8);
        return normalized;
    }

    public String report(DocuPodcastProject project, NarrationScriptDocument script, StoryboardDocument storyboard,
                         List<AudioJobSnapshot> jobs) {
        StringBuilder out = new StringBuilder();
        out.append("# Reporte diagnóstico DocuPodcast\n\n");
        out.append("Generado: ").append(Instant.now()).append("\n\n");
        out.append("## Proyecto\n\n");
        out.append("- Título: ").append(project.metadata().title()).append("\n");
        out.append("- Tipo: ").append(project.metadata().mode().displayName()).append("\n");
        out.append("- Estado tecnico: ").append(project.metadata().kind().displayName()).append("\n");
        out.append("- Estado: ").append(project.metadata().status().displayName()).append("\n");
        out.append("- Idioma: ").append(project.metadata().language()).append("\n");
        out.append("- Assets: ").append(project.assets().size()).append("\n\n");
        out.append("## Lectura preparada\n\n");
        if (script == null) {
            out.append("Sin lectura preparada cargada en la sesión.\n\n");
        } else {
            out.append("- Segmentos: ").append(script.segmentCount()).append("\n");
            out.append("- Palabras: ").append(script.wordCount()).append("\n");
            out.append("- Caracteres estimados: ").append(script.estimatedCharacters()).append("\n\n");
        }
        out.append("## Visuales\n\n");
        if (storyboard == null) {
            out.append("Sin visuales cargados en la sesión.\n\n");
        } else {
            out.append("- Bindings de imagen: ").append(storyboard.bindingCount()).append("\n\n");
        }
        out.append("## Jobs de audio\n\n");
        if (jobs == null || jobs.isEmpty()) {
            out.append("Sin jobs persistidos.\n");
        } else {
            for (AudioJobSnapshot job : jobs) {
                out.append("- ").append(job.jobId()).append(": ").append(job.state().displayName())
                        .append(" · ").append(job.completedSegments()).append("/").append(job.totalSegments())
                        .append(" · fallidos ").append(job.failedSegments())
                        .append(" · ").append(job.recoveryLabel()).append("\n");
            }
        }
        return out.toString();
    }
}
