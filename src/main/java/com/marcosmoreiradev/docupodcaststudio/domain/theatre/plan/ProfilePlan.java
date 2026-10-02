package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record ProfilePlan(String name, String notes, String voz, String tono, List<ImageRef> imagenes,
                          String id, List<String> aliases) {
    public ProfilePlan {
        name = name == null ? "" : name.strip();
        notes = notes == null ? "" : notes.strip();
        voz = voz == null ? "" : voz.strip();
        tono = tono == null ? "" : tono.strip();
        imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
        id = id == null ? "" : id.strip();
        aliases = aliases == null ? List.of() : aliases.stream().map(String::strip).filter(v -> !v.isBlank()).toList();
    }

    public ProfilePlan(String name, String notes, String voz, String tono, List<ImageRef> imagenes) {
        this(name, notes, voz, tono, imagenes, "", List.of());
    }
}
