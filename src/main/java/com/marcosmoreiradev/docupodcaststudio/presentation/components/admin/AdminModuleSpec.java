package com.marcosmoreiradev.docupodcaststudio.presentation.components.admin;

/** Display-only contract for a module contributed by an administrative mini-application. */
public interface AdminModuleSpec<M> {
    M id();
    String group();
    String title();
    String description();
}
