# JavaFX design system and usage policy

The desktop UI keeps JavaFX controls for their keyboard, focus and accessibility semantics, but
product surfaces must apply the shared DocuPodcast visual system. A native control is not a visual
exception merely because its implementation class belongs to JavaFX.

## Allowed native exceptions

- Short decisions may use the conventional JavaFX confirmation button bar through
  `NativeDialogResponse`/`NativeDecisionDialog`: accept, cancel, yes/no and destructive confirmation.
- `FileChooser` and `DirectoryChooser` are created only by `NativeSourceChooser` and remain
  operating-system surfaces.
- The exception covers the buttons and chooser chrome. Dialog content still receives the
  `product-dialog` class, ownership and focus behavior.
- Workspaces, side-docks, ribbons, administration, editors and long-running operations may not use
  visually default action controls.

## Official components

- `ActionButtonFactory` owns product action variants.
- `StudioFormControls` constructs text, numeric, selection, color, toggle and slider controls.
- `StudioCollectionControls` constructs lists, trees and tables.
- `StudioNavigationControls` constructs tabs and menus.
- `StudioFeedbackControls` constructs determinate and indeterminate progress controls.
- `StudioViewportControls` constructs scroll and split panes, including canvas-safe variants.
- `StudioDialogShell` owns product dialog content while leaving response ButtonBars native.
- `StudioAccordion` owns `Accordion` and `TitledPane` construction.
- `CollapsibleSection` is the lighter disclosure component for inspectors and dialogs.
- `StudioCanvasToolbar` owns the compact, stacked toolbar surface shared by canvas editors. It
  groups mode, appearance, view and contextual actions without owning domain state; editors keep
  their own behaviour and persistence.

Every official control stores a `StudioControlDescriptor` in its JavaFX properties. The descriptor
records family, variant, density, accessible name and help. CSS is anchored to official classes;
the slider skin, in particular, never targets private tracks belonging to unrelated controls. The
stylesheet deliberately does not target `.button`, so native confirmation bars retain the platform
appearance described above.

The official slider may color its own track and thumb, but never sets dimensions on those private
skin nodes. Width and height belong to the public slider variant or its product layout; this keeps
compact controls such as the status-bar reading zoom bounded. `COMPACT`, `REGULAR` and `EDITOR`
share native `SliderSkin` input semantics and add only a bounded value-fill layer. Check boxes and
radio buttons deliberately do not receive `ui-form-control`: their root stays transparent and only
the box/radio, mark, focus and state are decorated.

Accordions use a purple header (`-docu-accordion-header`), white title/arrow and white expanded
content. Hover and keyboard focus use a darker purple and a visible border. Direct construction of
`Accordion` or `TitledPane` outside `StudioAccordion` is rejected by the build.

## Product-complexity composition

`ProjectExperience` declares extensible `ProductRequirementId` values. Infrastructure and visual
composition must consume these requirements instead of inferring needs from historical workspace
identifiers. Current requirements cover paged documents, context rails, production boards, ink,
candidate review, persistent/recoverable jobs and long-running operations.

Persisted `SCRIPT_EDITOR`, `AUDIO_JOBS` and `STORYBOARD` routes are interpreted only by
`LegacyWorkspaceRouteMapper`. They cannot register a product workspace or provision infrastructure.

## Status bar and ink surfaces

The status bar preserves the compact reference interaction model: the operational message owns the
flexible space inside an invisible horizontal `ScrollPane`, contextual generation actions are direct
buttons, and document progress plus reading zoom remain anchored at the right. Long messages are
never ellipsized; wheel input over the message scrolls it horizontally. There is no intermediary
audio menu.

Reusable drawing infrastructure belongs to `studio-ink`. `InkCanvasViewport` owns the transparent
input target, target-to-canvas coordinate mapping and the current logical bounds. A `GROWING`
profile treats its declared dimensions as the initial minimum and may add tiles; only a `FIXED`
profile enforces its declared boundary. `InkEditorSession` owns provider lifetime, normalized and raw
pressure, zoom coordinate resets, history, restoration and export. Product dialogs retain only their
shell, overlays, image tools and product-specific persistence.

`InkCanvasZoomPane` is the single zoom/scroll implementation for technical problems, free
composition and theatrical frames. Its extent is `logical canvas size × zoom` and never depends on
the current viewport. Continuous slider changes are coalesced to one layout update per JavaFX pulse,
the logical center is restored after layout, and input coordinates are reset once after stabilization.

Canvas selection and transforms use the deterministic `InkSelectionGeometry` engine from
`studio-ink`. Hit testing derives an internal safety band from stroke width/spacing, closed
contours are explicit fill candidates, and translation, scale and rotation operate on vector
points before the surface is rebuilt. Raster region copy/move remains the compatibility fallback
for content that has no vector representation.

Right side-docks are coordinated in pixels by `SideDockSplitCoordinator`; collapsed rails use
74/78/84 px, or 88/92/96 px when they contain a footer. The rail touches the outer workspace edge
and expanded content grows toward the document. Hidden alternative docks are unmanaged and do not
contribute to layout.

## Automated audit

`GuiComponentUsagePolicyTest` inspects constructor calls in compiled bytecode, scans production
sources and writes
`target/reports/gui-usage/gui-component-usage.md`.

The build enforces:

- zero direct construction of audited JavaFX controls outside the visual-system package;
- `Alert`/`ButtonType` and source chooser construction only at their explicit native boundaries;
- runtime metadata on logical product controls, excluding skin internals and native ButtonBars;
- a generated breakdown by factory, entrypoint, surface and exception.

There is no remaining debt allowance. Receiving an accidental global CSS rule does not satisfy the
contract.
