import pathlib
from typing import Optional
import zipfile


def write_dir_to_zip(zf: zipfile.ZipFile, root: pathlib.Path, *, prefix: Optional[str]):
    def iter(path: pathlib.Path):
        if path.is_dir():
            for item in path.iterdir():
                iter(item)
        if path.is_file():
            arcname = path.relative_to(root).as_posix()
            if prefix is not None:
                arcname = f"{prefix}/{arcname}"
            zf.write(path, arcname)

    root = root.resolve()
    if not root.is_dir():
        return
    iter(root)
