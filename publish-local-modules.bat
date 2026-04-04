#!/bin/bash
# publish-local-modules.sh
# Publish api/ and core/ modules to local Maven repository (.m2)
# This allows the host to consume them as external dependencies before full repo extraction.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Publishing ExtremeCraft api and core modules to local Maven repository..."
echo "Target: ~/.m2/repository/com/extremecraft/"
echo ""

./gradlew.bat :api:publish :core:publish

echo ""
echo "✅ Published artifacts:"
echo "   - com.extremecraft:extremecraft-api:1.2.0"
echo "   - com.extremecraft:extremecraft-core:1.2.0"
echo ""
echo "To consume in the host build.gradle:"
echo "   dependencies {"
echo "       implementation 'com.extremecraft:extremecraft-api:1.2.0'"
echo "       implementation 'com.extremecraft:extremecraft-core:1.2.0'"
echo "   }"
