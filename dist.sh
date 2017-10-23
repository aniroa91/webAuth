#!/bin/bash

CURRENT_DIR=${PWD}
PROJECT_DIR="$(dirname "$0")"
#echo ${PROJECT_DIR}

cd ${PROJECT_DIR}

echo "=============== CLEAN ================="
rm -rf dist
mkdir dist

echo "=============== SBT DIST ================"
sbt dist

echo "=============== FINISH ================="
unzip -o target/universal/bigdata-play-1.0-SNAPSHOT.zip -d dist/
mv dist/bigdata-play-1.0-SNAPSHOT/* dist
rm -r dist/bigdata-play-1.0-SNAPSHOT

rm -rf target








