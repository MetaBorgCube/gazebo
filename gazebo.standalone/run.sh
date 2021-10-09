#!/usr/bin/env sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

GRADLE="$SCRIPT_DIR/gradlew"

GRADLE_OPTS="-Xss16M -Dorg.slf4j.simpleLogger.log.org.metaborg=debug -Dorg.slf4j.simpleLogger.log.nl.jochembroekhoff=debug"

$GRADLE -p"$SCRIPT_DIR" --console=plain :cli:run --args="$(printf -- '"%s" ' "$@")"
