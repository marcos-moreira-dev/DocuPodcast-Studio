"""Build deterministic theatre storyboards from local project references.

This smoke tool intentionally performs no network requests and does not use a
finished frame as input. It turns project assets into sparse line art and lays
them out from the theatre camera cue plus the spatial information in the demo
script. The resulting PNGs are suitable as editable drafts or ControlNet
Scribble inputs.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Final

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps


CANVAS_SIZE: Final[tuple[int, int]] = (960, 540)
WHITE: Final[tuple[int, int, int]] = (255, 255, 255)
INK: Final[tuple[int, int, int]] = (25, 25, 25)
GUIDE: Final[tuple[int, int, int]] = (155, 155, 155)


@dataclass(frozen=True)
class Assets:
    """Absolute paths to the visual references used by the compiler."""

    camera_close: Path
    camera_wide: Path
    captain: Path
    lieutenant: Path
    plane: Path
    route_map: Path
    cow: Path


def require_file(path: Path) -> Path:
    """Return a normalized existing file or raise an actionable error."""

    normalized = path.expanduser().resolve()
    if not normalized.is_file():
        raise FileNotFoundError(f"Missing local storyboard reference: {normalized}")
    return normalized


def crop_white_background(image: Image.Image, tolerance: int = 12) -> Image.Image:
    """Crop a mostly white catalog background without altering the subject."""

    rgb = image.convert("RGB")
    difference = ImageChops.difference(rgb, Image.new("RGB", rgb.size, WHITE))
    mask = difference.convert("L").point(lambda value: 255 if value > tolerance else 0)
    bounds = mask.getbbox()
    return rgb.crop(bounds) if bounds else rgb


def sparse_line_art(
    path: Path,
    *,
    crop_white: bool = True,
    crop_fraction: tuple[float, float, float, float] | None = None,
    guide: bool = False,
) -> Image.Image:
    """Convert a local raster reference into economical monochrome contours."""

    with Image.open(require_file(path)) as source:
        image = source.convert("RGB")

    if crop_white:
        image = crop_white_background(image)
    if crop_fraction is not None:
        left, top, right, bottom = crop_fraction
        width, height = image.size
        image = image.crop(
            (
                round(width * left),
                round(height * top),
                round(width * right),
                round(height * bottom),
            )
        )

    grayscale = ImageOps.grayscale(image).filter(ImageFilter.GaussianBlur(radius=1.8))
    edges = grayscale.filter(ImageFilter.FIND_EDGES)
    edges = ImageOps.autocontrast(edges, cutoff=2)
    inverted = ImageOps.invert(edges)

    if guide:
        result = inverted.point(
            lambda value: 170 if value < 105 else 215 if value < 185 else 255
        ).convert("RGB")
    else:
        result = inverted.point(
            lambda value: 20 if value < 120 else 145 if value < 185 else 255
        ).convert("RGB")
    ImageDraw.Draw(result).rectangle(
        (0, 0, result.width - 1, result.height - 1),
        outline=WHITE,
        width=min(7, max(1, min(result.size) // 25)),
    )
    return result


def resize_contained(image: Image.Image, box: tuple[int, int]) -> Image.Image:
    """Resize an image to fit completely inside a target box."""

    width, height = image.size
    target_width, target_height = box
    scale = min(target_width / width, target_height / height)
    size = (max(1, round(width * scale)), max(1, round(height * scale)))
    return image.resize(size, Image.Resampling.LANCZOS)


def multiply_paste(
    canvas: Image.Image,
    element: Image.Image,
    position: tuple[int, int],
    box: tuple[int, int],
) -> None:
    """Paste black-on-white line art while treating white as transparent."""

    resized = resize_contained(element, box)
    x, y = position
    x = max(0, min(x, canvas.width - resized.width))
    y = max(0, min(y, canvas.height - resized.height))
    region = canvas.crop((x, y, x + resized.width, y + resized.height))
    canvas.paste(ImageChops.multiply(region, resized), (x, y))


def opaque_paste(
    canvas: Image.Image,
    element: Image.Image,
    position: tuple[int, int],
    box: tuple[int, int],
) -> None:
    """Paste foreground line art opaquely so background lines do not cross it."""

    resized = resize_contained(element, box)
    x, y = position
    x = max(0, min(x, canvas.width - resized.width))
    y = max(0, min(y, canvas.height - resized.height))
    canvas.paste(resized, (x, y))


def new_stage(camera: Path) -> Image.Image:
    """Create a sparse stage guide from the exact associated camera plane."""

    camera_lines = sparse_line_art(camera, crop_white=False, guide=True)
    stage = Image.new("RGB", CANVAS_SIZE, WHITE)
    stage.paste(camera_lines.resize(CANVAS_SIZE, Image.Resampling.LANCZOS))
    draw = ImageDraw.Draw(stage)
    draw.rectangle((5, 5, 954, 534), outline=INK, width=3)
    return stage


def draw_screw_with_flower(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    """Draw the intervention's small rusty screw and flower as a readable cue."""

    draw.line((x, y + 12, x, y + 65), fill=INK, width=4)
    draw.rectangle((x - 8, y + 8, x + 8, y + 17), outline=INK, width=3)
    for offset in range(24, 58, 9):
        draw.line((x - 6, y + offset, x + 6, y + offset - 5), fill=INK, width=2)
    for dx, dy in ((0, -9), (9, 0), (0, 9), (-9, 0)):
        draw.ellipse((x + dx - 6, y + dy - 6, x + dx + 6, y + dy + 6), outline=INK, width=2)
    draw.ellipse((x - 4, y - 4, x + 4, y + 4), fill=INK)


