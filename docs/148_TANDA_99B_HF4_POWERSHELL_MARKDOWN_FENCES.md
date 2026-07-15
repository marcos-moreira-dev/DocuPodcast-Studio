# Tanda 99B-HF4 - PowerShell Markdown fences

Hotfix sobre T99B-HF3. Corrige parser errors en Windows PowerShell 5.1 causados por cercas Markdown escritas con comillas dobles en scripts `.ps1`.

Archivos principales:

- `scripts/tts/preflight-startup-engines.ps1`
- `scripts/tts/preflight-piper-ffmpeg.ps1`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/scripts/ScriptsRootSafeSourceTest.java`
- `docs/productizacion/T99B_HF4_POWERSHELL_MARKDOWN_FENCES.md`

Validacion esperada local:

```bat
scripts\99-diagnostico-completo.bat
```

Siguiente tanda recomendada: T99C - Deshuesadero visual minimo.
