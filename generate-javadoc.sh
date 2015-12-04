#!/bin/bash -e
./gradlew clean
./gradlew javadoc
rm -rf ../javadoc
mv uploadservice/build/docs/javadoc/ ../
git checkout gh-pages
rm -rf javadoc
mv ../javadoc .
