package com.marcosmoreiradev.docupodcaststudio.application.services;
import com.marcosmoreiradev.docupodcaststudio.application.batch.*;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRepository;
/** Dependencies composed once for the Express setup and its queue. */
public record BatchApplicationServices(DocumentVideoBatchWorkspaceRepository repository,
        DiscoverDocumentVideoBatchSourcesUseCase discovery, ManageDocumentVideoBatchQueueUseCase queue,
        CreateDocumentVideoBatchProjectUseCase creator) {
    public static BatchApplicationServices create(DocumentVideoBatchWorkspaceRepository repository, ProjectRepository projects) {
        var discovery = new DiscoverDocumentVideoBatchSourcesUseCase();
        return new BatchApplicationServices(repository, discovery, new ManageDocumentVideoBatchQueueUseCase(repository),
                new CreateDocumentVideoBatchProjectUseCase(discovery, repository, projects));
    }
}
