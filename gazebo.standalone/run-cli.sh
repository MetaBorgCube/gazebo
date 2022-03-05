#!/usr/bin/env sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

"$SCRIPT_DIR/run-gradle.sh" --console=plain :cli:run --args="$(printf -- '"%s" ' "$@")"
