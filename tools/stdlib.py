import itertools
import pathlib
import subprocess
import tempfile
import zipfile
from xml.etree import ElementTree

from gen.gen import run as run_gen


def read_pom_props(pom_path):
    pom = ElementTree.parse(pom_path).getroot()
    pom_props = pom.find("{http://maven.apache.org/POM/4.0.0}properties[1]")
    pom_revision = pom_props.find("{http://maven.apache.org/POM/4.0.0}revision").text
    pom_mb_ver = pom_props.find("{http://maven.apache.org/POM/4.0.0}metaborgVersion").text
    return pom_revision or "0.1.0-SNAPSHOT", pom_mb_ver or "2.6.0-SNAPSHOT"


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


def spoofax_language(root: pathlib.Path, gzb_rev: str, cat: str, name: str):
    return root / cat / f"{cat}.{name}" / "target" / f"{cat}.{name}-{gzb_rev}.spoofax-language"


def gen_stdlib(ver: str, root: pathlib.Path, proj_wd: pathlib.Path, out_gzb: pathlib.Path, out_gzbc: pathlib.Path):
    gzb_rev, mb_ver = read_pom_props(root / "pom.xml")

    spoofax_languages = [
        spoofax_language(root, gzb_rev, "lang", "gazebo"),
        spoofax_language(root, gzb_rev, "lang", "gazebo-core"),
        spoofax_language(root, gzb_rev, "ext", "gzb2gzbc"),
    ]

    proj_data = proj_wd / "data"
    proj_data.mkdir(parents=True, exist_ok=True)
    run_gen(root / "gen" / "input", proj_data, [root / "gen" / "overlay"])

    subprocess.run([
        str(root / "gazebo.standalone" / "run.sh"),
        str(proj_wd),
        *itertools.chain(*zip(
            itertools.repeat("--language-archive"),
            spoofax_languages
        )),
        "--internal-action=STDLIB"
    ])

    # TODO: LEFTOFF: package into .spoofax-language
    #       make sure that the different .stxlib files are not used by both languages! might not be possible at all
    #       probably have to emit 2 .spoofax-langauge archives, but it looks like AProjectResourcesPrimitive
    #       does not respect language contribution configuration

    stxlib_out_base = proj_wd / "target" / "gazebo-standalone" / "task" / "emit-stxlib"
    stxlib_gzb = stxlib_out_base / "gazebo.stxlib"
    stxlib_gzbc = stxlib_out_base / "gazebo-core.stxlib"

    with zipfile.ZipFile(out_gzb, "w", zipfile.ZIP_DEFLATED) as f:
        f.writestr("src-gen/metaborg.component.yaml", MB_COMP_TEMPLATE_GZB.format(metaborgVersion=mb_ver, gazeboVersion=gzb_rev))
        f.writestr("lib/stxlibs", '["std-gzb"]')
        f.write(stxlib_gzb, "lib/std-gzb.stxlib")

    with zipfile.ZipFile(out_gzbc, "w", zipfile.ZIP_DEFLATED) as f:
        f.writestr("src-gen/metaborg.component.yaml", MB_COMP_TEMPLATE_GZBC.format(metaborgVersion=mb_ver, gazeboVersion=gzb_rev))
        f.writestr("lib/stxlibs", '["std-gzbc"]')
        f.write(stxlib_gzbc, "lib/std-gzbc.stxlib")


if __name__ == "__main__":

    root = pathlib.Path(__file__).parent.parent

    with tempfile.TemporaryDirectory() as tmp_proj:
        tmp_proj = pathlib.Path(tmp_proj)
        gen_stdlib("1.17.1+0", root, tmp_proj, pathlib.Path("./out-gzb.spoofax-language"), pathlib.Path("./out-gzbc.spoofax-language"))

    # gen_stdlib("1.17.1+0", root, pathlib.Path("./proj_wd").resolve(), pathlib.Path("./out-gzb.spoofax-language"), pathlib.Path("./out-gzbc.spoofax-language"))
