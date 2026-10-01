#!/usr/bin/env sh
set -eu
RIFT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$RIFT_DIR"
if ! command -v java >/dev/null 2>&1; then
    echo "Instale Java 17 ou superior: https://adoptium.net/temurin/releases/" >&2
    exit 1
fi
exec java -Xms128m -Xmx768m -jar "$RIFT_DIR/RiftProtocol.jar"