def draw_cockpit_guides(draw: ImageDraw.ImageDraw) -> None:
    """Add a minimal theatrical biplane cockpit around two bust references."""

    draw.arc((92, 285, 860, 620), 180, 354, fill=INK, width=6)
    draw.arc((155, 315, 805, 590), 185, 350, fill=GUIDE, width=3)
    draw.line((110, 95, 178, 345), fill=GUIDE, width=3)
    draw.line((850, 95, 782, 345), fill=GUIDE, width=3)
    draw.line((95, 90, 865, 90), fill=GUIDE, width=4)
    draw.line((165, 118, 795, 118), fill=GUIDE, width=2)


def draw_hay_bales(draw: ImageDraw.ImageDraw) -> None:
    """Draw three simple hay-bale shapes used as the rural audience."""

    for index, x in enumerate((660, 735, 810)):
        top = 345 - (index % 2) * 12
        draw.rounded_rectangle((x, top, x + 65, top + 72), 12, outline=INK, width=3)
        for offset in (13, 28, 43, 58):
            draw.line((x + offset, top + 5, x + offset - 8, top + 66), fill=GUIDE, width=1)


def draw_battered_hat(draw: ImageDraw.ImageDraw) -> None:
    """Draw the damaged hat punchline in the foreground."""

    draw.ellipse((405, 456, 555, 500), outline=INK, width=4)
    draw.arc((438, 412, 525, 484), 190, 355, fill=INK, width=4)
    draw.line((445, 450, 515, 443), fill=INK, width=3)
    draw.line((480, 419, 496, 444), fill=INK, width=3)


def build_opening(assets: Assets) -> Image.Image:
    """Build the empty hangar establishing storyboard."""

    canvas = new_stage(assets.camera_close)
    plane = sparse_line_art(assets.plane)
    multiply_paste(canvas, plane, (190, 150), (590, 310))
    draw = ImageDraw.Draw(canvas)
    draw.line((125, 421, 835, 421), fill=GUIDE, width=2)
    draw.rectangle((100, 360, 165, 421), outline=INK, width=2)
    draw.rectangle((800, 345, 860, 421), outline=INK, width=2)
    return canvas


def build_maintenance(assets: Assets) -> Image.Image:
    """Build the two-person maintenance disagreement storyboard."""

    canvas = new_stage(assets.camera_close)
    plane = sparse_line_art(assets.plane)
    captain = sparse_line_art(assets.captain)
    lieutenant = sparse_line_art(assets.lieutenant)
    multiply_paste(canvas, plane, (285, 210), (390, 205))
    opaque_paste(canvas, captain, (115, 112), (275, 350))
    opaque_paste(canvas, lieutenant, (585, 120), (255, 342))
    draw = ImageDraw.Draw(canvas)
    draw_screw_with_flower(draw, 565, 265)
    draw.line((370, 238, 555, 238), fill=GUIDE, width=2)
    return canvas


def build_map_scene(assets: Assets) -> Image.Image:
    """Build the cockpit/map intervention with the panoramic camera cue."""

    canvas = new_stage(assets.camera_wide)
    captain = sparse_line_art(
        assets.captain, crop_fraction=(0.0, 0.0, 1.0, 0.66)
    )
    lieutenant = sparse_line_art(
        assets.lieutenant, crop_fraction=(0.0, 0.0, 1.0, 0.66)
    )
    route_map = sparse_line_art(assets.route_map)
    opaque_paste(canvas, captain, (135, 132), (300, 285))
    opaque_paste(canvas, lieutenant, (520, 125), (290, 295))
    opaque_paste(canvas, route_map, (560, 245), (225, 165))
    draw = ImageDraw.Draw(canvas)
    draw_cockpit_guides(draw)
    draw.line((605, 315, 575, 350), fill=INK, width=3)
    draw.line((750, 315, 780, 350), fill=INK, width=3)
    return canvas


def build_closing(assets: Assets) -> Image.Image:
    """Build the closing rural tableau with the close central camera cue."""

    canvas = new_stage(assets.camera_close)
    plane = sparse_line_art(assets.plane)
    captain = sparse_line_art(assets.captain)
    lieutenant = sparse_line_art(assets.lieutenant)
    cow = sparse_line_art(assets.cow)
    multiply_paste(canvas, plane, (235, 175), (485, 245))
    opaque_paste(canvas, captain, (120, 150), (230, 300))
    opaque_paste(canvas, lieutenant, (410, 160), (215, 290))
    opaque_paste(canvas, cow, (720, 220), (190, 215))
    draw = ImageDraw.Draw(canvas)
    draw_hay_bales(draw)
    draw_battered_hat(draw)
    return canvas


