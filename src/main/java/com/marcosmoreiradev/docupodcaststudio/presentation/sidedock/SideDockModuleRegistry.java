package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Collects side-dock modules and filters them by context. */
public final class SideDockModuleRegistry {
    private final List<SideDockModule> modules = new ArrayList<>();

    public SideDockModuleRegistry register(SideDockModule module) {
        modules.add(Objects.requireNonNull(module, "module"));
        return this;
    }

    public List<SideDockModule> modulesFor(SideDockContext context) {
        return modules.stream().filter(module -> module.supports(context)).toList();
    }
}
