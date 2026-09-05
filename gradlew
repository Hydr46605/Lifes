#!/bin/sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

property() {
    key=$1
    sed -n "s/^${key}=//p" "$PROPERTIES" | tail -n 1
}

DISTRIBUTION_URL=$(property distributionUrl | sed 's/\\:/:/g')
DISTRIBUTION_SHA=$(property distributionSha256Sum)
ARCHIVE_NAME=${DISTRIBUTION_URL##*/}
DISTRIBUTION_NAME=${ARCHIVE_NAME%-bin.zip}
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
CACHE_DIR="$GRADLE_USER_HOME/wrapper/dists/lifes/$DISTRIBUTION_NAME"
GRADLE_HOME="$CACHE_DIR/$DISTRIBUTION_NAME"
ARCHIVE="$CACHE_DIR/$ARCHIVE_NAME"

verify_archive() {
    if command -v sha256sum >/dev/null 2>&1; then
        actual=$(sha256sum "$ARCHIVE" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then
        actual=$(shasum -a 256 "$ARCHIVE" | awk '{print $1}')
    else
        echo "ERROR: sha256sum or shasum is required to verify Gradle." >&2
        exit 1
    fi
    if [ "$actual" != "$DISTRIBUTION_SHA" ]; then
        rm -f "$ARCHIVE"
        echo "ERROR: Gradle distribution checksum mismatch." >&2
        exit 1
    fi
}

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$CACHE_DIR"
    if [ ! -f "$ARCHIVE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl --fail --location --silent --show-error "$DISTRIBUTION_URL" --output "$ARCHIVE"
        elif command -v wget >/dev/null 2>&1; then
            wget --quiet "$DISTRIBUTION_URL" --output-document="$ARCHIVE"
        else
            echo "ERROR: curl or wget is required to download Gradle." >&2
            exit 1
        fi
    fi
    verify_archive
    if ! command -v unzip >/dev/null 2>&1; then
        echo "ERROR: unzip is required to install Gradle." >&2
        exit 1
    fi
    TEMP_DIR="$CACHE_DIR/.extract-$$"
    rm -rf "$TEMP_DIR"
    mkdir -p "$TEMP_DIR"
    unzip -q "$ARCHIVE" -d "$TEMP_DIR"
    rm -rf "$GRADLE_HOME"
    mv "$TEMP_DIR/$DISTRIBUTION_NAME" "$GRADLE_HOME"
    rm -rf "$TEMP_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
