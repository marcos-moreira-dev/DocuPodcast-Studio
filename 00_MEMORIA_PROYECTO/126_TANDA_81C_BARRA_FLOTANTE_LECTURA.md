# Memoria — Tanda 81C

Se incorporó `FloatingReadingControlBar` como componente transversal. `DocumentWorkspaceView` lo instala con `documentSurface.setTop(floatingReadingControl())`, evitando que la acción primaria de lectura quede mezclada con el cuerpo de la hoja. Se corrigió el test de importación de muestras de voz para el contrato de navegación limpia: la capacidad sigue disponible, pero no exige menú principal `Voz`.