def save_storyboard(image: Image.Image, path: Path) -> None:
    """Save an exact 960x540 RGB PNG after validating its dimensions."""

    path.parent.mkdir(parents=True, exist_ok=True)
    if image.size != CANVAS_SIZE:
        raise ValueError(f"Unexpected storyboard size {image.size}: {path}")
    image.save(path, format="PNG", optimize=True)


def save_region_mask(
    path: Path,
    rectangles: tuple[tuple[int, int, int, int], ...],
    *,
    feather: int = 18,
) -> None:
    """Save a white-on-black attention mask for regional conditioning."""

    mask = Image.new("L", CANVAS_SIZE, 0)
    draw = ImageDraw.Draw(mask)
    for rectangle in rectangles:
        draw.rounded_rectangle(rectangle, radius=24, fill=255)
    if feather > 0:
        mask = mask.filter(ImageFilter.GaussianBlur(radius=feather))
    path.parent.mkdir(parents=True, exist_ok=True)
    mask.save(path, format="PNG", optimize=True)


def remove_white_to_alpha(image: Image.Image, tolerance: int = 18) -> Image.Image:
    """Turn the white catalog background into a softly feathered alpha matte."""

    rgba = image.convert("RGBA")
    red, green, blue, _ = rgba.split()
    minimum = ImageChops.darker(ImageChops.darker(red, green), blue)
    alpha = minimum.point(
        lambda value: 0
        if value >= 255 - tolerance
        else min(255, (255 - value) * 10)
    ).filter(ImageFilter.GaussianBlur(radius=1.2))
    rgba.putalpha(alpha)
    return rgba


def build_stage_plane_background(assets: Assets) -> Image.Image:
    """Composite the exact camera and aircraft references without diffusion."""

    with Image.open(require_file(assets.camera_close)) as camera_source:
        background = camera_source.convert("RGB").resize(
            CANVAS_SIZE, Image.Resampling.LANCZOS
        )
    with Image.open(require_file(assets.plane)) as plane_source:
        plane = remove_white_to_alpha(crop_white_background(plane_source))
    fitted = resize_contained(plane, (570, 315))
    background.paste(fitted, (195, 170), fitted)
    return background


def main() -> None:
    """Generate all four local deterministic storyboard candidates."""

    workspace = Path(__file__).resolve().parents[1]
    examples = workspace / "src/main/resources/examples/aviadores-comicos/assets"
    cameras = workspace / "src/main/resources/images/theatre/cameras"
    output = workspace / "target/experiments/local-ai/aviadores-storyboards-compiled"
    assets = Assets(
        camera_close=cameras / "CERCA_CENTRO_NIVEL.png",
        camera_wide=cameras / "PANORAMICA_CENTRO_NIVEL.png",
        captain=examples / "personajes/capitan_bigote/capitan_bigote_01_frontal.png",
        lieutenant=examples
        / "personajes/teniente_tornillo/teniente_tornillo_01_frontal.png",
        plane=examples / "utileria/avion_tornillo_dorado_01.png",
        route_map=examples / "utileria/obj_mapa_01.png",
        cow=examples / "utileria/animal_vaca_01.png",
    )

    storyboards = {
        "fragmento_01_escena1_hangar_presentacion_storyboard_compilado.png": build_opening(
            assets
        ),
        "fragmento_04_dignidad_mantenimiento_storyboard_compilado.png": build_maintenance(
            assets
        ),
        "fragmento_09_mapa_norte_abajo_storyboard_compilado.png": build_map_scene(
            assets
        ),
        "fragmento_24_cierre_tornillo_dorado_storyboard_compilado.png": build_closing(
            assets
        ),
    }
    for filename, storyboard in storyboards.items():
        target = output / filename
        save_storyboard(storyboard, target)
        print(target)

    masks = output / "masks"
    save_region_mask(masks / "fragmento_04_capitan.png", ((35, 0, 405, 539),))
    save_region_mask(masks / "fragmento_04_teniente.png", ((555, 0, 925, 539),))
    save_region_mask(masks / "fragmento_09_capitan.png", ((80, 80, 475, 480),))
    save_region_mask(masks / "fragmento_09_teniente.png", ((485, 80, 875, 480),))
    save_region_mask(masks / "fragmento_24_capitan.png", ((80, 105, 365, 485),))
    save_region_mask(masks / "fragmento_24_teniente.png", ((360, 105, 660, 485),))

    backgrounds = output / "backgrounds"
    backgrounds.mkdir(parents=True, exist_ok=True)
    stage_plane = backgrounds / "fragmento_04_stage_plane.png"
    save_storyboard(build_stage_plane_background(assets), stage_plane)
    print(stage_plane)


if __name__ == "__main__":
    main()
