import itertools
import json
import pathlib
from collections import defaultdict
from dataclasses import dataclass
from typing import Any, Union, Iterable, List, Dict, DefaultDict, Tuple

Paths = Dict[int, List[str]]

OVERLAY_HEADER = "/* OVERLAY */\n\n"


def flat(notflat):
    return itertools.chain.from_iterable(notflat)


def _mkdirs(path: pathlib.Path):
    path.mkdir(parents=True, exist_ok=True)


@dataclass
class NbtDoc:
    registries: dict
    root_modules: dict
    compounds: list
    enums: list
    modules: list


@dataclass
class McData:
    registries: dict
    simplified_blocks: dict


@dataclass
class WriteContext:
    outputs: DefaultDict[str, List[str]]
    module_paths: Paths
    compound_paths: Paths
    enum_paths: Paths


def _path_all_rel_parents(path: pathlib.Path):
    if len(path.name) == 0:
        return
    if path.suffix == ".gzb":
        yield path.stem
    else:
        yield path.name
    yield from _path_all_rel_parents(path.parent)


def file_ident(path: List[str]):
    if len(path) < 2:
        raise ValueError("too few path elements")
    return json.dumps(path)


def file_ident_to_filename(fident: str) -> pathlib.Path:
    path = json.loads(fident)
    return pathlib.Path(path[0], "gazebo", *path[1:-1], path[-1] + ".gzb")


def rel_filename_to_file_ident(path: pathlib.Path) -> str:
    elems = list(_path_all_rel_parents(path))
    elems.reverse()
    type = elems.pop(1)
    assert type == "gazebo"
    return json.dumps(elems)


def file_ident_to_nsid(fident: str) -> str:
    return nsid(json.loads(fident))


def nsid(path: List[str]):
    if len(path) < 2:
        raise ValueError("too few path elements")
    return f"{path[0]}:{'~'.join(path[1:])}"


def parse_nsid(input: str):
    split = input.split(":", 2)
    ns = split[0]
    name_raw = split[1]
    name = list(flat(map(lambda x: x.split("~"), name_raw.split("/"))))
    return [ns] + name


def _escape_nsid_part(part: str):
    possible_keywords = {"count", "match", "true", "false"}
    if "." in part or "-" in part or part in possible_keywords:
        return f"`{part}`"
    return part


def escape_nsid(inp: Union[str, List[str]]):
    if isinstance(inp, str):
        inp = parse_nsid(inp)
    return nsid(list(map(_escape_nsid_part, inp)))


def escape_field_name(field: str) -> str:
    # TODO: don't escape for 'facing' once it is no longer a restricted identifier
    if ":" in field or field == "facing":
        return f'"{field}"'
    return field


def extract_paths(
        path: List[str],
        module,
        doc: NbtDoc,
        ctx: WriteContext
):
    for child_name, child_meta in module["children"].items():
        child_path = path + [child_name]
        if "Module" in child_meta:
            child_module_idx = child_meta["Module"]
            ctx.module_paths[child_module_idx] = child_path
            extract_paths(child_path, doc.modules[child_module_idx], doc, ctx)
        if "Compound" in child_meta:
            compound_idx = child_meta["Compound"]
            ctx.compound_paths[compound_idx] = child_path
        if "Enum" in child_meta:
            enum_idx = child_meta["Enum"]
            ctx.enum_paths[enum_idx] = child_path


TYPES_GATHERED = set()
REGISTRIES_GENERATED = set()
REGISTRIES_REFERENCED = set()

PRIMITIVES = {
    "Boolean": "Bool",
    "Byte": "Int",  # TODO: support Byte
    "Double": "Float",  # TODO: support Double
    "Float": "Float",
    "Int": "Int",
    "Long": "Int",  # TODO: support Long
    "Short": "Int",  # TODO: support Short
    "String": "String"
}


def _map_type(ctx: WriteContext, typename: str, config: Any) -> str:
    TYPES_GATHERED.add(json.dumps({
        "name": typename,
        "config": config,
    }))
    if typename in PRIMITIVES:
        return PRIMITIVES[typename]
    if typename == "IntArray":
        # TODO: support value_range
        len_constraint = ""
        if len_constraint_config := config.get("length_range"):
            if len_constraint_config[0] != len_constraint_config[1]:
                raise ValueError(f"Unsupported length_range: {len_constraint_config}")
            len_constraint = f" {len_constraint_config[0]}"
        return f"[Int;{len_constraint}]"
    if typename == "Compound":
        return nsid(ctx.compound_paths[config])
    if typename == "Enum":
        return nsid(ctx.enum_paths[config])
    if typename == "Id":
        if config == "minecraft:entity":
            # HACK: workaround for wrong registry name in mc-nbtdoc
            config = "minecraft:entity_type"
        if config == "minecraft:block_entity":
            # HACK: workaround for wrong registry name in mc-nbtdoc
            config = "minecraft:block_entity_type"
        REGISTRIES_REFERENCED.add(config)
        return f"$<{config}>"
    if typename == "Index":
        # TODO: index
        return "String // TODO(Index)"
    if typename == "List":
        # TODO: length_range
        return f"[{map_type(ctx, config['value_type'])}]"
    if typename == "Or":
        if len(config) == 0:
            return "__BOTTOM"
        if len(config) == 1:
            return map_type(ctx, config[0])
        else:
            # TODO: sum type
            alternatives = set(map(lambda t: map_type(ctx, t), config))
            if len(alternatives) == 1:
                return alternatives.pop()
            return f"String // TODO(Or): {' | '.join(alternatives)}"
    return "Unk"


