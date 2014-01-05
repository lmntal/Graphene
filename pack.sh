#!/bin/sh

# リリース用のZIP作成用script
# minifyした"unyo-x.y.z.min.jar"という名前のjarファイルが必要です(x,y,zはバージョン番号)

version=`ls target | grep '\.min\.jar$' | sed -e 's/unyo-//' | sed -e 's/\.min\.jar//'`
rm -r target/unyo
cp -R template target/unyo/
cp target/unyo-$version.min.jar target/unyo/unyo.jar
cd target
zip -r unyo.$version.zip unyo
cd ../
