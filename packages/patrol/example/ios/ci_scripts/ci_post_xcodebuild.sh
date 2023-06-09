#!/usr/bin/env bash
set -euo pipefail

cd "$CI_WORKSPACE/packages/patrol/example"

dart pub global activate \
    --source git https://github.com/leancodepl/patrol.git \
    --git-path packages/patrol_cli \
    --git-ref patrol_cli-v2.0.0-dev.2
    
patrol build ios --target integration_test/example_test.dart

export CI_TEST_PRODUCTS_PATH="$CI_WORKSPACE/packages/patrol/example/build/ios_integ/Build/Products"
