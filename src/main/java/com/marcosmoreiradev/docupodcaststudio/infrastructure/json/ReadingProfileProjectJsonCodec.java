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

final class ReadingProfileProjectJsonCodec implements ProjectJsonSectionCodec {
    static ReadingProfile readReadingProfile(Map<String, Object> map) throws IOException {
        if (map.isEmpty()) {
            return ReadingProfile.academicDefaults();
        }
        HeadingDetectionRules defaults = HeadingDetectionRules.academicDefaults();
        Map<String, Object> rules = optionalObject(map.get("headingRules"));
        HeadingDetectionRules headingRules = new HeadingDetectionRules(
                stringListOrDefault(rules.get("titleStyleKeywords"), defaults.titleStyleKeywords()),
                stringListOrDefault(rules.get("headingStyleKeywords"), defaults.headingStyleKeywords()),
                stringListOrDefault(rules.get("subheadingStyleKeywords"), defaults.subheadingStyleKeywords()),
                intOrDefault(rules.get("maxShortBoldWords"), defaults.maxShortBoldWords()),
                booleanOrDefault(rules.get("treatShortBoldParagraphAsSubheading"), defaults.treatShortBoldParagraphAsSubheading())
        );
        return new ReadingProfile(
                stringOrDefault(map.get("id"), ReadingProfile.academicDefaults().id()),
                stringOrDefault(map.get("name"), ReadingProfile.academicDefaults().name()),
                stringOrDefault(map.get("description"), ReadingProfile.academicDefaults().description()),
                headingRules,
                enumValue(ImageNarrationPolicy.class, stringOrDefault(map.get("imagePolicy"), ImageNarrationPolicy.READ_DESCRIPTION_OR_OMIT.name()), "readingProfile.imagePolicy"),
                enumValue(TableNarrationPolicy.class, stringOrDefault(map.get("tablePolicy"), TableNarrationPolicy.ANNOUNCE_SUMMARY.name()), "readingProfile.tablePolicy")
        );
    }

    @SuppressWarnings("unchecked")

    static void writeReadingProfile(StringBuilder out, ReadingProfile profile) {
        indent(out, 1).append("\"readingProfile\": {\n");
        field(out, 2, "id", quote(profile.id())); out.append(",\n");
        field(out, 2, "name", quote(profile.name())); out.append(",\n");
        field(out, 2, "description", quote(profile.description())); out.append(",\n");
        field(out, 2, "imagePolicy", quote(profile.imagePolicy().name())); out.append(",\n");
        field(out, 2, "tablePolicy", quote(profile.tablePolicy().name())); out.append(",\n");
        indent(out, 2).append("\"headingRules\": {\n");
        stringArrayField(out, 3, "titleStyleKeywords", profile.headingRules().titleStyleKeywords()); out.append(",\n");
        stringArrayField(out, 3, "headingStyleKeywords", profile.headingRules().headingStyleKeywords()); out.append(",\n");
        stringArrayField(out, 3, "subheadingStyleKeywords", profile.headingRules().subheadingStyleKeywords()); out.append(",\n");
        field(out, 3, "maxShortBoldWords", Integer.toString(profile.headingRules().maxShortBoldWords())); out.append(",\n");
        field(out, 3, "treatShortBoldParagraphAsSubheading", Boolean.toString(profile.headingRules().treatShortBoldParagraphAsSubheading())); out.append("\n");
        indent(out, 2).append("}\n");
        indent(out, 1).append("}");
    }

    @Override public String sectionName() { return "readingProfile"; }
}

