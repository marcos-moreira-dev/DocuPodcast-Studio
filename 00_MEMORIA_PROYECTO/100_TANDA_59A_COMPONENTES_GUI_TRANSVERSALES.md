# Memoria — Tanda 59A

Después de fijar principios rectores en T59, T59A introduce el primer guardarraíl ejecutable contra scaffolding visual disperso.

Decisión cerrada: los workspaces no deben crear botoneras repetidas con `new Button(...)` si existe componente compartido. Las acciones pasan por `ActionButtonFactory`, `ActionBar` y `TransportControls`.

La complejidad del producto se conserva, pero la superficie visual empieza a quedar gobernada por componentes transversales.
