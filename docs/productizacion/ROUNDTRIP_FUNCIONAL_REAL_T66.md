# Round-trip funcional real — Tanda 66

## Propósito

La Tanda 66 convierte el round-trip en una pieza ejecutable del cerebro de DocuPodcast, no solo en un smoke manual o en una promesa documental.

El objeto padre sigue siendo el **Documento narrable**. El guion se mantiene como **proyección interna de narración** para TTS, playback, Markdown compatible y diagnóstico avanzado, pero no como raíz paralela frente al usuario normal.

## Contrato de round-trip

Un proyecto DocuPodcast debe poder pasar por este ciclo sin pérdida funcional:

```text
abrir/importar documento fuente solo lectura
→ construir Documento narrable
→ crear proyección interna de narración
→ asignar capas narrativas
→ asociar imagen/storyboard
→ persistir jobs de audio
→ guardar proyecto
→ cerrar
→ reabrir
→ recuperar documento, proyección, capas, storyboard, assets y jobs
```

## Qué cubre T66

T66 agrega `ProjectRoundTripUseCase`, `ProjectRoundTripRequest` y `ProjectRoundTripResult` en la capa `application.project`.

El caso de uso:

1. materializa `document/document.json` y copia la fuente en `source/`;
2. materializa `script/narration-script.json` como proyección interna;
3. materializa `storyboard/storyboard.json`;
4. persiste snapshots de jobs de audio bajo `jobs/`;
5. guarda `.docupodcast.json` con assets y capas;
6. reabre el proyecto desde disco;
7. rehidrata artefactos de workspace;
8. lista jobs de audio persistidos;
9. devuelve un resultado verificable.

## Regla de fuente inmutable

El round-trip nunca edita ni sobrescribe Word/DOCX, PDF, Markdown/MD o TXT. Si el usuario modifica la fuente en otra aplicación, el camino correcto es **Refrescar contenido**, no editar la fuente desde DocuPodcast.

## Alcance honesto

T66 no rediseña la interfaz. Tampoco garantiza todavía que todos los flujos visuales estén pulidos. Su objetivo es que el cerebro tenga una cadena ejecutable para verificar persistencia y reapertura de artefactos críticos.

## Criterio de salida

La tanda queda lista si:

- `ProjectRoundTripUseCaseTest` demuestra persistencia y reapertura de Documento narrable, proyección interna, capas, imagen, storyboard y job de audio.
- `BrainRoundTripFunctionalSourceTest` protege el contrato de producto.
- `scripts\02-ejecutar-tests.bat` pasa en local.
