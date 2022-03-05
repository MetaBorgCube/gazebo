import itertools
import pathlib
import subprocess
from typing import Optional


def _call(cmd: list):
    # make sure only string parts remain
    cmd = [*map(str, cmd)]
    print("calling Gazebo Standalone:", " ".join(cmd))
    subprocess.run(cmd)


def gzbs_run_gradle(
        root: pathlib.Path,
        *args,
):
    _call([
        root / "gazebo.standalone" / "run-gradle.sh",
        *args,
    ])


def gzbs_run_cli(
        root: pathlib.Path,
        proj_wd=None,
        compress: Optional[bool] = None,
        language_archives: Optional[list] = None,
        internal_action: Optional[str] = None,
):
    _call([
        root / "gazebo.standalone" / "run-cli.sh",
        *([proj_wd] if proj_wd is not None else []),
        *(["--compress"] if compress else []),
        *(itertools.chain(*zip(
            itertools.repeat("--language-archive"),
            language_archives
        )) if language_archives is not None else []),
        *(["--internal-action", internal_action] if internal_action is not None else []),
    ])
