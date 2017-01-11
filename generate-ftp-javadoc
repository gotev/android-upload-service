#!/bin/bash -e
./gradlew :uploadservice-ftp:clean :uploadservice-ftp:javadoc
rm -rf ../javadoc-ftp
mv uploadservice-ftp/build/docs/javadoc/ ../javadoc-ftp
git checkout gh-pages
rm -rf javadoc-ftp
mv ../javadoc-ftp .
cd javadoc-ftp
git add . --force
git commit -m "updated ftp module javadocs"
git push
cd ..
git checkout master
