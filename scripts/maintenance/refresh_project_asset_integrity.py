from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from datetime import datetime
from pathlib import Path


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Synchronize the canonical theatre Markdown and refresh project asset hashes."
    )
    parser.add_argument("project_root", type=Path)
    parser.add_argument("--package", required=True)
    args = parser.parse_args()

    root = args.project_root.resolve()
    project_file = root / "obra.docupodcast.json"
    canonical_markdown = root / "source" / "obra.teatro.md"
    package_root = root / "assets" / "theatre" / args.package
    package_markdown = package_root / "obra.teatro.md"
    package_manifest_file = package_root / "docupodcast-theatre.json"

    required = [project_file, canonical_markdown, package_markdown, package_manifest_file]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing required files: " + ", ".join(missing))

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_root = root / "backups" / f"asset-refresh-{stamp}"
    backup_root.mkdir(parents=True, exist_ok=False)
    shutil.copy2(project_file, backup_root / project_file.name)
    shutil.copy2(package_markdown, backup_root / "package-obra.teatro.md")
    shutil.copy2(package_manifest_file, backup_root / package_manifest_file.name)

    shutil.copy2(canonical_markdown, package_markdown)

    project = json.loads(project_file.read_text(encoding="utf-8"))
    assets = project.get("assets", {}).get("items", [])
    changed: list[dict[str, str]] = []
    for asset in assets:
        relative = asset.get("relativePath", "")
        path = root.joinpath(*relative.split("/"))
        if not path.is_file():
            raise FileNotFoundError(f"Missing catalogued asset {asset.get('id')}: {relative}")
        actual = sha256(path)
        previous = str(asset.get("checksum", "")).removeprefix("sha256:").lower()
        if previous != actual:
            changed.append({"id": str(asset.get("id", "")), "path": relative})
            asset["checksum"] = actual

    project_file.write_text(
        json.dumps(project, ensure_ascii=False, indent=4) + "\n", encoding="utf-8"
    )

    manifest = json.loads(package_manifest_file.read_text(encoding="utf-8"))
    for asset in manifest.get("assets", []):
        relative = str(asset.get("path", ""))
        path = package_root.joinpath(*relative.split("/"))
        if not path.is_file():
            raise FileNotFoundError(
                f"Missing package asset {asset.get('logicalId')}: {relative}"
            )
        asset["sha256"] = sha256(path)
        asset["size"] = path.stat().st_size
    package_manifest_file.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=4) + "\n", encoding="utf-8"
    )

    print(f"Backup: {backup_root}")
    print(f"Synchronized Markdown: {package_markdown}")
    print(f"Refreshed project checksums: {len(changed)}")
    for item in changed:
        print(f"  {item['id']}: {item['path']}")
    print(f"Verified package assets: {len(manifest.get('assets', []))}")


if __name__ == "__main__":
    main()
