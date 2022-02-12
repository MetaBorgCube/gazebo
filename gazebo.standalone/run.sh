#!/usr/bin/env sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

GRADLE="$SCRIPT_DIR/gradlew"

$GRADLE -p"$SCRIPT_DIR" --console=plain :cli:run --args="$(printf -- '"%s" ' "$@")"
