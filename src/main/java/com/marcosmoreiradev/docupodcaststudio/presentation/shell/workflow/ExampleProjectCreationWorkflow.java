package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectMaterialization;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Creates a bundled example project without leaving the orchestration inside the shell view.
 *
 * <p>The shell remains responsible for user choices such as FileChooser. This workflow owns the
 * operational sequence: materialize resources, create/import/save project, bind demo visuals and
 * return any user-visible fallback notice.</p>
 */
public final class ExampleProjectCreationWorkflow {
    public Result create(DocuPodcastShellViewModel viewModel,
                         ExampleProjectDescriptor example,
                         Path targetProjectFile) throws IOException {
        Objects.requireNonNull(viewModel, "viewModel");
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(targetProjectFile, "targetProjectFile");
        ExampleProjectMaterialization materialized = viewModel.applicationServices()
                .examples()
                .createExampleProject()
                .materialize(example, targetProjectFile);
        viewModel.createNewProject(example.defaultProjectName());
        viewModel.importWordDocument(materialized.sourceDocument());
        if (example.hasTheatreMarkdown()) {
            viewModel.configureAviadoresTheatreDemo(
                    example.id(),
                    materialized.visualAssets(),
                    materialized.theatreMarkdownFile(),
                    targetProjectFile);
            viewModel.saveCurrentProjectAs(targetProjectFile);
            String message = "Proyecto demo creado desde teatro.md: " + example.title() + ".";
            viewModel.showDocumentWorkspace(message);
            return new Result(message, Optional.empty());
        }
        viewModel.saveCurrentProjectAs(targetProjectFile);
        ExampleVisualBindingWorkflow.Result visualResult = viewModel.importAndBindExampleVisuals(
                materialized.visualAssets(), example.visualBindings());
        viewModel.configureAviadoresTheatreDemo(example.id(), materialized.visualAssets(), materialized.theatreMarkdownFile());
        viewModel.saveCurrentProjectAs(targetProjectFile);
        String message = "Proyecto demo creado: " + example.title() + ". " + visualResult.message();
        viewModel.showDocumentWorkspace(message);
        return new Result(message, visualResult.userDecision(example.title()));
    }

    public Result createInDirectory(DocuPodcastShellViewModel viewModel,
                                    ExampleProjectDescriptor example,
                                    Path targetDirectory) throws IOException {
        Objects.requireNonNull(viewModel, "viewModel");
        Objects.requireNonNull(example, "example");
        Objects.requireNonNull(targetDirectory, "targetDirectory");
        ExampleProjectMaterialization materialized = null;
        boolean projectFileExistedBefore = false;
        try {
            materialized = viewModel.applicationServices()
                    .examples()
                    .createExampleProject()
                    .materializeInDirectory(example, targetDirectory);
            projectFileExistedBefore = Files.exists(materialized.projectFile());
            viewModel.createNewProject(example.defaultProjectName());
            viewModel.importWordDocument(materialized.sourceDocument());
            viewModel.configureAviadoresTheatreDemo(
                    example.id(),
                    materialized.visualAssets(),
                    materialized.theatreMarkdownFile(),
                    materialized.projectFile());
            viewModel.saveCurrentProjectAs(materialized.projectFile());
            String message = "Proyecto demo creado desde teatro.md: " + example.title() + ".";
            viewModel.showDocumentWorkspace(message);
            return new Result(message, Optional.empty());
        } catch (IOException | RuntimeException ex) {
            if (materialized != null && !projectFileExistedBefore) {
                Files.deleteIfExists(materialized.projectFile());
            }
            viewModel.closeCurrentProject();
            throw ex;
        }
    }

    public record Result(String message, Optional<UserVisibleDecision> decision) {
        public Result {
            message = message == null ? "Proyecto demo creado." : message.strip();
            decision = decision == null ? Optional.empty() : decision;
        }
    }
}
