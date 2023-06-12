#!/usr/bin/env bash
set -euo pipefail

cd "$CI_WORKSPACE/packages/patrol/example"

export PATH="$PATH:$HOME/flutter/bin"

dart pub global activate \
    --source git https://github.com/leancodepl/patrol.git \
    --git-path packages/patrol_cli \
    --git-ref patrol_cli-v2.0.0-dev.2

export PATH="$PATH":"$HOME/.pub-cache/bin"
    
patrol build ios --simulator --target integration_test/example_test.dart
mv build/ios_integ/Build/Products/*.xctestrun '/Volumes/workspace/TestProducts/RunnerCopy_iphonesimulator16.4-x86_64.xctestrun'


