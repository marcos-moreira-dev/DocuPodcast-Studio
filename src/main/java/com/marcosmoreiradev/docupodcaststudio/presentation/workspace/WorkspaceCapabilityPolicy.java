package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.CommandAvailabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.WorkspaceCapabilityCommandMapper;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.beans.binding.BooleanBinding;

import java.util.Objects;

/** Runtime availability adapter for legacy workspace capabilities exposed in UI. */
public final class WorkspaceCapabilityPolicy {
    private final WorkspaceCapabilityCommandMapper commandMapper = new WorkspaceCapabilityCommandMapper();
    private final CommandAvailabilityPolicy commandAvailabilityPolicy = new CommandAvailabilityPolicy();

    public BooleanBinding disabledBinding(WorkspaceCapability capability, DocuPodcastShellViewModel viewModel) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(viewModel, "viewModel");
        return commandAvailabilityPolicy.disabledBinding(commandMapper.commandFor(capability), viewModel);
    }
}
