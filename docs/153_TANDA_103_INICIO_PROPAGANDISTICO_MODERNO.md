# Tanda 103 — Inicio propagandistico moderno

T103 rediseña la vista Inicio como portada de producto moderna y sobria. La pantalla queda orientada a la promesa principal: abrir un documento, escucharlo desde una hoja limpia y agregar capas solo cuando una oracion lo necesita.

## Alcance

- Hero moderno con promesa clara del producto.
- Tarjetas compactas del flujo: abre, escucha, selecciona una oracion y exporta.
- Panel de alcance V1 con reglas honestas de fuente documental.
- Panel de recientes como empty state, sin simular historial inexistente.
- Formas geometricas decorativas generadas con JavaFX.

## Fuera de alcance

- Historial real de proyectos recientes.
- Cambios en Ribbon, StatusBar o Documento.
- Configuracion, motores, STT/Whisper o jobs.
- Nueva logica de proyecto o persistencia.

## Validacion

Se agrego `WelcomeModernHomeT103SourceTest` para proteger que Inicio use componentes transversales, mantenga la promesa de producto y no vuelva a superficies tecnicas.
