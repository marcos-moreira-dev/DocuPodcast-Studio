package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Input for rendering one theatre intervention with simultaneous character voices. */
public record TheatreChoralVoiceRenderRequest(
        DocuPodcastProject project,
        Path projectFile,
        NarrationScriptDocument script,
        String interventionId,
        List<String> participantCharacterIds,
        AudioEngineDescriptor engineDescriptor
) {
    public TheatreChoralVoiceRenderRequest {
        project = Objects.requireNonNull(project, "project");
        projectFile = Objects.requireNonNull(projectFile, "projectFile").toAbsolutePath().normalize();
        script = Objects.requireNonNull(script, "script");
        interventionId = interventionId == null ? "" : interventionId.strip();
        participantCharacterIds = participantCharacterIds == null ? List.of() : participantCharacterIds.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        engineDescriptor = engineDescriptor == null ? AudioEngineDescriptor.mock() : engineDescriptor;
    }
}
