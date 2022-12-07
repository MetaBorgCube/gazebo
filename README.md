# Gazebo: A Domain-Specific Language for Minecraft

This repository accompanies a technical report describing the project.
It can be found [here](./doc/tech-report.pdf) (in the `doc` directory).
For most background information, please refer to this document.
Other miscellaneous documentation, if any, can also be found in the `doc` directory.

The document's abstract is as follows.

> The popular video game Minecraft offers a feature-rich environment for building extensions.
> Unfortunately, the learning curve is steep for new users as it requires a non-conventional way of thinking about data organization and program flow in a highly verbose and error-prone syntax.
> We propose Gazebo, a new domain-specific language, inspired by existing community efforts and our earlier work, that aims to provide a more natural and expressive way of programming for Minecraft.
> The language is fully implemented using the Spoofax Language Workbench, resulting in a product with editor support and a stand-alone command line interface.
> We report on the design of the language, its compilation pipeline and our experiences with using Spoofax to achieve this.

## Repository Layout

The repository is relatively large, as it contains a handful of different parts that together make up the entire compiler pipeline.
For most parts, we use [Spoofax](https://www.spoofax.dev) and hence adhere to its directory structure.

- `.mvn`: required by Spoofax, configured Maven with Spoofax's extension repositories and Maven's own JVM arguments.
- `doc`: some documentation.
- `examples`: various examples, collected over the course of the initial implementation.
- `ext`: 'extension' projects, which are Spoofax projects that are not part of the language itself but are used by it.
  In our case, these projects contain the logic to transform one language into another.
- `gazebo.standalone`: a Gradle project containing several subprojects that form a stand-alone compiler with a command line interface and a for-demonstration-purposes-only GUI.
- `gen`: collection of Python scripts and submodules to generate the standard library.
- `lang`: the actual Spoofax language definitions. These contain the syntax definitions and static semantics.
- `misc`: a meta-project that collects all Spoofax projects, just for build convenience.
- `str-common`: a shared Stratego library used by the extension projects.
- `test`: a minimal Spoofax test suite, mostly for our languages' syntax and some static semantics.
- `tools`: Python module of miscellaneous tools. Mostly used to generate the standard library.

## Building Towards a Fully Stand-Alone Compiler

Requirements and tested versions:
- Java 11
- Maven 3.7
- Gradle 7.3.1 (already provided by Gradle Wrapper)
- Python 3.8+

Everything has been developed and tested under Linux.
It may work on macOS, and it could perhaps work on Windows too, but we have not verified either of these.

In `.gitlab-ci.yml` an example CI pipeline is described.
Most of the build steps should be self-explanatory from this, as there is little to configure anyway.
In the following paragraphs, we will also give a step-by-step breakdown.

The first step is to build the Spoofax projects.
This can be done as easily as executing `mvn package` in the repository root.
However, at the current stage, the test suite does not pass.
To exclude the test suite, execute `mvn -DskipTests package`.
This may take a while as we haven't enabled any incremental compilation features.
The output of this step is a set of Spoofax langauge archives, found in `misc/target/out-lang`.
These are already usable as-is with existing Spoofax tooling, such as Sunshine or the full Eclipse environment.

The next step is bootstrapping Gazebo Standalone to build the final compiler with the standard library embedded.
Since the standard library depends on the compiler, we first do a build of the compiler without the standard library, which is then used to build the standard library.
Afterwards, we re-build Gazebo Standalone with the standard library embedded.
Generating the standard library requires the GIt submodules to be initialized.
If you have not done so yet, do it now.
To launch a fully-automated standard library bootstrapping, run the build automation tool.
Note that `./standalone_out` can be any directory where the standard library files are emitted.

```shell
# if not done already: initialize Git submodules
git submodule update --init --recursive
# generate the standard library in ./standalone_out
python -m tools.build_automation -O ./standalone_out
```

Now, the final compiler is almost ready to be built.
First, we need to copy all `.spoofax-language` files from `misc/target/out-lang` to the same directory as we generated the standard library in (`./standalone_out` in the example above).
There is only one step remaining, which is specifying which parts you actually want to make built-in.
Do this by adding a file in `./standalone_out` called `gzbs-builtin-langs.txt` and put the name of every file on a separate line.
Alternatively, execute the following command to get the result instantaneously.

```shell
# copy all main Spoofax Language Archives to ./standalone_out, to be included in the final compiler
cp -t ./standalone_out ./misc/target/out-lang/*.spoofax-language
# generate the contents of gzbs-builtin-langs.txt
ls -1 standalone_out/*.spoofax-language | tr '\n' '\0' | xargs -0 -n 1 basename > ./standalone_out/gzbs-builtin-langs.txt
```

The final step is to package some compiler distribution archives.
This can be achieved by executing the Gradle task `:cli:build`, with an additional parameter `gzbsLibsDir` set to the
*absolute* path of the directory containing the file `gzbs-builtin-langs.txt`.

```shell
# assuming we're currently in the repository root, call Gradle with the absolute path to the stdlib output directory
./gazebo.standalone/run-gradle.sh :cli:build -PgzbsLibsDir="$PWD"/standalone_out
```

Distributions are generated as ZIP and Gzipped TAR archives in `gazebo.standalone/cli/build/distributions`.
These contain ready-to-use binaries that work in the presence of the included JARs.

## Development

There is no set-up required for development.

The Spoofax projects can be imported into the Spoofax Eclipse environment without configuration.

Similarly, the Gazebo Standalone Gradle project can be loaded into an IDE such as IntelliJ IDEA directly.

The Python scripts from the `gen` and `tools` directories also need no further setup, as they only rely on the Python standard library.

## License

The _code_ in this repository is licensed under MIT, see [`LICENSE`](./LICENSE).
