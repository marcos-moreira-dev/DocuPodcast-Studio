package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Product-level release gate names used by TP6 documentation and scripts. */
public record ReleaseCandidateGate(String id, String label, boolean mandatory, String evidencePath) {
    public static List<ReleaseCandidateGate> defaultGates() {
        return List.of(
                new ReleaseCandidateGate("diagnostic", "Diagnóstico completo verde", true, "target/diagnostico-completo"),
                new ReleaseCandidateGate("visual-smoke", "Smoke visual UX T112 revisado", true, "docs/productizacion/T112_SMOKE_VISUAL_REPORTE_MANUAL.md"),
                new ReleaseCandidateGate("runtime-layout", "Runtime layout TP3 verificado", true, "target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md"),
                new ReleaseCandidateGate("third-party", "Manifest de terceros TP4 generado", true, "target/legal/THIRD_PARTY_MANIFEST.md"),
                new ReleaseCandidateGate("app-image", "App-image portable TP5 generado", true, "dist/app-image/DocuPodcastStudio"),
                new ReleaseCandidateGate("real-engines", "Smoke motores reales opt-in", false, "target/real-engines-smoke")
        );
    }
}
