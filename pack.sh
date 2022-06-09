#!/bin/sh

# リリース用のZIP作成用script

scalaversion=scala-2.11
version=`ls target/$scalaversion | grep '\.jar$' | sed -e 's/Graphene-assembly-//' | sed -e 's/\.jar//'`
rm -rf target/graphene
cp -R template target/graphene/
cp target/$scalaversion/Graphene-assembly-$version.jar target/graphene/graphene.jar
cd target
zip -r graphene-$version.zip graphene
cd ..
