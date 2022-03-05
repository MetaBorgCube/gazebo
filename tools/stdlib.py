import pathlib
import zipfile
from typing import Tuple

from tools.gzbs import gzbs_run_cli
from tools.zip import write_dir_to_zip

MB_COMP_TEMPLATE_GZB = """---
metaborgVersion: "{metaborgVersion}"
contributions:
- name: "gazebo"
  id: "nl.jochembroekhoff.gazebo:lang.gazebo:${{gazeboVersion}}"
gazeboVersion: "{gazeboVersion}"
name: "gazebo-lib-std-mcje-gzb"
id: "nl.jochembroekhoff.gazebo:lib.std.mcje.gzb:${{gazeboVersion}}"
dependencies:
  compile:
  - "nl.jochembroekhoff.gazebo:lang.gazebo:${{gazeboVersion}}"
  source: []
exports:
- includes:
  - "stxlibs"
  - "*.stxlib"
  directory: "./lib"
- language: gazebo
  includes:
  - "**/*.gzb"
  directory: "./data"
"""


MB_COMP_TEMPLATE_GZBC = """---
metaborgVersion: "{metaborgVersion}"
contributions:
- name: "gazebo-core"
  id: "nl.jochembroekhoff.gazebo:lang.gazebo-core:${{gazeboVersion}}"
gazeboVersion: "{gazeboVersion}"
name: "gazebo-lib-std-mcje-gzbc"
id: "nl.jochembroekhoff.gazebo:lib.std.mcje.gzbc:${{gazeboVersion}}"
dependencies:
  compile:
  - "nl.jochembroekhoff.gazebo:lang.gazebo-core:${{gazeboVersion}}"
  source: []
exports:
- includes:
  - "stxlibs"
  - "*.stxlib"
  directory: "./lib"
- language: gazebo-core
  includes:
  - "**/*.gzbc"
  directory: "./src-gen/gzb-interm"
"""


MB_COMP_TEMPLATE_LLMC = """---
metaborgVersion: "{metaborgVersion}"
contributions:
- name: "llmc"
  id: "nl.jochembroekhoff.gazebo:lang.llmc:${{gazeboVersion}}"
gazeboVersion: "{gazeboVersion}"
name: "gazebo-lib-std-mcje-llmc"
id: "nl.jochembroekhoff.gazebo:lib.std.mcje.llmc:${{gazeboVersion}}"
dependencies:
  compile:
  - "nl.jochembroekhoff.gazebo:lang.llmc:${{gazeboVersion}}"
  source: []
exports:
- language: llmc
  includes:
  - "**/*.llmc"
  directory: "./src-gen/llmc-interm"
"""


def _spoofax_language(root: pathlib.Path, gzb_rev: str, cat: str, name: str):
    direct_compile_artifact = root / cat / f"{cat}.{name}" / "target" / f"{cat}.{name}-{gzb_rev}.spoofax-language"
    if direct_compile_artifact.exists():
        return direct_compile_artifact
    else:
        return root / "misc" / "target" / "out-lang" / f"{cat}.{name}-{gzb_rev}.spoofax-language"


def gen_stdlib(ver: Tuple[str, str, str], root: pathlib.Path, proj_wd: pathlib.Path, out_gzb: pathlib.Path, out_gzbc: pathlib.Path, out_llmc: pathlib.Path):
    ver, gzb_rev, mb_ver = ver

    spoofax_languages = [
        _spoofax_language(root, gzb_rev, "lang", "gazebo"),
        _spoofax_language(root, gzb_rev, "lang", "gazebo-core"),
        _spoofax_language(root, gzb_rev, "lang", "llmc"),
        _spoofax_language(root, gzb_rev, "ext", "gzb2gzbc"),
        _spoofax_language(root, gzb_rev, "ext", "gzbc2llmc"),
    ]

    gzbs_run_cli(
        root,
        proj_wd=proj_wd,
        language_archives=spoofax_languages,
        internal_action="STDLIB",
    )

    # TODO: LEFTOFF: package into .spoofax-language
    #       make sure that the different .stxlib files are not used by both languages! might not be possible at all
    #       probably have to emit 2 .spoofax-langauge archives, but it looks like AProjectResourcesPrimitive
    #       does not respect language contribution configuration

    stxlib_out_base = proj_wd / "target" / "gazebo-standalone" / "task" / "emit-stxlib"
    stxlib_gzb = stxlib_out_base / "gazebo.stxlib"
    stxlib_gzbc = stxlib_out_base / "gazebo-core.stxlib"

    with zipfile.ZipFile(out_gzb, "w", zipfile.ZIP_DEFLATED) as f:
        f.writestr("src-gen/metaborg.component.yaml", MB_COMP_TEMPLATE_GZB.format(metaborgVersion=mb_ver, gazeboVersion=gzb_rev))
        f.writestr("lib/stxlibs", f'["std-gzb-{ver}"]')
        f.write(stxlib_gzb, f"lib/std-gzb-{ver}.stxlib")
        write_dir_to_zip(f, proj_wd / "data", prefix="data")

    with zipfile.ZipFile(out_gzbc, "w", zipfile.ZIP_DEFLATED) as f:
        f.writestr("src-gen/metaborg.component.yaml", MB_COMP_TEMPLATE_GZBC.format(metaborgVersion=mb_ver, gazeboVersion=gzb_rev))
        f.writestr("lib/stxlibs", f'["std-gzbc-{ver}"]')
        f.write(stxlib_gzbc, f"lib/std-gzbc-{ver}.stxlib")
        write_dir_to_zip(f, proj_wd / "src-gen" / "gzb-interm", prefix="src-gen/gzb-interm")

    with zipfile.ZipFile(out_llmc, "w", zipfile.ZIP_DEFLATED) as f:
        f.writestr("src-gen/metaborg.component.yaml", MB_COMP_TEMPLATE_LLMC.format(metaborgVersion=mb_ver, gazeboVersion=gzb_rev))
        # no stxlibs
        write_dir_to_zip(f, proj_wd / "src-gen" / "llmc-interm", prefix="src-gen/llmc-interm")
