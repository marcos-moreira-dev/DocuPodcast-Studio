# Roadmap posterior a T81B

## Estado después de T81B

T81B deja tres correcciones importantes listas:

1. Configuración ya no se promociona en la bienvenida.
2. Configuración no aparece en toolbar ni como acción principal fuera del menú.
3. El documento central ya no imprime metadatos técnicos del importador debajo de cada bloque.

La app sigue conservando el estilo visual base, pero la jerarquía de interacción aún no está terminada.

## Próxima tanda recomendada: T81C — Barra flotante de lectura global

Objetivo: agregar una barra pequeña sobre la hoja, debajo de la toolbar, que controle la lectura del documento completo.

Estados previstos:

```text
[▶ Iniciar lectura en voz alta]
[▶ Preparar y escuchar]
[⏸ Pausar] [⏹ Detener]
[▶ Reanudar] [⏹ Detener]
```

Criterio: esta barra controla el documento completo, no la oración seleccionada.

## T81D — Sidebar izquierdo contextual

Objetivo: mover operaciones frecuentes de fragmento al inspector izquierdo, con módulos simples:

- Detalles;
- Audio / Narración;
- Imagen.

En Audio/Narración debe usarse la nomenclatura acordada:

- `Voz IA`;
- `Audio del computador`;
- acciones directas: `Elegir audio…`, `Extraer audio de video…`.

Emoción/estilo solo se habilita con `Voz IA`.

## T81E — Sidebar derecho de miniaturas / medios

Objetivo: convertir el panel derecho en navegación visual. Cada tarjeta debe mostrar:

- miniatura;
- fragmento relacionado;
- breve descripción o nombre de archivo.

Clic en miniatura debe llevar al fragmento del documento.

## T81F — Toolbar con iconos y grupos

Objetivo: reemplazar la barra textual saturada por acciones compactas con iconos y grupos:

- Documento;
- Lectura;
- Vista;
- Exportar.

No deben aparecer como acciones permanentes:

- Narración avanzada;
- Voces;
- Audio;
- Storyboard;
- Jobs;
- Diagnóstico.

## T81G — Retiro/degradación de vistas secundarias redundantes

Objetivo: sacar de navegación principal los workspaces que compiten con Documento. El cerebro se conserva; se reduce la exposición visual.

Candidatos:

- Narración interna;
- Audio jobs;
- Voces;
- Storyboard;
- Reproducción técnica.

Deben quedar en Herramientas avanzadas, Configuración o desaparecer como superficie visible si ya no aportan al flujo V1.
