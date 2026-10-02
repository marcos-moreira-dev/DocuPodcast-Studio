package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Explicit, idempotent queue commands persisted after every user action. */
public final class ManageDocumentVideoBatchQueueUseCase {
    private final DocumentVideoBatchRepository repository;

    public ManageDocumentVideoBatchQueueUseCase(DocumentVideoBatchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocumentVideoBatchProject pause(DocumentVideoBatchProject project, Path descriptor, String itemId) throws IOException {
        return update(project, descriptor, itemId, item -> switch (item.state()) {
            case RUNNING -> item.withState(BatchItemState.PAUSE_REQUESTED, item.stage(), item.progress(),
                    "La pausa se aplicará en el próximo punto seguro");
            case PENDING -> item.withState(BatchItemState.PAUSED, item.stage(), item.progress(), "Pausado antes de iniciar");
            default -> item;
        });
    }

    public DocumentVideoBatchProject resume(DocumentVideoBatchProject project, Path descriptor, String itemId) throws IOException {
        return update(project, descriptor, itemId, item -> switch (item.state()) {
            case PAUSED, PAUSE_REQUESTED, FAILED -> item.withState(BatchItemState.PENDING, item.stage(), item.progress(), "Listo para continuar");
            default -> item;
        });
    }

    public DocumentVideoBatchProject skip(DocumentVideoBatchProject project, Path descriptor, String itemId) throws IOException {
        return update(project, descriptor, itemId, item -> switch (item.state()) {
            case COMPLETED, RUNNING -> item;
            default -> item.withState(BatchItemState.SKIPPED, item.stage(), item.progress(), "Omitido por el usuario");
        });
    }

    public DocumentVideoBatchProject retry(DocumentVideoBatchProject project, Path descriptor, String itemId) throws IOException {
        return update(project, descriptor, itemId, item -> switch (item.state()) {
            case FAILED, CANCELLED, SKIPPED -> item.withState(BatchItemState.PENDING, item.stage(), item.progress(), "Listo para reintentar");
            default -> item;
        });
    }

    /** Cancels every item that can still perform work, preserving completed and skipped results. */
    public DocumentVideoBatchProject cancelAll(DocumentVideoBatchProject project, Path descriptor) throws IOException {
        List<DocumentVideoBatchItem> changed = project.items().stream().map(item -> switch (item.state()) {
            case COMPLETED, FAILED, SKIPPED, CANCELLED -> item;
            default -> item.withState(BatchItemState.CANCELLED, item.stage(), item.progress(),
                    "Producción completa cancelada por el usuario; derivados válidos conservados");
        }).toList();
        if (changed.equals(project.items())) return project;
        return save(project, descriptor, changed);
    }

    /** Persists an executor checkpoint. Safe to call repeatedly with the same values. */
    public DocumentVideoBatchProject transition(DocumentVideoBatchProject project, Path descriptor,
                                                String itemId, BatchItemState state,
                                                BatchItemStage stage, double progress,
                                                String message) throws IOException {
        return update(project, descriptor, itemId,
                item -> item.withState(state, stage, progress, message));
    }

    /** Makes an interrupted process explicitly recoverable on the next application start. */
    public DocumentVideoBatchProject recoverInterrupted(DocumentVideoBatchProject project,
                                                        Path descriptor) throws IOException {
        List<DocumentVideoBatchItem> recovered = project.items().stream().map(item -> switch (item.state()) {
            case RUNNING -> item.withState(BatchItemState.PENDING, item.stage(), item.progress(),
                    "Interrumpido anteriormente; listo para continuar desde el último derivado válido");
            case PAUSE_REQUESTED -> item.withState(BatchItemState.PAUSED, item.stage(), item.progress(),
                    "Pausa aplicada al recuperar la cola");
            default -> item;
        }).toList();
        return recovered.equals(project.items()) ? project : save(project, descriptor, recovered);
    }

    public DocumentVideoBatchProject move(DocumentVideoBatchProject project, Path descriptor, String itemId, int delta) throws IOException {
        List<DocumentVideoBatchItem> ordered = new ArrayList<>(project.items());
        ordered.sort(Comparator.comparingInt(DocumentVideoBatchItem::order));
        int from = -1;
        for (int index = 0; index < ordered.size(); index++) if (ordered.get(index).id().equals(itemId)) from = index;
        int to = Math.max(0, Math.min(ordered.size() - 1, from + delta));
        if (from < 0 || from == to) return project;
        DocumentVideoBatchItem moving = ordered.remove(from); ordered.add(to, moving);
        List<DocumentVideoBatchItem> renumbered = new ArrayList<>();
        for (int index = 0; index < ordered.size(); index++) {
            DocumentVideoBatchItem item = ordered.get(index);
            renumbered.add(new DocumentVideoBatchItem(item.id(), index, item.title(), item.sourceRelativePath(),
                    item.copiedSourceRelativePath(), item.childProjectRelativePath(), item.outputVideoRelativePath(),
                    item.sha256(), item.sourceBytes(), item.embeddedMediaCount(), item.state(), item.stage(),
                    item.progress(), item.message(), Instant.now()));
        }
        return save(project, descriptor, renumbered);
    }

    private DocumentVideoBatchProject update(DocumentVideoBatchProject project, Path descriptor, String itemId,
                                              java.util.function.UnaryOperator<DocumentVideoBatchItem> change) throws IOException {
        List<DocumentVideoBatchItem> changed = project.items().stream()
                .map(item -> item.id().equals(itemId) ? change.apply(item) : item).toList();
        if (changed.equals(project.items())) return project;
        return save(project, descriptor, changed);
    }

    private DocumentVideoBatchProject save(DocumentVideoBatchProject project, Path descriptor,
                                            List<DocumentVideoBatchItem> items) throws IOException {
        DocumentVideoBatchProject updated = new DocumentVideoBatchProject(project.formatVersion(), project.id(),
                project.title(), project.sourceRoot(), project.projectRoot(), project.descriptorRelativePath(),
                project.profile(), items, project.ignoredFileCount(), project.createdAt(), Instant.now());
        repository.save(updated, descriptor);
        return updated;
    }
}
