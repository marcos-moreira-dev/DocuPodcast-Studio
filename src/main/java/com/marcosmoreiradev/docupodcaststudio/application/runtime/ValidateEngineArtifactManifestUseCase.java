package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.ArrayList;
import java.util.List;

/** Applies the RC-final rules to the engine artifact manifest. */
public final class ValidateEngineArtifactManifestUseCase {
    public EngineArtifactValidationReport validateForFinalRc(EngineArtifactManifest manifest) {
        List<String> issues = new ArrayList<>();
        if (manifest == null || manifest.artifacts().isEmpty()) {
            issues.add("No existe manifiesto de artefactos de motor.");
            return new EngineArtifactValidationReport(false, issues);
        }
        for (EngineArtifactDescriptor artifact : manifest.requiredForFinalRc()) {
            if (artifact.expectedPath().isBlank()) {
                issues.add(artifact.id() + ": ruta esperada vacía.");
            }
            if (!artifact.hasConcreteSha256()) {
                issues.add(artifact.id() + ": falta SHA-256 concreto para RC final.");
            }
            if (artifact.licensePending()) {
                issues.add(artifact.id() + ": licencia pendiente para RC final.");
            }
        }
        return new EngineArtifactValidationReport(issues.isEmpty(), issues);
    }
}
