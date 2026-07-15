# T114-HF1 — Build verde + no-guion visible mínimo

Hotfix conservadora posterior a RF1. Corrige source tests que seguían buscando orquestación de exportación dentro de `DocuPodcastShellViewModel`, cuando ahora corresponde a `ExportWorkflowCoordinator`.

También evita que los flujos tocados activen o persistan workspaces legacy como `SCRIPT_EDITOR` o `STORYBOARD`, y cambia mensajes visibles urgentes de “guion” a “lectura preparada/documento”.

Documento principal: `docs/productizacion/T114_HF1_BUILD_VERDE_NO_GUION_VISIBLE.md`.
