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

final class ViewStateProjectJsonCodec implements ProjectJsonSectionCodec {
    static Map<String, String> readView(Map<String, Object> view) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : view.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")

    static void writeView(StringBuilder out, Map<String, String> viewState) {
        indent(out, 1).append("\"view\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : viewState.entrySet()) {
            field(out, 2, entry.getKey(), quote(entry.getValue()));
            if (index < viewState.size() - 1) {
                out.append(",");
            }
            out.append("\n");
            index++;
        }
        indent(out, 1).append("}");
    }

    @Override public String sectionName() { return "view"; }
}

