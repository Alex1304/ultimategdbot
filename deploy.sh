#!/bin/bash

zipdir="$1"
zipfile="$2"
configdir="$3"

unzipdir="${zipfile:0:-14}"

cd $zipdir
unzip $zipfile && rm $zipfile
cd $unzipdir
chmod u+x bin/* lib/jspawnhelper
cp -r $configdir/* .
