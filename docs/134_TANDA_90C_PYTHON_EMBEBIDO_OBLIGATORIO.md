# Tanda 90C — Python embebido obligatorio

T90C endurece Coqui/XTTS para que nunca use Python global. El único runtime permitido es el local bajo `tools/xtts-wrapper/.venv/Scripts/python.exe`. Se elimina el fallback a `python` y se agregan guardarraíles fuente.
