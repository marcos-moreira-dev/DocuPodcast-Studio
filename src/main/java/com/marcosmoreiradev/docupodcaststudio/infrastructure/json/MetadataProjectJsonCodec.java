package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.*;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.*;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.*;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.domain.study.*;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.*;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonReadSupport.*;
import static com.marcosmoreiradev.docupodcaststudio.infrastructure.json.ProjectJsonWriteSupport.*;

final class MetadataProjectJsonCodec implements ProjectJsonSectionCodec {
    static ProjectMetadata readMetadata(Map<String, Object> project) throws IOException {
        ProjectMode mode = project.containsKey("mode")
                ? enumValue(ProjectMode.class, string(project.get("mode"), "project.mode"), "project.mode")
                : null;
        return new ProjectMetadata(
                string(project.get("id"), "project.id"),
                string(project.get("title"), "project.title"),
                stringOrDefault(project.get("description"), ""),
                stringOrDefault(project.get("language"), "es"),
                enumValue(ProjectKind.class, stringOrDefault(project.get("kind"), ProjectKind.EMPTY.name()), "project.kind"),
                mode,
                enumValue(ProjectStatus.class, stringOrDefault(project.get("status"), ProjectStatus.DRAFT.name()), "project.status"),
                instant(project.get("createdAt"), "project.createdAt"),
                instant(project.get("updatedAt"), "project.updatedAt")
        );
    }

    static void writeProject(StringBuilder out, ProjectMetadata metadata) {
        indent(out, 1).append("\"project\": {\n");
        field(out, 2, "id", quote(metadata.id())); out.append(",\n");
        field(out, 2, "title", quote(metadata.title())); out.append(",\n");
        field(out, 2, "description", quote(metadata.description())); out.append(",\n");
        field(out, 2, "language", quote(metadata.language())); out.append(",\n");
        field(out, 2, "kind", quote(metadata.kind().name())); out.append(",\n");
        field(out, 2, "mode", quote(metadata.mode().name())); out.append(",\n");
        field(out, 2, "status", quote(metadata.status().name())); out.append(",\n");
        field(out, 2, "createdAt", quote(metadata.createdAt().toString())); out.append(",\n");
        field(out, 2, "updatedAt", quote(metadata.updatedAt().toString())); out.append("\n");
        indent(out, 1).append("}");
    }

    @Override public String sectionName() { return "project"; }
}

