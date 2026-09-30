#!/usr/bin/env sh
set -eu
RIFT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$RIFT_DIR"
java Build.java
