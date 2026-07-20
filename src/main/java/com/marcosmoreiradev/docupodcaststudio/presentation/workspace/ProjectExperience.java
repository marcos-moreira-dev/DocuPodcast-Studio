package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockModuleId;

import java.util.Objects;
import java.util.Set;

/** Declarative product composition; shared infrastructure remains outside the experience. */
public record ProjectExperience(
        ProjectMode mode,
        ProductMaturity maturity,
        WorkspaceKind primaryWorkspace,
        Set<WorkspaceKind> workspaces,
        Set<AppCommandId> commands,
        Set<SideDockModuleId> sideDocks,
        Set<String> exports,
        Set<CapabilityId> capabilities) {
    public ProjectExperience {
        mode = Objects.requireNonNull(mode, "mode");
        maturity = Objects.requireNonNullElse(maturity, ProductMaturity.STABLE);
        primaryWorkspace = Objects.requireNonNull(primaryWorkspace, "primaryWorkspace");
        workspaces = workspaces == null ? Set.of(primaryWorkspace) : Set.copyOf(workspaces);
        if (!workspaces.contains(primaryWorkspace)) throw new IllegalArgumentException("primary workspace must be contributed");
        commands = commands == null ? Set.of() : Set.copyOf(commands);
        sideDocks = sideDocks == null ? Set.of() : Set.copyOf(sideDocks);
        exports = exports == null ? Set.of() : Set.copyOf(exports);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
