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

final class AssetsProjectJsonCodec implements ProjectJsonSectionCodec {
    static ProjectAssetCatalog readAssets(Map<String, Object> assets) throws IOException {
        Object rawItems = assets.getOrDefault("items", List.of());
        if (!(rawItems instanceof List<?> list)) {
            throw new IOException("assets.items must be an array");
        }
        List<ProjectAssetReference> references = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawAsset)) {
                throw new IOException("assets.items entries must be objects");
            }
            Map<String, Object> asset = (Map<String, Object>) rawAsset;
            references.add(new ProjectAssetReference(
                    string(asset.get("id"), "asset.id"),
                    enumValue(ProjectAssetKind.class, string(asset.get("kind"), "asset.kind"), "asset.kind"),
                    string(asset.get("displayName"), "asset.displayName"),
                    string(asset.get("relativePath"), "asset.relativePath"),
                    stringOrDefault(asset.get("mimeType"), ""),
                    stringOrDefault(asset.get("purpose"), ""),
                    stringOrDefault(asset.get("checksum"), ""),
                    stringOrDefault(asset.get("notes"), "")
            ));
        }
        return new ProjectAssetCatalog(references);
    }


    @SuppressWarnings("unchecked")

    static void writeAssets(StringBuilder out, DocuPodcastProject project) {
        indent(out, 1).append("\"assets\": {\n");
        indent(out, 2).append("\"items\": [");
        if (!project.assets().references().isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < project.assets().references().size(); i++) {
            ProjectAssetReference asset = project.assets().references().get(i);
            indent(out, 3).append("{\n");
            field(out, 4, "id", quote(asset.id())); out.append(",\n");
            field(out, 4, "kind", quote(asset.kind().name())); out.append(",\n");
            field(out, 4, "displayName", quote(asset.displayName())); out.append(",\n");
            field(out, 4, "relativePath", quote(asset.relativePath())); out.append(",\n");
            field(out, 4, "mimeType", quote(asset.mimeType())); out.append(",\n");
            field(out, 4, "purpose", quote(asset.purpose())); out.append(",\n");
            field(out, 4, "checksum", quote(asset.checksum())); out.append(",\n");
            field(out, 4, "notes", quote(asset.notes())); out.append("\n");
            indent(out, 3).append("}");
            if (i < project.assets().references().size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        indent(out, 2).append("]\n");
        indent(out, 1).append("}");
    }

    @Override public String sectionName() { return "assets"; }
}

