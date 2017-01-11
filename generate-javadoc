#!/bin/bash -e
./gradlew :uploadservice:clean :uploadservice:javadoc
rm -rf ../javadoc
mv uploadservice/build/docs/javadoc/ ../
git checkout gh-pages
rm -rf javadoc
mv ../javadoc .
cd javadoc
git add . --force
git commit -m "updated javadocs"
git push
cd ..
git checkout master
