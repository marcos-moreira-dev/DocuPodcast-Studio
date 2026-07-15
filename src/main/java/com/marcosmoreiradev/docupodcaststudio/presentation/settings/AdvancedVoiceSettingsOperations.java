package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectComputeEnvironmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.PrepareXttsPytorchCudaUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.RunXttsCudaSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaPreparationReport;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ConfirmXttsSmokePlaybackUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.DownloadXttsOfficialModelUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImportXttsModelFolderUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectXttsSetupReadinessUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.PrepareXttsPortableRuntimeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.RunXttsReadinessSmokeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.SelectXttsAsEngineUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsModelDownloadReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsModelImportReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsRuntimePreparationReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsSetupReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.XttsSmokeTestReport;
import com.marcosmoreiradev.docupodcaststudio.application.services.SettingsApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.presentation.process.FxBackgroundTaskRunner;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.nio.file.Path;

/** Coordinates advanced voice and CUDA settings operations without owning the Settings dialog layout. */
final class AdvancedVoiceSettingsOperations {
    private final FxBackgroundTaskRunner backgroundTaskRunner;
    private final SettingsOperationProgressCoordinator operationProgress;
    private final ConfirmationPrompt confirmationPrompt;
    private final Runnable refresh;
    private final PiperAfterAdvancedPrompt piperAfterAdvancedPrompt;

    AdvancedVoiceSettingsOperations(FxBackgroundTaskRunner backgroundTaskRunner,
                                    SettingsOperationProgressCoordinator operationProgress,
                                    ConfirmationPrompt confirmationPrompt,
                                    Runnable refresh,
                                    PiperAfterAdvancedPrompt piperAfterAdvancedPrompt) {
        this.backgroundTaskRunner = backgroundTaskRunner;
        this.operationProgress = operationProgress;
        this.confirmationPrompt = confirmationPrompt;
        this.refresh = refresh;
        this.piperAfterAdvancedPrompt = piperAfterAdvancedPrompt;
    }

