# Contribuir

- Sigue la [guía de desarrollo](docs/development/build-test.md); requisitos y comandos se mantienen allí.
- Conserva los contratos compartidos y comprueba sus consumidores Theatre, Documentary y Narrative. Una vista especializada no vuelve exclusiva la infraestructura que utiliza.
- Mantén compatibilidad de APIs y formatos de proyecto; los cambios incompatibles requieren diseño y migración explícitos.
- Añade pruebas deterministas y fixtures pequeños. Los motores reales son optativos; no conviertas modelos instalados o una copia hermana en requisito del build.
- Conserva ejemplos Word/teatro, muestras de voz, licencias y recursos asociados. No uses su nombre o antigüedad como criterio de limpieza.
- Escribe resultados experimentales bajo target/experiments; no versiones compilaciones ni credenciales. No modifiques destinos de datos productivos para limpiar el repositorio.
- Revisa git diff antes y después, preservando cambios ajenos y archivos nuevos. No ejecutes git clean -xfd.
- Actualiza el contrato documental vigente, no informes históricos redundantes. Conserva ADR y evidencia todavía necesaria.
