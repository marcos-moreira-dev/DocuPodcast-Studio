# Estándares pendientes y tandas restantes — continuidad RC

Este archivo es el complemento operativo del plan en piedra. Su función es evitar que la continuidad dependa del contexto de chat.

## Base vigente

```text
Última base estable validada localmente:
DocuPodcast-Studio-DEMO-PLAY-BODY-BINDINGS-DOCX-HEADING-HF1-v1.zip

Diagnóstico completo:
OK

Demo teatral:
estable
```

## Regla de autoridad

Para retomar implementación:

```text
1. Leer DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md.
2. Leer este archivo.
3. Leer DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md.
4. Ejecutar scripts\99-diagnostico-completo.bat antes de modificar.
5. Elegir la siguiente tanda pendiente en el orden recomendado.
```

## Orden restante recomendado

```text
01. RUNTIME-PATHS-RF1
02. EXTERNAL-PROCESS-RUNNER-RF1
03. EXTERNAL-PROCESS-EXCEPTIONS-HF1
04. MODEL-ARTIFACT-CONTRACT-RF1
05. MANAGED-DOWNLOAD-RF1
06. ENGINE-READINESS-UI-HF1
07. XTTS-PYTORCH-CUDA-INSTALL-HF1
08. RUNTIME-ARCH-RC1
09. COMMAND-AUDIT-RC1
10. RIBBON-CATALOG-RF1
11. SETTINGS-SPLIT-RF1
12. GUI-COMPONENTS-RF1
13. VOICE-WORKSPACE-SPLIT-RF1
14. NO-HARDCODED-JAVAFX-RC1
15. VOICE-REC-HF1
16. DOC-VISUAL-RAIL-RC1
17. OPERATIONAL-MICROCOPY-HF1
18. PERSISTENCE-RC1
19. VOICE-SAMPLES-PATH-HF1
20. PROJECT-ASSET-REPAIR-HF1
21. LEGACY-PROJECT-MIGRATION-HF1
22. STATUSBAR-HOVER-TEST1
23. ENGINE-CATALOG-TEST1
24. XTTS-PATH-TEST1
25. GPU-SMOKE-TEST1
26. VOICE-WORKSPACE-UX-TEST1
27. EXPORT-READINESS-TEST1
28. DIAGNOSTIC-SCRIPTS-RC1
29. PACKAGING-MEMORY-RC1
30. RC-GATE1
31. AUDIO-UNIT-MODEL-POSTRC
```

Estado actualizado:

```text
RUNTIME-PATHS-RF1, EXTERNAL-PROCESS-RUNNER-RF1 y la integración operativa inicial de excepciones de proceso quedaron avanzadas en RUNTIME-PROCESOS-INTEGRACION1.
La siguiente tanda debe partir de esa base y no reintroducir ProcessBuilder directo ni rutas operativas duplicadas.
```

## Dependencias fuertes

```text
RUNTIME-PATHS-RF1
  desbloquea EXTERNAL-PROCESS-RUNNER-RF1, MODEL-ARTIFACT-CONTRACT-RF1, MANAGED-DOWNLOAD-RF1, XTTS-PYTORCH-CUDA-INSTALL-HF1, PACKAGING-MEMORY-RC1.

EXTERNAL-PROCESS-RUNNER-RF1
  desbloquea EXTERNAL-PROCESS-EXCEPTIONS-HF1, RUNTIME-ARCH-RC1, DIAGNOSTIC-SCRIPTS-RC1.

MODEL-ARTIFACT-CONTRACT-RF1
  desbloquea ENGINE-READINESS-UI-HF1 y RC-GATE1.

ENGINE-READINESS-UI-HF1
  debe preceder a COMMAND-AUDIT-RC1 si comandos críticos dependen de motores.

PERSISTENCE-RC1
  debe preceder a RC-GATE1.
```

## Estándares transversales que no se pueden romper

```text
- Nada visible sin propósito operativo.
- Nada de dashboard de relleno.
- Ningún comando visible sin handler.
- Ningún motor visible como usable si no pasó readiness real.
- Ningún fallback defensivo silencioso.
- Ninguna ruta crítica duplicada si ya existe RuntimeArtifactPaths.
- Ningún ProcessBuilder nuevo fuera del runner cuando se implemente EXTERNAL-PROCESS-RUNNER-RF1.
- Ningún cambio de formato de proyecto sin migración explícita.
```

## Token CSS corregido

Se agrega alias:

```css
-dp-text-secondary: -docu-text-muted;
```

Motivo:

```text
Evitar warning JavaFX en .example-project-readiness al abrir ejemplos.
```
