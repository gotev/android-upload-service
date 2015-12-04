#!/bin/bash -e
./gradlew clean
./gradlew javadoc
mv uploadservice/build/docs/javadoc/ ../
git checkout gh-pages
rm -rf javadoc
mv ../javadoc .
