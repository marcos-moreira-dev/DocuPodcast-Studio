# Memoria — Tanda 64

Tras T63, dos source tests de playback fallaban porque seguían buscando `playbackBufferPolicy.waitingLabel()` dentro del shell, aunque la responsabilidad pasó al coordinador de playback. T64 corrige esa expectativa y agrega `AudioWorkflowCoordinator` para generación, reanudación, cancelación, jobs persistidos y diagnósticos.
