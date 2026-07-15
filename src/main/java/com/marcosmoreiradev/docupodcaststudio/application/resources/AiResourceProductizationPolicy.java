package com.marcosmoreiradev.docupodcaststudio.application.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Product guardrail for AI resource metadata.
 *
 * <p>A resource may only be marked as importable when the application has a real
 * parser/importer chain for its declared contract. Until that chain exists,
 * resources can still be exported as references, prompts, examples or templates,
 * but the index must not present them as importable by the app.</p>
 */
public final class AiResourceProductizationPolicy {
    private final Set<String> importableContracts;

    private AiResourceProductizationPolicy(Collection<String> importableContracts) {
        Objects.requireNonNull(importableContracts, "importableContracts");
        TreeSet<String> safeContracts = new TreeSet<>();
        for (String contract : importableContracts) {
            if (contract != null && !contract.isBlank()) {
                safeContracts.add(contract.trim());
            }
        }
        this.importableContracts = Set.copyOf(safeContracts);
    }

    /** Product state without Markdown importers enabled. */
    public static AiResourceProductizationPolicy withoutMarkdownImporters() {
        return new AiResourceProductizationPolicy(List.of());
    }

    /** General factory for registered Markdown import contracts. */
    public static AiResourceProductizationPolicy withImportableContracts(Collection<String> importableContracts) {
        return new AiResourceProductizationPolicy(importableContracts);
    }

    public boolean supportsImport(AiResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return descriptor.importable()
                && !descriptor.contract().isBlank()
                && importableContracts.contains(descriptor.contract());
    }

    public String importStatus(AiResourceDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (!descriptor.importable()) {
            return "No importable: recurso de referencia, plantilla, prompt o ejemplo hasta que exista parser/importador real.";
        }
        if (descriptor.contract().isBlank()) {
            return "No importable: no declara contrato de importacion.";
        }
        if (!importableContracts.contains(descriptor.contract())) {
            return "No importable: el contrato " + descriptor.contract() + " no tiene parser/importador registrado.";
        }
        return "Importable: existe parser/importador registrado para " + descriptor.contract() + ".";
    }

    public List<String> validate(Collection<AiResourceDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        List<String> violations = new ArrayList<>();
        for (AiResourceDescriptor descriptor : descriptors) {
            if (!descriptor.importable()) {
                continue;
            }
            if (descriptor.contract().isBlank()) {
                violations.add(descriptor.id() + " is marked importable but does not declare a contract.");
                continue;
            }
            if (!importableContracts.contains(descriptor.contract())) {
                violations.add(descriptor.id() + " is marked importable for unsupported contract " + descriptor.contract() + ".");
            }
            if (descriptor.kind() == AiResourceKind.AI_TEMPLATE) {
                violations.add(descriptor.id() + " is a template and must not be marked importable.");
            }
        }
        return List.copyOf(violations);
    }
}