def map_type(ctx: WriteContext, nbttype: Any) -> str:
    typename = nbttype
    config = {}
    if isinstance(nbttype, dict):
        items = list(nbttype.items())
        if len(items) != 1:
            raise ValueError("nbttype contained more than one item")
        typename = items[0][0]
        config = items[0][1]
    return _map_type(ctx, typename, config)


def write_compound(ctx: WriteContext, idx, compound: Any):
    path = ctx.compound_paths[idx]
    file = file_ident(path[:-1])
    name = path[-1]

    supers = ""
    supers_meta = compound["supers"]
    if supers_meta:
        super_compound_idx = supers_meta.get("Compound")
        if super_compound_idx is not None:
            supers = f" : {nsid(ctx.compound_paths[super_compound_idx])}"

    fields = []
    for field_name, field_meta in sorted(compound["fields"].items()):
        field_type = map_type(ctx, field_meta["nbttype"])
        field = f"""
        /**{field_meta["description"]} */
        {escape_field_name(field_name)} {field_type}"""
        fields.append(field)

    fields = "\n    ".join(fields)

    content = f"""
    /**{compound["description"]} */
    type {name} interface{supers}
    {{
        {fields}
    }}"""

    ctx.outputs[file].append(content)


def write_enum(ctx: WriteContext, idx, enum: Any):
    path = ctx.enum_paths[idx]
    file = file_ident(path[:-1])
    name = path[-1]

    enum_content_type_raw = next(enum["et"].keys().__iter__())
    if enum_content_type_raw not in {"Byte", "Int", "String"}:
        raise ValueError("unsupported enum value type")
    # apply general type mapping in any case, may do something useful, or just nothing
    enum_content_type = map_type(ctx, enum_content_type_raw)

    enum_entries = []
    for enum_entry_name, enum_entry_conf in enum["et"][enum_content_type_raw].items():
        enum_entries.append((enum_entry_name, enum_entry_conf["description"], enum_entry_conf["value"]))
    enum_entries.sort(key=lambda tup: tup[2])

    enum_entries_out = []
    for enum_entry_name, enum_entry_description, enum_entry_value in enum_entries:
        enum_entries_out.append(f"""
        /**{enum_entry_description} */
        {enum_entry_name} = {json.dumps(enum_entry_value)}""")

    enum_entries_out = ",\n       ".join(enum_entries_out)

    content = f"""
    /**{enum["description"]} */
    type {name} enum {enum_content_type}
    {{
        {enum_entries_out}
    }}"""

    ctx.outputs[file].append(content)


def _type_block_states(states: List[str]) -> Tuple[str, List[str]]:
    peek = states[0]
    if peek == "true" or peek == "false":
        return "Bool", states
    try:
        if str(int(peek)) == peek:
            return "Int", states
    except:
        pass
    return "String", list(map(lambda v: f'"{v}"', states))


def _reg_aux__common_data(nbtdoc_registry: Tuple[dict, int], registration_name: str, write_context: WriteContext) -> str:
    refs, default_idx = nbtdoc_registry
    idx = refs.get(registration_name)
    if idx is None:
        idx = default_idx
    return f"data {nsid(write_context.compound_paths[idx])}"


def _reg_aux_block(registration_name: str, mcdata: McData, nbtdoc: NbtDoc, write_context: WriteContext):
    block = mcdata.simplified_blocks[registration_name]
    block_states = []
    for block_state_name, block_state_options in block["properties"].items():
        block_state_type, block_state_options_typed = _type_block_states(block_state_options)
        block_states.append(f"""
        {block_state_name} {block_state_type} {{ {", ".join(block_state_options_typed)} }}
        """)
    block_states = "\n".join(block_states)
    return f"""
    states
    {{
        {block_states}
    }}
    {_reg_aux__common_data(nbtdoc.registries['minecraft:block'], registration_name, write_context)}
    """


def _reg_aux_item(registration_name: str, mcdata: McData, nbtdoc: NbtDoc, write_context: WriteContext):
    return _reg_aux__common_data(nbtdoc.registries["minecraft:item"], registration_name, write_context)


def _reg_aux_entity_type(registration_name: str, mcdata: McData, nbtdoc: NbtDoc, write_context: WriteContext):
    # Note: nbtdoc calls the registry "minecraft:entity", but officially it is "minecraft:entity_type"
    return _reg_aux__common_data(nbtdoc.registries["minecraft:entity"], registration_name, write_context)


