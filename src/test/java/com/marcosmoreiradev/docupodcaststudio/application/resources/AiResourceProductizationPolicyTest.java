package com.marcosmoreiradev.docupodcaststudio.application.resources;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AiResourceProductizationPolicyTest {
    @Test
    void rejectsImportableResourceWhenContractHasNoImporter() {
        AiResourceDescriptor descriptor = new AiResourceDescriptor(
                "example", AiResourceKind.MINIMAL_EXAMPLE, "/ai-resources/example.md", "example.md",
                true, "document-contract-v1", "Ejemplo", "Ejemplo"
        );

        AiResourceProductizationPolicy policy = AiResourceProductizationPolicy.withoutMarkdownImporters();

        assertFalse(policy.supportsImport(descriptor));
        assertTrue(policy.importStatus(descriptor).contains("no tiene parser/importador registrado"));
        assertFalse(policy.validate(List.of(descriptor)).isEmpty());
    }

    @Test
    void acceptsImportableResourceOnlyWhenContractIsRegistered() {
        AiResourceDescriptor descriptor = new AiResourceDescriptor(
                "example", AiResourceKind.MINIMAL_EXAMPLE, "/ai-resources/example.md", "example.md",
                true, "document-contract-v1", "Ejemplo", "Ejemplo"
        );

        AiResourceProductizationPolicy policy = AiResourceProductizationPolicy.withImportableContracts(
                List.of("document-contract-v1")
        );

        assertTrue(policy.supportsImport(descriptor));
        assertTrue(policy.validate(List.of(descriptor)).isEmpty());
    }

    @Test
    void templatesMustNotBeImportableEvenWhenContractExists() {
        AiResourceDescriptor descriptor = new AiResourceDescriptor(
                "template", AiResourceKind.AI_TEMPLATE, "/ai-resources/template.md", "template.md",
                true, "document-contract-v1", "Plantilla", "Plantilla"
        );

        AiResourceProductizationPolicy policy = AiResourceProductizationPolicy.withImportableContracts(
                List.of("document-contract-v1")
        );

        assertFalse(policy.validate(List.of(descriptor)).isEmpty());
    }
}
