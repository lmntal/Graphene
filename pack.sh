#!/bin/sh

version=`ls target | grep '\.min\.jar$' | sed -e 's/unyo-//' | sed -e 's/\.min\.jar//'`
cp -R template target/unyo/
cp target/unyo-$version.min.jar target/unyo/unyo.$version.jar
cd target
zip -r unyo.$version.zip unyo
cd ../
