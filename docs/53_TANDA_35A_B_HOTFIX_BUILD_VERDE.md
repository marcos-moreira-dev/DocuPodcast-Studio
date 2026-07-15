# Tanda 35A-B — Hotfix build verde

## Objetivo

Dejar verde la Tanda 35A sin introducir funcionalidades nuevas ni volver a convertir la bienvenida en una pantalla técnica.

## Problema detectado

La Tanda 35A compilaba, pero fallaba un guardarraíl fuente:

```text
VisibleActionContractSourceTest.welcomeMentionsMarkdownImportAsGuidedStepNotFakeButton
```

El guardarraíl exige que la bienvenida recuerde la importación Markdown como paso guiado u opción avanzada real, no como botón falso ni promesa pendiente.

## Cambio aplicado

Se agregó en `WelcomeWorkspaceView` una nota secundaria:

```text
Opcional avanzado: importa guion Markdown docupodcast-script-v1 desde Guion cuando ya tengas un guion preparado.
```

Esto conserva la alineación de producto de Tanda 35A:

```text
Word/DOCX primero
Documento narrado como experiencia principal
Markdown solo como opción avanzada de intercambio/guion
configuraciones pesadas fuera de la pantalla principal
```

## Alcance

No se modificó audio, exportación, storyboard, voces, jobs ni persistencia.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Resultado esperado:

```text
BUILD SUCCESS
```
