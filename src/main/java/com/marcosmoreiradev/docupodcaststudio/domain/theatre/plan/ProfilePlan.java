package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record ProfilePlan(String name, String notes, String voz, String tono, List<ImageRef> imagenes) {
}
