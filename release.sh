#!/bin/bash

if [ -z "$1" ]; then
    echo "Version number required"
    exit 1
fi

if [ -z "$2" ]; then
    echo "Next version number required"
    exit 1
fi

set -e

mvn versions:set -DnewVersion=$1
git add .
git commit -m "Release version v$1"
git tag "v$1"
./build-multiplatform-distributions.sh
scp "delivery/target/distribution/ultimategdbot-$1_linux_x64.zip" alex@alex1304.com:/home/alex/ultimategdbot/deploy
mvn versions:set -DnewVersion=$2
git add .
git commit -m "Prepare for next development iteration"
git push
git push --tags