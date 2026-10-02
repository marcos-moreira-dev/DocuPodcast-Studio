package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AbstractLocalEngineAdministrationTest {
    @TempDir Path temporary;

    @Test
    void actionSuccessIsIndependentFromGlobalReadinessAfterTheAction() throws Exception {
        EngineId id = new EngineId("test-engine");
        MediaEngine engine = new MediaEngine() {
            @Override public EngineDescriptor descriptor() {
                return new EngineDescriptor(id, CapabilityId.IMAGE_GENERATION,
                        "Test", "1", "local", Set.of(), true);
            }

            @Override public EngineConfigurationSchema configurationSchema() {
                return new EngineConfigurationSchema(id, List.of());
            }

            @Override public List<EnginePresetDescriptor> presets() {
                return List.of();
            }

            @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
                return EngineReadiness.unavailable(id,
                        "La acción acabó, pero todavía falta otro modelo.", "Importa el modelo.");
            }
        };
        AbstractLocalEngineAdministration administration = new AbstractLocalEngineAdministration(
                engine, new RuntimeAssetCatalog(temporary, Map.of()),
                List.of(new EngineActionDescriptor(EngineActionId.REPAIR, "Reparar", "Prueba",
                        List.of(), false, 0L, Map.of()))) {
            @Override protected List<GenerationArtifact> perform(
                    EngineActionRequest request, ExecutionContext context) {
                return List.of();
            }
        };

        EngineActionResult result = administration.execute(
                new EngineActionRequest(id, EngineActionId.REPAIR, Map.of()),
                ExecutionContext.defaults("action-result"));

        assertTrue(result.success());
        assertFalse(result.readiness().ready());
        assertEquals("UNAVAILABLE", result.diagnostics().get("globalReadiness"));
        assertTrue(result.message().startsWith("La reparación terminó correctamente."));
    }
}
