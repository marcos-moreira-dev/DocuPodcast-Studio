# Estrategia — Tanda 15 Packaging / RC

Esta tanda transforma DocuPodcast Studio de prototipo por tandas a candidato local de app desktop ejecutable. La estrategia sigue la referencia de Domain Model Studio: scripts públicos reducidos, manifests y smoke manual antes de considerar una release candidata válida.

## Secuencia recomendada

1. `scripts\00-verificar-entorno.bat`
2. `scripts\03-verificar-toolchain.bat`
3. `scripts\02-ejecutar-tests.bat`
4. `scripts\13-revalidacion-local-completa.bat`
5. `scripts\14-app-image-completa.bat`
6. Smoke manual.
7. `scripts\16-release-candidate.bat`

## Criterio de aceptación

La app debe abrir, permitir crear/abrir proyecto, importar DOCX, crear guion, generar audio mock o TTS configurado, ver storyboard, reproducir por segmento, exportar paquete y mostrar guía.
