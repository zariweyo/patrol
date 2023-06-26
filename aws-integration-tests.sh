#!/usr/bin/env bash
set -euo pipefail

# export AWS_ACCESS_KEY_ID=""
# export AWS_SECRET_ACCESS_KEY=""
# export AWS_SESSION_TOKEN=""
# export DEV_PROJECT_ARN=""
# export TEST_SPEC_ARN=""
# export DEVICE_POOL_ARN=""

# Upload app
aws devicefarm create-upload \
    --name ~/Documents/patrol/packages/patrol/example/build/app/outputs/apk/debug/app-debug-androidTest.apk \
    --type ANDROID_APP \
    --project-arn $DEV_PROJECT_ARN

# Upload test
aws devicefarm create-upload \
    --name ~/Documents/patrol/packages/patrol/example/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
    --type INSTRUMENTATION_TEST_PACKAGE \
    --project-arn $DEV_PROJECT_ARN

# TODO: get test ARN

# Run tests
aws devicefarm schedule-run \
    --profile DeviceFarm-523978932065 \
    --project-arn $DEV_PROJECT_ARN \
    --device-pool-arn $DEVICE_POOL_ARN \
    --test type="INSTRUMENTATION",testPackageArn=string,testSpecArn=$TEST_SPEC_ARN,filter=string,parameters={KeyName1=string,KeyName2=string}

# TODO: remove uploads
