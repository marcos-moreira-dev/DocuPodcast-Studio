# Estrategia — T81C-HF1

La estrategia del hotfix es no revertir el rediseño. La barra flotante de lectura es correcta porque acerca la acción global al documento sin llenar la hoja de controles. Los tests deben validar el componente transversal, no forzar que `DocumentWorkspaceView` vuelva a contener internamente cada botón.
