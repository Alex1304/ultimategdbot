#!/bin/sh

set -e

JAVA_HOME=$LINUX_JDK

# Build for Linux

mvn clean package -Djlink.jdk="$LINUX_JDK"
mkdir -p delivery/target/distribution
file=$(find delivery/target/ -name "*.zip" -printf "%f\n")
mv "delivery/target/$file" "delivery/target/distribution/${file%.*}_linux_x64.zip"

# Build for Windows

mvn package -Djlink.jdk="$WINDOWS_JDK"
mv "delivery/target/$file" "delivery/target/distribution/${file%.*}_windows_x64.zip"
