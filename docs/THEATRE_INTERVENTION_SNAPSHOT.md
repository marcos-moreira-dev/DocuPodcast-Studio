# Snapshot canónico de intervención

`ResolveTheatreInterventionSnapshotUseCase` es puro respecto del modelo: no usa IA ni filesystem. Resuelve herencia anterior dentro de la misma escena, herencia explícita hacia una intervención anterior y reset. Rechaza ciclos y referencias futuras.

Incluye identidad, secuencia, acto, escena, hablante, voz, targets, personajes, zonas, orientación, mirada, variantes, vestuario, eventos, objetos, portadores, fondo, mapa, cámara, coro, audio, frame, intermedios, assets resueltos y referencias inválidas. Los campos opcionales no configurados quedan vacíos; no se inventan valores.

