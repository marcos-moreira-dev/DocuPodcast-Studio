# Tanda 15 — Packaging / Release Candidate

## Estado

Implementada como cierre de MVP técnico. Esta tanda no intenta declarar el producto final como comercialmente terminado; prepara scripts, documentación, smoke manual y manifests para generar una app-image o MSI localmente.

## Alcance implementado

- Scripts root-safe para revalidación, app-image, MSI, release candidate y JavaDoc.
- `02-ejecutar-tests.bat` ahora muestra confirmación visible `Tests OK` cuando Maven termina sin fallos.
- Documentación de smoke manual de release candidate.
- Reporte manual editable para registrar resultados.
- Tests fuente para asegurar que los scripts de packaging existen, resuelven la raíz del repo y usan `jpackage`.

## Scripts nuevos

```text
scripts/13-revalidacion-local-completa.bat
scripts/14-app-image-completa.bat
scripts/15-msi-completo.bat
scripts/16-release-candidate.bat
scripts/31-generar-javadoc.bat
```

## Decisiones

- El build sigue usando Java 21 Eclipse Temurin mediante Maven Toolchains.
- `mvn -version` puede mostrar otro JDK como runtime de Maven, pero la compilación formal debe seleccionar Temurin 21.
- El motor TTS real no se empaqueta todavía como modelo/binario pesado dentro del repo.
- La app-image o MSI producidos deben probarse manualmente antes de distribuirse.

## Resultado esperado local

```bat
scripts\13-revalidacion-local-completa.bat
scripts\14-app-image-completa.bat
scripts\16-release-candidate.bat
```

Salida esperada:

```text
dist/app-image/DocuPodcastStudio
dist/app-image/APP_IMAGE_MANIFEST.txt
dist/release-candidate/RELEASE_CANDIDATE_MANIFEST.txt
```

## Pendiente posterior

- Polish visual profundo: barra de ventana, espaciados, iconos, pantalla de inicio más refinada, statusbar rica.
- MSI firmado/no firmado según disponibilidad de WiX/jpackage.
- Empaquetado opcional de runtime TTS real.
- Instalador final con recursos visuales propios.
