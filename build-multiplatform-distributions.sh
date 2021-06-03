#!/bin/bash

# Build for Linux

mvn clean package

if [ $? -ne 0 ]; then
	exit
fi

mkdir -p delivery/target/distribution
file=`find delivery/target/ -name *.zip -printf "%f\n"`
mv delivery/target/$file delivery/target/distribution/${file%.*}_linux_x64.zip

# Build for MacOS

mvn package -Djlink.jdk=$MACOS_JDK

if [ $? -ne 0 ]; then
	exit
fi

mv delivery/target/$file delivery/target/distribution/${file%.*}_macOS_x64.zip

# Build for Windows

mvn package -Djlink.jdk=$WINDOWS_JDK

if [ $? -ne 0 ]; then
	exit
fi

mv delivery/target/$file delivery/target/distribution/${file%.*}_windows_x64.zip