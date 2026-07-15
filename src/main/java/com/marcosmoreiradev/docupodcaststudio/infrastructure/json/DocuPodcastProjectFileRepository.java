package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRepository;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectValidationResult;
import com.marcosmoreiradev.docupodcaststudio.application.project.ValidateProjectPayloadUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** File-based repository for .docupodcast.json projects. */
public final class DocuPodcastProjectFileRepository implements ProjectRepository {
    private final DocuPodcastProjectJsonWriter writer = new DocuPodcastProjectJsonWriter();
    private final DocuPodcastProjectJsonReader reader = new DocuPodcastProjectJsonReader();
    private final ValidateProjectPayloadUseCase payloadValidator = new ValidateProjectPayloadUseCase();

    @Override
    public void save(DocuPodcastProject project, Path targetFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(targetFile, "targetFile");
        ProjectValidationResult validation = payloadValidator.validate(project);
        if (!validation.valid()) {
            throw new IOException("Invalid DocuPodcast project payload: " + String.join("; ", validation.messages()));
        }
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(targetFile, writer.write(project), StandardCharsets.UTF_8);
    }

    @Override
    public DocuPodcastProject open(Path sourceFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        String json = Files.readString(sourceFile, StandardCharsets.UTF_8);
        return reader.read(json);
    }
}
