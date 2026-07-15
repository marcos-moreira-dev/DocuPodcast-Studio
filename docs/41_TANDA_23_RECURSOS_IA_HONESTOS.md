# Tanda 23 — Recursos IA honestos

## Objetivo

Alinear los recursos IA con el estado real del producto: DocuPodcast exporta gramaticas, plantillas, prompts, ejemplos y referencias, pero todavia no tiene un parser/importador Markdown `docupodcast-script-v1`.

Por eso, ningun recurso oficial debe presentarse como importable por la aplicacion hasta que exista la cadena completa:

```text
parser Markdown
  -> workspace editable
  -> revision humana
  -> guardado .docupodcast.json
  -> exportacion/roundtrip verificado
```

## Cambios principales

```text
AiResourceProductizationPolicy
AiResourceProductizationPolicyTest
AiResourcesImportabilityTest
OfficialAiResourceCatalog sin recursos importables por ahora
Ejemplos oficiales con importable: false
Indice de recursos IA con estado de importacion explicito
Ayuda de recursos IA actualizada
```

## Decisión de producto

Los recursos IA siguen siendo valiosos, pero se declaran como referencia hasta que Tanda 24 implemente el importador real.

Esto evita que el usuario crea que puede importar Markdown `docupodcast-script-v1` si la app aun no tiene parser.

## Validación esperada

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```

En este entorno no se ejecuto Maven porque `mvn` no esta instalado; se valido con `javac` parcial y revision estatica.

## Siguiente tanda

Tanda 24 — Importador Markdown de guion narrable, o decision de mantener Markdown como referencia no importable.
