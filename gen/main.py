import itertools
import json
import pathlib
from collections import defaultdict
from dataclasses import dataclass
from typing import Any

Paths = dict[int, list[str]]


def flat(notflat):
    return itertools.chain.from_iterable(notflat)


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
    outputs: defaultdict[pathlib.Path, str]
    module_paths: Paths
    compound_paths: Paths
    enum_paths: Paths


def filename(path: list[str]):
    if len(path) < 2:
        raise ValueError("too few path elements")
    return pathlib.Path(path[0], "function", *path[1:-1], path[-1] + ".gzb")


def nsid(path: list[str]):
    if len(path) < 2:
        raise ValueError("too few path elements")
    return f"{path[0]}:{'~'.join(path[1:])}"


def parse_nsid(input: str):
    split = input.split(":", 2)
    ns = split[0]
    name_raw = split[1]
    name = list(flat(map(lambda x: x.split("~"), name_raw.split("/"))))
    return [ns] + name


def extract_paths(
        path: list[str],
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

PRIMITIVES = {
    "Boolean": "Bool",
    "Byte": "Int",
    "Double": "Float",
    "Float": "Float",
    "Int": "Int",
    "IntArray": "[Int;]",  # TODO: int array length_range and value_range
    "Long": "Int",
    "Short": "Short",
    "String": "String"
}


def _map_type(ctx: WriteContext, typename: str, config: Any) -> str:
    if typename in PRIMITIVES:
        return PRIMITIVES[typename]
    TYPES_GATHERED.add(json.dumps({
        "name": typename,
        "config": config,
    }))
    if typename == "Compound":
        return nsid(ctx.compound_paths[config])
    if typename == "Enum":
        return nsid(ctx.enum_paths[config])
    if typename == "Id":
        return f"$<{config}>"
    if typename == "Index":
        # TODO
        return "Todo_Index"
    if typename == "List":
        # TODO: length_range
        return f"[{map_type(ctx, config['value_type'])}]"
    if typename == "Or":
        # TODO
        return "Todo_Or"
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
    file = filename(path[:-1])
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
        {field_name} {field_type}"""
        fields.append(field)

    fields = "\n    ".join(fields)

    content = f"""
    /**{compound["description"]} */
    type {name} interface{supers}
    {{
        {fields}
    }}"""

    ctx.outputs[file] += content


def write_enum(ctx: WriteContext, idx, enum: Any):
    path = ctx.enum_paths[idx]
    file = filename(path[:-1])
    name = path[-1]

    enum_content_type = next(enum["et"].keys().__iter__())
    if enum_content_type not in {"Byte", "Int", "String"}:
        raise ValueError("unrecognized enum value type")

    enum_entries = []
    for enum_entry_name, enum_entry_conf in enum["et"][enum_content_type].items():
        enum_entries.append((enum_entry_name, enum_entry_conf["description"], enum_entry_conf["value"]))
    enum_entries.sort(key=lambda tup: tup[2])

    enum_entries_out = []
    for enum_entry_name, enum_entry_description, enum_entry_value in enum_entries:
        enum_entries_out.append(f"""
        /**{enum_entry_description} */
        {enum_entry_name}({json.dumps(enum_entry_value)})""")

    enum_entries_out = ",\n       ".join(enum_entries_out)

    content = f"""
    /**{enum["description"]} */
    type {name} enum {enum_content_type}
    {{
        {enum_entries_out}
    }}"""

    ctx.outputs[file] += content


def _type_block_states(states: list[str]) -> tuple[str, list[str]]:
    peek = states[0]
    if peek == "true" or peek == "false":
        return "Bool", states
    try:
        if str(int(peek)) == peek:
            return "Int", states
    except:
        pass
    return "String", list(map(lambda v: f'"{v}"', states))


def _reg_aux__common_data(nbtdoc_registry: tuple[dict, int], registration_name: str, write_context: WriteContext) -> str:
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
    block_states
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


def create_registrations(registry_name: str, registry_entries, mcdata: McData, nbtdoc: NbtDoc, write_context: WriteContext) -> list[str]:
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
        register {registration_name}
        {{
            protocol_id {registration_content["protocol_id"]}
            {aux_fn(registration_name, mcdata, nbtdoc, write_context)}
        }}
        """)
    return res


def main():
    outfiles = defaultdict(lambda: "")
    write_context = WriteContext(outfiles, {}, {}, {})

    #
    # process mc-nbtdoc
    #

    with open("input/mc-nbtdoc/build/generated.json", "r") as f:
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

    with open("input/mcdata/generated/reports/registries.json") as f:
        mcdata_registries = json.load(f)
    with open("input/mcdata/processed/reports/blocks/simplified/data.min.json") as f:
        mcdata_siplified_blocks = json.load(f)
    mcdata = McData(mcdata_registries, mcdata_siplified_blocks)

    for registry_name, registry_content in mcdata_registries.items():
        registry_nsid = parse_nsid(registry_name)
        registrations = create_registrations(registry_name, registry_content["entries"], mcdata, nbtdoc, write_context)
        registrations = "\n    ".join(registrations)
        outfiles[filename(registry_nsid)] += f"""
        static registry $<{registry_name}>
        {{
            protocol_id {registry_content["protocol_id"]}
            {registrations}
        }}
        """

    #
    # output
    #

    outdir = pathlib.Path("output")
    for file, content in outfiles.items():
        path = (outdir / file)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w") as f:
            f.write(content)
            f.write("\n")


if __name__ == "__main__":
    main()
