package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.ArrayList;
import java.util.List;

/** Builds the auditable packaging plan used by TP5 and TP6 scripts. */
public final class BuildDistributionPackagePlanUseCase {
    public DistributionPackagePlan execute(DistributionPackageKind kind) {
        List<DistributionPackageStep> steps = new ArrayList<>();
        steps.add(step("diagnostic", "Diagnóstico completo", "scripts\\99-diagnostico-completo.bat", "target\\diagnostico-completo", true));
        steps.add(step("runtime-layout", "Verificar runtime layout", "scripts\\29-verificar-runtime-layout.bat", "target\\runtime-layout\\TP3_RUNTIME_LAYOUT_REPORT.md", true));
        steps.add(step("third-party", "Generar manifest de terceros", "scripts\\30-generar-manifest-terceros.bat", "target\\legal\\THIRD_PARTY_MANIFEST.md", true));
        if (kind == DistributionPackageKind.PORTABLE_APP_IMAGE || kind == DistributionPackageKind.WINDOWS_MSI) {
            steps.add(step("app-image", "Generar app-image", "scripts\\14-app-image-completa.bat", "dist\\app-image\\APP_IMAGE_MANIFEST.txt", true));
            steps.add(step("portable-layout", "Preparar carpeta portable", "scripts\\32-preparar-app-portable-layout.bat", "dist\\portable\\PORTABLE_MANIFEST.txt", true));
        }
        if (kind == DistributionPackageKind.WINDOWS_MSI) {
            steps.add(step("msi", "Generar MSI", "scripts\\15-msi-completo.bat", "dist\\installer\\MSI_MANIFEST.txt", false));
        }
        return new DistributionPackagePlan(kind, steps);
    }

    private static DistributionPackageStep step(String id, String title, String command, String evidence, boolean mandatory) {
        return new DistributionPackageStep(id, title, command, evidence, mandatory);
    }
}
