package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.AppIcon;
import javafx.scene.Parent;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Simple side-dock module backed by an existing JavaFX node supplier. */
public final class StaticSideDockModule implements SideDockModule {
    private final SideDockModuleId id;
    private final String title;
    private final String tooltip;
    private final AppIcon icon;
    private final Supplier<Parent> contentSupplier;
    private final Predicate<SideDockContext> supportPredicate;
    private Parent cachedView;

    public StaticSideDockModule(SideDockModuleId id, String title, String tooltip, AppIcon icon,
                                Supplier<Parent> contentSupplier, Predicate<SideDockContext> supportPredicate) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = require(title, "title");
        this.tooltip = tooltip == null ? title : tooltip;
        this.icon = Objects.requireNonNullElse(icon, AppIcon.DEFAULT);
        this.contentSupplier = Objects.requireNonNull(contentSupplier, "contentSupplier");
        this.supportPredicate = supportPredicate == null ? context -> true : supportPredicate;
    }

    public static StaticSideDockModule of(SideDockModuleId id, String title, String tooltip, AppIcon icon,
                                          Supplier<Parent> contentSupplier) {
        return new StaticSideDockModule(id, title, tooltip, icon, contentSupplier, context -> true);
    }

    /** Source-compatible bridge; new modules should provide an explicit {@link AppIcon}. */
    @Deprecated(forRemoval = false)
    public StaticSideDockModule(SideDockModuleId id, String title, String tooltip, String ignoredIconText,
                                Supplier<Parent> contentSupplier, Predicate<SideDockContext> supportPredicate) {
        this(id, title, tooltip, AppIcon.DEFAULT, contentSupplier, supportPredicate);
    }

    /** Source-compatible bridge; new modules should provide an explicit {@link AppIcon}. */
    @Deprecated(forRemoval = false)
    public static StaticSideDockModule of(SideDockModuleId id, String title, String tooltip,
                                          String ignoredIconText, Supplier<Parent> contentSupplier) {
        return of(id, title, tooltip, AppIcon.DEFAULT, contentSupplier);
    }

    @Override public SideDockModuleId id() { return id; }
    @Override public String title() { return title; }
    @Override public String tooltip() { return tooltip; }
    @Override public AppIcon icon() { return icon; }
    @Override public boolean supports(SideDockContext context) { return supportPredicate.test(context); }
    @Override public Parent createView(SideDockContext context) {
        if (cachedView == null) {
            cachedView = Objects.requireNonNull(contentSupplier.get(), "contentSupplier returned null");
        }
        return cachedView;
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
