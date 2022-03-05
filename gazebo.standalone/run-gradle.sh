#!/usr/bin/env sh

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

if [ -z "$GITLAB_CI" ]; then
  GRADLE="$SCRIPT_DIR/gradlew"
else
  GRADLE="gradle"
fi

$GRADLE -p"$SCRIPT_DIR" "$@"
