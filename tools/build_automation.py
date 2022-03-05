import argparse
import pathlib
import tempfile

from gen.gen import run as run_gen

from tools.pom import read_pom_vers
from tools.stdlib import gen_stdlib

ROOT = pathlib.Path(__file__).parent.parent


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-O", "--output-dir",
                        type=pathlib.Path, help="Final output dir",
                        default=".")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory() as tmp_proj:
        tmp_proj = pathlib.Path(tmp_proj)
        proj_data = tmp_proj / "data"
        proj_data.mkdir(parents=True, exist_ok=True)

        mc_ver = run_gen(ROOT / "gen" / "input", proj_data, [ROOT / "gen" / "overlay"])
        mc_ver += "+0"

        gzb_ver, mb_ver = read_pom_vers(ROOT / "pom.xml")
        ver = mc_ver, gzb_ver, mb_ver

        out_gzb = args.output_dir / f"lib.std.mcje.gzb-{mc_ver}-{gzb_ver}.spoofax-language"
        out_gzbc = args.output_dir / f"lib.std.mcje.gzbc-{mc_ver}-{gzb_ver}.spoofax-language"
        out_llmc = args.output_dir / f"lib.std.mcje.llmc-{mc_ver}-{gzb_ver}.spoofax-language"

        gen_stdlib(ver, ROOT, tmp_proj, out_gzb, out_gzbc, out_llmc)
