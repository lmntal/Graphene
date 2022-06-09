# Graphene

Version 4

## Requires
* Scala (version >= 2.11.12)
* Java and JDK (version >= 1.11)
* sbt (See http://scalajp.github.io/sbt-getting-started-guide-ja/setup/)

## Build
* sbt assembly
* (if this does not work due to sbt.internal.ServerAlreadyBootingException, try sbt --batch -Dsbt.server.forcestart=true assembly)
* then Graphene-assembly-x.x.x.jar will be generated

## Release

```
$ vim build.sbt # バージョン番号を更新
$ vim template/README.md # 更新情報を記述
$ rm -rf target
$ sbt assembly
$ ./pack.sh
$ # target/graphene-x.x.x.zipをアップロード
$ git checkout master
$ git merge release/vx.x.x
$ git tag -a vx.x.x -m "Version x.x.x"
```

* リリース時にはbuild.sbtのバージョンを更新し、git tagでタグ付けをする
* バージョンはx.x.xの形式にし、前から順にmajor、minor、patchとする
* 詳しくは[http://semver.org/spec/v2.0.0.html]()を参考にする

## コード内で使っている用語
### 座標系
* スクリーン座標系とワールド座標系の2種類がある
* スクリーン座標系はjava.awt.Graphicsと対応している
* ワールド座標系はノードたちが存在する座標系で、位置や倍率を変更してスクリーン座標系に対応させることができる