    void prepareCuda(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                     ComputeEnvironmentReport currentReport, Node anchor) {
        OperationalSettings settings = form.toSettings();
        ComputeEnvironmentReport report = services == null
                ? (currentReport == null
                ? new InspectComputeEnvironmentUseCase().inspect(settings)
                : currentReport)
                : services.inspectComputeEnvironment().inspect(settings);
        OperationProgress progress = operationProgress.show(anchor,
                "Preparando CUDA para Voz IA avanzada",
                "Instalando PyTorch CUDA en Python local",
                "La app reinstalara PyTorch CUDA dentro del Python autocontenido de Voz IA avanzada. No se usara Python global.");
        progress.update("Iniciando instalacion CUDA local. Esta descarga puede tardar varios minutos...");
        backgroundTaskRunner.start("preparar-cuda-voz-ia-avanzada", () -> {
            try {
                PrepareXttsPytorchCudaUseCase useCase = services == null
                        ? new PrepareXttsPytorchCudaUseCase()
                        : services.prepareXttsPytorchCuda();
                XttsCudaPreparationReport preparation = useCase.prepare(applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                if (!preparation.success()) {
                    Platform.runLater(() -> {
                        String detail = preparation.compactOutput();
                        String message = preparation.userMessage()
                                + " Log: " + preparation.logFile()
                                + (detail.isBlank() ? "" : " Detalle: " + SettingsDialog.friendlyEngineText(detail));
                        progress.fail(message);
                        refresh.run();
                    });
                    return;
                }
                RunXttsCudaSmokeUseCase smokeUseCase = services == null
                        ? new RunXttsCudaSmokeUseCase()
                        : services.runXttsCudaSmoke();
                XttsCudaSmokeReport smoke = smokeUseCase.run(settings, report, applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> {
                    progress.finish(preparation.userMessage() + " " + describeCudaSmoke(smoke));
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    progress.fail("No se pudo preparar CUDA para Voz IA avanzada: " + SettingsDialog.friendlyEngineText(ex.getMessage()));
                    refresh.run();
                });
            }
        });
    }

    void runCudaSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot,
                      ComputeEnvironmentReport currentReport, Node anchor) {
        OperationalSettings settings = form.toSettings();
        ComputeEnvironmentReport report = services == null
                ? (currentReport == null
                ? new InspectComputeEnvironmentUseCase().inspect(settings)
                : currentReport)
                : services.inspectComputeEnvironment().inspect(settings);
        OperationProgress progress = operationProgress.show(anchor,
                "Probando GPU de Voz IA avanzada",
                "Verificando CUDA en Python local",
                "La app comprobará si el Python autocontenido de Voz IA avanzada puede importar PyTorch y usar CUDA. No se usará Python global.");
        progress.update("Ejecutando smoke CUDA local...");
        backgroundTaskRunner.start("prueba-gpu-voz-ia-avanzada", () -> {
            try {
                RunXttsCudaSmokeUseCase useCase = services == null
                        ? new RunXttsCudaSmokeUseCase()
                        : services.runXttsCudaSmoke();
                XttsCudaSmokeReport result = useCase.run(settings, report, applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> {
                    String message = describeCudaSmoke(result);
                    if (result.gpuUsableForXtts()) {
                        progress.finish(message);
                    } else {
                        progress.finish(message + " En Automático Voz IA avanzada usará CPU; en Dispositivo específico se intentará el dispositivo solicitado y el runtime informará si no lo soporta.");
                    }
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo probar GPU para Voz IA avanzada: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void verify(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        InspectXttsSetupReadinessUseCase inspector = services == null
                ? new InspectXttsSetupReadinessUseCase()
                : services.inspectXttsSetupReadiness();
        XttsSetupReadinessReport report = inspector.inspect(form.toSettings(), applicationRoot);
        status.setText(describeReadiness(report, form));
    }

    void confirmAndPrepare(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (!confirmationPrompt.confirm(status, "Preparar Voz IA avanzada", "Preparar voz de alta calidad",
                "La app revisará lo necesario, descargará recursos si faltan y dejará la voz seleccionada cuando quede lista.\n\n"
                        + "Puede tardar varios minutos y requiere Internet.")) {
            status.setText("Preparación cancelada. No se modificó la configuración.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Preparando Voz IA avanzada",
                "Preparando este equipo",
                "La app preparará recursos locales de voz dentro del programa y verificará que puedas escuchar documentos.");
        progress.update("Revisando componentes locales...");
        status.setText("Preparando Voz IA avanzada dentro del programa...");
        backgroundTaskRunner.start("preparando-voz-ia-avanzada", () -> {
            try {
                PrepareXttsPortableRuntimeUseCase prepareUseCase = services == null
                        ? new PrepareXttsPortableRuntimeUseCase()
                        : services.prepareXttsPortableRuntime();
                XttsRuntimePreparationReport preparationReport = prepareUseCase.prepare(form.toSettings(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                InspectXttsSetupReadinessUseCase inspector = services == null
                        ? new InspectXttsSetupReadinessUseCase()
                        : services.inspectXttsSetupReadiness();
                XttsSetupReadinessReport readiness = inspector.inspect(form.toSettings(), applicationRoot);
                XttsModelDownloadReport downloadReport = null;
                if (!readiness.canBeSelectedAsEngine()) {
                    DownloadXttsOfficialModelUseCase downloader = services == null
                            ? new DownloadXttsOfficialModelUseCase()
                            : services.downloadXttsOfficialModel();
                    downloadReport = downloader.download(form.toSettings(), applicationRoot,
                            message -> progress.update(SettingsDialog.friendlyProgress(message)));
                    readiness = inspector.inspect(form.toSettings(), applicationRoot);
                }
                XttsSetupReadinessReport finalReadiness = readiness;
                XttsModelDownloadReport finalDownloadReport = downloadReport;
                XttsRuntimePreparationReport finalPreparationReport = preparationReport;
                Platform.runLater(() -> {
                    if (finalReadiness.canBeSelectedAsEngine()) {
                        String message = selectAndSave(form, services);
                        status.setText(message);
                        progress.finish(message);
                        refresh.run();
                        piperAfterAdvancedPrompt.prompt(form, services, applicationRoot, status);
                    } else {
                        String message = finalDownloadReport == null
                                ? "Voz IA avanzada aún requiere preparación: " + summarizeMissingDetailed(finalReadiness) + "." + runtimePreparationReportDetail(finalPreparationReport)
                                : describeAdvancedVoiceSetupFailure(finalDownloadReport, finalReadiness, finalPreparationReport);
                        status.setText(message);
                        progress.fail(message);
                        refresh.run();
                    }
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo completar la preparación automática: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void confirmAndDownloadModel(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        if (!confirmationPrompt.confirm(status, "Descargar Voz IA avanzada", "Descargar recursos de voz de alta calidad",
                "La descarga es grande, requiere Internet y quedará guardada dentro del programa.\n\n"
                        + "Al terminar se verificará y se seleccionará automáticamente si está lista.")) {
            status.setText("Descarga cancelada. No se modificó la configuración.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Descargando Voz IA avanzada",
                "Descarga local en curso",
                "La app está descargando recursos de voz y verificará el resultado al terminar.");
        progress.update("Iniciando descarga de recursos de voz...");
        status.setText("Descargando Voz IA avanzada... Puede tardar bastante según tu conexión.");
        backgroundTaskRunner.start("descarga-modelo-voz-ia", () -> {
            try {
                DownloadXttsOfficialModelUseCase downloader = services == null
                        ? new DownloadXttsOfficialModelUseCase()
                        : services.downloadXttsOfficialModel();
                XttsModelDownloadReport report = downloader.download(form.toSettings(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                InspectXttsSetupReadinessUseCase inspector = services == null
                        ? new InspectXttsSetupReadinessUseCase()
                        : services.inspectXttsSetupReadiness();
                XttsSetupReadinessReport readiness = inspector.inspect(form.toSettings(), applicationRoot);
                Platform.runLater(() -> {
                    String message;
                    if (readiness.canBeSelectedAsEngine()) {
                        message = selectAndSave(form, services);
                        progress.finish(message);
                        piperAfterAdvancedPrompt.prompt(form, services, applicationRoot, status);
                    } else if (report.success()) {
                        message = "Descarga completada, pero Voz IA avanzada aún no está lista: "
                                + summarizeMissingDetailed(readiness) + "." + diagnosticReportDetail(report);
                        progress.finish(message);
                    } else {
                        message = describeAdvancedVoiceSetupFailure(report, readiness, null);
                        progress.fail(message);
                    }
                    status.setText(message);
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo completar la descarga de voz: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void importModelFolder(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona la carpeta de recursos de voz");
        java.io.File selected = chooser.showDialog(status.getScene() == null ? null : status.getScene().getWindow());
        if (selected == null) {
            status.setText("Importación cancelada. Selecciona una carpeta válida de Voz IA avanzada.");
            return;
        }
        OperationProgress progress = operationProgress.show(status,
                "Importando Voz IA avanzada",
                "Copiando y verificando recursos",
                "La app copiará la carpeta seleccionada dentro del programa y comprobará que pueda usarse.");
        progress.update("Validando carpeta seleccionada antes de copiar...");
        status.setText("Importando Voz IA avanzada...");
        backgroundTaskRunner.start("importando-modelo-voz-ia", () -> {
            try {
                ImportXttsModelFolderUseCase importer = services == null
                        ? new ImportXttsModelFolderUseCase()
                        : services.importXttsModelFolder();
                XttsModelImportReport report = importer.importFrom(selected.toPath(), form.toSettings(), applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                InspectXttsSetupReadinessUseCase inspector = services == null
                        ? new InspectXttsSetupReadinessUseCase()
                        : services.inspectXttsSetupReadiness();
                XttsSetupReadinessReport readiness = inspector.inspect(form.toSettings(), applicationRoot);
                Platform.runLater(() -> {
                    String message;
                    if (report.success() && readiness.canBeSelectedAsEngine()) {
                        message = selectAndSave(form, services);
                        progress.finish(message);
                    } else if (report.success()) {
                        message = "Importación completada. Aún falta preparar: " + summarizeMissingDetailed(readiness) + ".";
                        progress.finish(message);
                    } else {
                        message = SettingsDialog.friendlyEngineText(report.userMessage());
                        progress.fail(message);
                    }
                    status.setText(message);
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo importar Voz IA avanzada: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void runReadinessSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        InspectXttsSetupReadinessUseCase inspector = services == null
                ? new InspectXttsSetupReadinessUseCase()
                : services.inspectXttsSetupReadiness();
        XttsSetupReadinessReport readiness = inspector.inspect(form.toSettings(), applicationRoot);
        if (!readiness.canBeSelectedAsEngine()) {
            status.setText("No se puede probar Voz IA avanzada todavía: " + summarizeMissingDetailed(readiness) + ".");
            return;
        }
        String selectedMessage = selectAndSave(form, services);
        OperationalSettings smokeSettings = OperationalSettingsMigrationPolicy.repair(form.toSettings());
        form.load(smokeSettings);
        OperationProgress progress = operationProgress.show(status,
                "Probando Voz IA avanzada",
                "Generando WAV real de prueba",
                "La app ejecutará el motor local con una frase corta. Este paso valida que la descarga no solo exista, sino que el motor pueda producir audio real.");
        progress.update("Preparando prueba WAV real...");
        status.setText("Probando Voz IA avanzada... " + selectedMessage);
        backgroundTaskRunner.start("prueba-voz-ia-avanzada", () -> {
            try {
                RunXttsReadinessSmokeUseCase smoke = services == null
                        ? new RunXttsReadinessSmokeUseCase()
                        : services.runXttsReadinessSmoke();
                XttsSmokeTestReport report = smoke.run(smokeSettings, applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> {
                    String message = describeSmoke(report);
                    status.setText(message);
                    if (report.generatedWavProof()) {
                        progress.finish(message);
                    } else {
                        progress.fail(message);
                    }
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo probar Voz IA avanzada: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void playAndConfirmSmoke(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        OperationProgress progress = operationProgress.show(status,
                "Reproduciendo prueba de Voz IA avanzada",
                "Confirmando audio de prueba",
                "La app reproducirá el WAV de prueba y marcará la voz como confirmada solo si el audio termina dentro del reproductor interno.");
        progress.update("Buscando prueba WAV generada...");
        status.setText("Reproduciendo prueba de Voz IA avanzada...");
        backgroundTaskRunner.start("reproduciendo-prueba-voz-ia", () -> {
            try {
                ConfirmXttsSmokePlaybackUseCase confirm = services == null
                        ? new ConfirmXttsSmokePlaybackUseCase()
                        : services.confirmXttsSmokePlayback();
                XttsSmokeTestReport report = confirm.confirm(applicationRoot,
                        message -> progress.update(SettingsDialog.friendlyProgress(message)));
                Platform.runLater(() -> {
                    String message = describeSmoke(report);
                    status.setText(message);
                    if (report.fullyVerified()) {
                        progress.finish(message);
                    } else {
                        progress.fail(message);
                    }
                    refresh.run();
                });
            } catch (RuntimeException ex) {
                Platform.runLater(() -> {
                    String message = "No se pudo reproducir la prueba de Voz IA avanzada: " + SettingsDialog.friendlyEngineText(ex.getMessage());
                    status.setText(message);
                    progress.fail(message);
                    refresh.run();
                });
            }
        });
    }

    void selectIfReady(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Label status) {
        InspectXttsSetupReadinessUseCase inspector = services == null
                ? new InspectXttsSetupReadinessUseCase()
                : services.inspectXttsSetupReadiness();
        XttsSetupReadinessReport report = inspector.inspect(form.toSettings(), applicationRoot);
        if (!report.canBeSelectedAsEngine()) {
            status.setText("Voz IA avanzada aún requiere preparación: " + summarizeMissingDetailed(report) + ".");
            return;
        }
        String message = selectAndSave(form, services);
        status.setText(message);
        refresh.run();
    }

    String selectAndSave(SettingsFormModel form, SettingsApplicationServices services) {
        SelectXttsAsEngineUseCase selector = services == null
                ? new SelectXttsAsEngineUseCase()
                : services.selectXttsAsEngine();
        OperationalSettings selected = selector.select(form.toSettings());
        form.load(selected);
        if (services != null) {
            try {
                services.saveOperationalSettings().save(selected);
                return "Voz IA avanzada lista y seleccionada como motor principal.";
            } catch (IOException ex) {
                return "Voz IA avanzada lista. No se pudo guardar automáticamente; pulsa Guardar cambios.";
            }
        }
        return "Voz IA avanzada lista y seleccionada en pantalla.";
    }

    static String describeCudaSmoke(XttsCudaSmokeReport report) {
        if (report == null) {
            return "Prueba CUDA pendiente.";
        }
        if (report.gpuUsableForXtts()) {
            String device = report.deviceName().isBlank() ? report.deviceArgument() : report.deviceName();
            String torch = report.torchVersion().isBlank() ? "PyTorch" : "PyTorch " + report.torchVersion();
            String cuda = report.torchCudaVersion().isBlank() ? "CUDA" : "CUDA " + report.torchCudaVersion();
            return "GPU confirmada para Voz IA avanzada: " + device + " · " + torch + " · " + cuda + ".";
        }
        String issues = report.issues().isEmpty() ? "" : " Detalle: " + SettingsDialog.friendlyEngineText(String.join(" · ", report.issues())) + ".";
        return SettingsDialog.friendlyEngineText(report.userMessage().isBlank() ? report.statusLabel() : report.userMessage()) + issues;
    }

    static String describeReadiness(XttsSetupReadinessReport report, SettingsFormModel form) {
        if (report == null) {
            return "No se pudo verificar Voz IA avanzada.";
        }
        if (report.ready()) {
            return SettingsDialog.friendlyEngineText(report.userMessage()) + " Dispositivo: " + form.currentComputeDeviceLabel() + ".";
        }
        String warnings = report.warnings().isEmpty() ? "" : " Advertencias: " + String.join(" · ", report.warnings()) + ".";
        return SettingsDialog.friendlyEngineText(report.userMessage()) + " Faltante: " + summarizeMissing(report) + "." + SettingsDialog.friendlyEngineText(warnings);
    }

    static String describeSmoke(XttsSmokeTestReport report) {
        if (report == null) {
            return "No se recibió resultado de prueba de Voz IA avanzada.";
        }
        if (report.generatedWavProof()) {
            String playback = report.playbackConfirmed()
                    ? " Reproducción confirmada dentro de la app."
                    : " Falta reproducir una prueba desde la app antes de documentos largos.";
            return SettingsDialog.friendlyEngineText(report.userMessage()) + " WAV: " + report.audioFile().getFileName()
                    + " (" + report.outputBytes() + " bytes)." + playback;
        }
        String issues = report.issues().isEmpty() ? "" : " Detalle: " + String.join(" · ", report.issues()) + ".";
        return SettingsDialog.friendlyEngineText(report.userMessage()) + SettingsDialog.friendlyEngineText(issues);
    }

    static String summarizeMissing(XttsSetupReadinessReport report) {
        if (report == null || report.missingRequirements().isEmpty()) {
            return "sin componentes pendientes";
        }
        int count = report.missingRequirements().size();
        return count == 1 ? "1 componente pendiente" : count + " componentes pendientes";
    }

    static String summarizeMissingDetailed(XttsSetupReadinessReport report) {
        if (report == null || report.missingRequirements().isEmpty()) {
            return "sin componentes pendientes";
        }
        return report.missingRequirements().stream()
                .limit(4)
                .map(SettingsDialog::friendlyEngineText)
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    private static String describeXttsDownload(XttsModelDownloadReport report) {
        if (report == null) {
            return "No se recibió resultado de descarga.";
        }
        String failures = report.failedFiles().isEmpty()
                ? ""
                : " Pendiente: " + friendlyDownloadFailures(report.failedFiles()) + ".";
        String inspection = report.inspection() == null ? "" : " Verificación: " + SettingsDialog.friendlyEngineText(report.inspection().userMessage());
        return SettingsDialog.friendlyEngineText(report.userMessage())
                + " Recursos descargados: " + report.downloadedFiles().size()
                + ", ya disponibles: " + report.skippedFiles().size()
                + "." + failures + inspection + diagnosticReportDetail(report);
    }

    private static String describeAdvancedVoiceSetupFailure(XttsModelDownloadReport report, XttsSetupReadinessReport readiness,
                                                            XttsRuntimePreparationReport preparationReport) {
        if (downloadedModelIsUsableButRuntimePending(report, readiness)) {
            return "Modelo de voz descargado correctamente. Falta preparar el entorno local de ejecución: "
                    + summarizeMissingDetailed(readiness) + "."
                    + runtimePreparationReportDetail(preparationReport, readiness)
                    + diagnosticReportDetail(report);
        }
        if (report != null && report.success() && readiness != null && readiness.modelUsable() && !readiness.canBeSelectedAsEngine()) {
            return "Modelo de voz descargado correctamente. No quedó seleccionable porque falta completar: "
                    + summarizeMissingDetailed(readiness) + "."
                    + runtimePreparationReportDetail(preparationReport, readiness)
                    + diagnosticReportDetail(report);
        }
        String download = describeXttsDownload(report);
        String missing = readiness == null || readiness.canBeSelectedAsEngine()
                ? ""
                : " Falta completar: " + summarizeMissingDetailed(readiness) + "." + runtimePreparationReportDetail(preparationReport, readiness);
        String warnings = readiness == null || readiness.warnings().isEmpty()
                ? ""
                : " Advertencias: " + SettingsDialog.friendlyEngineText(String.join(" · ", readiness.warnings())) + ".";
        return download + missing + warnings;
    }

    private static boolean downloadedModelIsUsableButRuntimePending(XttsModelDownloadReport report, XttsSetupReadinessReport readiness) {
        return report != null
                && report.success()
                && report.inspection() != null
                && report.inspection().usable()
                && readiness != null
                && readiness.modelUsable()
                && !readiness.runtimeReady()
                && !readiness.canBeSelectedAsEngine();
    }

    private static String runtimePreparationReportDetail(XttsRuntimePreparationReport preparationReport) {
        return runtimePreparationReportDetail(preparationReport, preparationReport == null ? null : preparationReport.readinessAfter());
    }

    private static String runtimePreparationReportDetail(XttsRuntimePreparationReport preparationReport, XttsSetupReadinessReport readiness) {
        Path report = preparationReport == null ? null : preparationReport.setupReport();
        if (report == null && readiness != null && readiness.applicationRoot() != null) {
            report = XttsRuntimePreparationReport.defaultReportPath(readiness.applicationRoot());
        }
        if (report == null) {
            return "";
        }
        return " Reporte de preparación local: " + report.toAbsolutePath().normalize() + ".";
    }

    private static String diagnosticReportDetail(XttsModelDownloadReport report) {
        if (report == null || report.diagnosticReport() == null) {
            return "";
        }
        return " Reporte técnico para soporte: " + report.diagnosticReport().toAbsolutePath().normalize()
                + ". Si necesitas ayuda, adjunta el reporte técnico.";
    }

    private static String friendlyDownloadFailures(java.util.List<String> failedFiles) {
        return SettingsTechnicalMessageHumanizer.downloadFailures(failedFiles);
    }

    @FunctionalInterface
    interface ConfirmationPrompt {
        boolean confirm(Node anchor, String title, String header, String message);
    }

    @FunctionalInterface
    interface PiperAfterAdvancedPrompt {
        void prompt(SettingsFormModel form, SettingsApplicationServices services, Path applicationRoot, Node anchor);
    }
}
