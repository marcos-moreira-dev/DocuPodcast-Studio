package com.marcosmoreiradev.docupodcaststudio.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.CacheMode;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = ArchitectureBoundaryTest.BASE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class,
        cacheMode = CacheMode.FOREVER
)
final class ArchitectureBoundaryTest {
    static final String BASE_PACKAGE = "com.marcosmoreiradev.docupodcaststudio";

    @ArchTest
    static final ArchRule domain_is_independent = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..",
                    "..infrastructure..",
                    "..presentation..",
                    "javafx.."
            );

    @ArchTest
    static final ArchRule application_uses_ports_not_adapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..",
                    "..presentation..",
                    "javafx.."
            );

    @ArchTest
    static final ArchRule presentation_does_not_construct_infrastructure = noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule javafx_stays_in_presentation_and_composition = noClasses()
            .that().resideOutsideOfPackages(
                    "..presentation..",
                    "..bootstrap..",
                    "..ink..",
                    BASE_PACKAGE
            )
            .should().dependOnClassesThat().resideInAnyPackage("javafx..");

    @ArchTest
    static final ArchRule documentary_video_plans_do_not_use_theatre = noClasses()
            .that().resideInAPackage("..application.documentstudy..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.theatre..",
                    "..domain.theatre..",
                    "..presentation.theatre.."
            );

    @ArchTest
    static final ArchRule narrative_video_plans_do_not_use_theatre = noClasses()
            .that().resideInAPackage("..application.narrative..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.theatre..",
                    "..domain.theatre..",
                    "..presentation.theatre.."
            );

    @ArchTest
    static final ArchRule narrative_video_export_does_not_use_theatre = noClasses()
            .that().resideInAPackage("..application.video..")
            .and().haveSimpleNameContaining("Narrative")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.theatre..",
                    "..domain.theatre..",
                    "..presentation.theatre.."
            );

    @ArchTest
    static final ArchRule visual_core_does_not_depend_on_consumers = noClasses()
            .that().resideInAPackage("..application.visual..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.theatre..",
                    "..domain.theatre..",
                    "..application.documentstudy..",
                    "..domain.study..",
                    "..application.narrative.."
            );

    @ArchTest
    static final ArchRule generic_visual_engine_contracts_do_not_depend_on_consumers = noClasses()
            .that().haveSimpleNameContaining("LocalVisualImageEngineManager")
            .or().haveSimpleNameContaining("ImageEngineSmokeRequest")
            .or().haveSimpleNameContaining("ImageEnginePresetSupport")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.theatre..",
                    "..domain.theatre..",
                    "..application.documentstudy..",
                    "..domain.study..",
                    "..application.narrative.."
            );

    @ArchTest
    static final ArchRule desktop_layers_do_not_depend_on_local_media_adapters = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..", "..presentation..")
            .should().dependOnClassesThat().resideInAPackage("..localmedia..");

    @ArchTest
    static final ArchRule transversal_ink_does_not_depend_on_document_product = noClasses()
            .that().resideInAPackage("..presentation.ink..")
            .should().dependOnClassesThat().resideInAPackage("..presentation.document..");

    @ArchTest
    static final ArchRule administrative_shell_does_not_know_product_modules = noClasses()
            .that().resideInAPackage("..presentation.components.admin..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..presentation.voice..",
                    "..presentation.theatre..",
                    "..presentation.settings..");

    @ArchTest
    static final ArchRule presentation_does_not_use_root_application_locator = noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices");

    @ArchTest
    static final ArchRule engine_administration_is_confined_to_settings = noClasses()
            .that().resideInAPackage("..presentation..")
            .and().resideOutsideOfPackage("..presentation.settings..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.media.api.EngineAdministration")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.media.api.EngineAdministrationRegistry")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.presentation.settings.EngineAdministrationPane")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.presentation.settings.EngineAdministrationController");

    @ArchTest
    static final ArchRule neutral_media_service_does_not_know_process_or_http_protocols = noClasses()
            .that().resideInAPackage("..application.media..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.process..", "java.net.http..");

    @ArchTest
    static final ArchRule content_analysis_consumers_are_confined_to_document_study = noClasses()
            .that().resideOutsideOfPackages(
                    "..application.document..", "..application.documentstudy..",
                    "..application.media..",
                    "..media.api..",
                    "..localmedia..")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    "com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest");
}