def create_registrations(registry_name: str, registry_entries, mcdata: McData, nbtdoc: NbtDoc, write_context: WriteContext) -> List[str]:
    def aux_fn(_1: str, _2: McData, _3: NbtDoc, _4: WriteContext):
        return ""

    if registry_name == "minecraft:item":
        aux_fn = _reg_aux_item
    elif registry_name == "minecraft:entity_type":
        aux_fn = _reg_aux_entity_type
    elif registry_name == "minecraft:block":
        aux_fn = _reg_aux_block

    res = []
    for registration_name, registration_content in registry_entries.items():
        res.append(f"""
        register {escape_nsid(registration_name)}
        {{
            protocol_id {registration_content["protocol_id"]}
            {aux_fn(registration_name, mcdata, nbtdoc, write_context)}
        }}
        """)
    return res


def run(input_dir: pathlib.Path, output_dir: pathlib.Path, overlay_dirs: Iterable[pathlib.Path]) -> str:
    in_nbtdoc = input_dir / "mc-nbtdoc"
    in_mcdata = input_dir / "mcdata"

    outfiles = defaultdict(lambda: [])
    write_context = WriteContext(outfiles, {}, {}, {})

    #
    # process mc-nbtdoc
    #

    with (in_nbtdoc / "build" / "generated.json").open("r") as f:
        nbtdoc_raw = json.load(f)
    nbtdoc = NbtDoc(
        registries=nbtdoc_raw["registries"],
        root_modules=nbtdoc_raw["root_modules"],
        compounds=nbtdoc_raw["compound_arena"],
        enums=nbtdoc_raw["enum_arena"],
        modules=nbtdoc_raw["module_arena"],
    )

    for root_name, root_mod_idx in nbtdoc.root_modules.items():
        root_mod = nbtdoc.modules[root_mod_idx]
        root_path = [root_name]
        write_context.module_paths[root_mod_idx] = root_path
        extract_paths(root_path, root_mod, nbtdoc, write_context)

    for compound_idx, compound in enumerate(nbtdoc.compounds):
        write_compound(write_context, compound_idx, compound)
    for enum_idx, enum in enumerate(nbtdoc.enums):
        write_enum(write_context, enum_idx, enum)

    #
    # process mcdata
    #

    with (in_mcdata / "generated" / "reports" / "registries.json").open("r") as f:
        mcdata_registries = json.load(f)
    with (in_mcdata / "processed" / "reports" / "blocks" / "simplified" / "data.min.json").open("r") as f:
        mcdata_siplified_blocks = json.load(f)
    mcdata = McData(mcdata_registries, mcdata_siplified_blocks)

    for registry_name, registry_content in mcdata_registries.items():
        registry = registry_name.replace("/", "~")
        REGISTRIES_GENERATED.add(registry)

        registrations = create_registrations(registry_name, registry_content["entries"], mcdata, nbtdoc, write_context)
        registrations = "\n    ".join(registrations)

        registry_nsid = parse_nsid(registry_name)
        outfiles[file_ident(registry_nsid)].append(f"""
        static registry $<{registry}>
        {{
            protocol_id {registry_content["protocol_id"]}
            {registrations}
        }}
        """)

    #
    # output
    #

    for fident, content in outfiles.items():
        path = (output_dir / file_ident_to_filename(fident))
        _mkdirs(path.parent)
        with path.open("w") as f:
            f.write("module ")
            f.write(file_ident_to_nsid(fident))
            f.write("\n\n")
            f.write("".join(content))
            f.write("\n")

    #
    # overlay
    #

    def apply_overlay(p: pathlib.Path, *, current_root=None):
        if current_root is None:
            current_root = p

        if p.is_dir():
            for entry in p.iterdir():
                apply_overlay(entry, current_root=current_root)
            return

        p_rel = p.relative_to(current_root)
        p_base = output_dir / p_rel
        _mkdirs(p_base.parent)
        # delete if overlay content was written to a new file previously (instead of actually overlaying)
        #  (not doing this would cause the file to grow only and never be reset)
        if p_base.exists():
            with p_base.open("r") as f_base:
                if f_base.read(len(OVERLAY_HEADER)) == OVERLAY_HEADER:
                    p_base.unlink()
        # append overlay content
        with p.open("r") as f_in:
            new_file = not p_base.exists()
            with p_base.open("a") as f_out:
                f_out.write(OVERLAY_HEADER)
                if new_file:
                    f_out.write(f"/*inject*/ module {file_ident_to_nsid(rel_filename_to_file_ident(p_rel))}\n\n")
                f_out.write(f_in.read())

    for overlay_dir in overlay_dirs:
        apply_overlay(overlay_dir)

    #
    # warn
    #

    for nonregistered_registry in REGISTRIES_REFERENCED - REGISTRIES_GENERATED:
        print("referenced non-registered registry:", nonregistered_registry)

    #
    # return the version that was used
    #

    return (in_mcdata / "VERSION.txt").read_text().strip()


if __name__ == "__main__":
    run(pathlib.Path("./input"), pathlib.Path("./output"), [pathlib.Path("./overlay")])
