package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.presentation.components.StudioNavigationControls;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.SemanticActionIcons;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Shared action order and labels for Word and PDF document selections. */
final class DocumentSelectionContextMenuFactory {
    private DocumentSelectionContextMenuFactory() { }

    static ContextMenu create(boolean secondaryComponent,
                              boolean hasDescription,
                              Runnable narrate,
                              Runnable copy,
                              Runnable viewDescription,
                              Runnable editDescription) {
        MenuItem narrateItem = item("Narrar desde aquí", narrate);
        MenuItem copyItem = item("Copiar texto", copy);
        if (!secondaryComponent) {
            return StudioNavigationControls.contextMenu(narrateItem, copyItem);
        }
        MenuItem viewItem = item("Ver contenido y descripción…", viewDescription);
        MenuItem editItem = item(hasDescription
                ? "Editar descripción manualmente…"
                : "Definir descripción manualmente…", editDescription);
        return StudioNavigationControls.contextMenu(
                narrateItem, copyItem, new SeparatorMenuItem(), viewItem, editItem);
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        SemanticActionIcons.decorate(item);
        item.setOnAction(event -> {
            if (action != null) action.run();
        });
        item.setDisable(action == null);
        return item;
    }
}
