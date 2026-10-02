package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Writes a human-readable, credential-free summary beside the batch outputs. */
public final class WriteDocumentVideoBatchReportUseCase {
    public static final String REPORT_RELATIVE_PATH = "informes/resumen-ultima-ejecucion.txt";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    public Path write(DocumentVideoBatchProject project, Path descriptor,
                      boolean paused, String runMessage) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(descriptor, "descriptor");
        Path root = descriptor.toAbsolutePath().normalize().getParent();
        if (root == null) throw new IOException("El proyecto por lotes no tiene una carpeta válida.");
        Path report = root.resolve(REPORT_RELATIVE_PATH).normalize();
        if (!report.startsWith(root)) throw new IOException("Ruta de informe fuera del proyecto por lotes.");
        Files.createDirectories(report.getParent());

        long completed = count(project, BatchItemState.COMPLETED);
        long failed = count(project, BatchItemState.FAILED);
        long skipped = count(project, BatchItemState.SKIPPED);
        long cancelled = count(project, BatchItemState.CANCELLED);
        long pending = project.items().size() - completed - failed - skipped - cancelled;
        StringBuilder text = new StringBuilder()
                .append("DocuPodcast Studio — Documentos a audio o video Express\n")
                .append("Salida: ").append(project.profile().outputLabel()).append('\n')
                .append("Proyecto: ").append(project.title()).append('\n')
                .append("Actualizado: ").append(DATE_TIME.format(project.updatedAt())).append('\n')
                .append("Resultado: ").append(paused ? "PAUSADO" : "TERMINADO").append('\n')
                .append("Detalle: ").append(safe(runMessage)).append("\n\n")
                .append("Completados: ").append(completed).append('\n')
                .append("Fallidos: ").append(failed).append('\n')
                .append("Omitidos: ").append(skipped).append('\n')
                .append("Cancelados: ").append(cancelled).append('\n')
                .append("Pendientes o pausados: ").append(Math.max(0, pending)).append("\n\n")
                .append("DOCUMENTOS\n");
        project.items().stream()
                .sorted(java.util.Comparator.comparingInt(DocumentVideoBatchItem::order))
                .forEach(item -> text.append(item.order() + 1).append(". ")
                        .append(item.title()).append(" — ")
                        .append(label(item.state())).append(" — ")
                        .append(safe(item.message())).append('\n')
                        .append("   Fuente: ").append(item.sourceRelativePath()).append('\n')
                        .append(project.profile().audioOnly() ? "   Audio: " : "   Video: ").append(item.outputRelativePath()).append('\n'));

        Path temporary = report.resolveSibling(report.getFileName() + ".tmp");
        Files.writeString(temporary, text.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, report, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, report, StandardCopyOption.REPLACE_EXISTING);
        }
        return report;
    }

    private static long count(DocumentVideoBatchProject project, BatchItemState state) {
        return project.items().stream().filter(item -> item.state() == state).count();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Sin detalle adicional" : value.strip();
    }

    private static String label(BatchItemState state) {
        return switch (state) {
            case PENDING -> "Pendiente";
            case RUNNING -> "En proceso";
            case PAUSE_REQUESTED -> "Pausa solicitada";
            case PAUSED -> "Pausado";
            case COMPLETED -> "Completado";
            case FAILED -> "Fallido";
            case SKIPPED -> "Omitido";
            case CANCELLED -> "Cancelado";
        };
    }
}
