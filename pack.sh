#!/bin/sh

# リリース用のZIP作成用script
# minifyした"graphene-x.y.z.min.jar"という名前のjarファイルが必要です(x,y,zはバージョン番号)

version=`ls target | grep '\.min\.jar$' | sed -e 's/graphene-//' | sed -e 's/\.min\.jar//'`
rm -r target/graphene
cp -R template target/graphene/
cp target/graphene-$version.min.jar target/graphene/graphene.jar
cd target
zip -r graphene.$version.zip graphene
cd ../
