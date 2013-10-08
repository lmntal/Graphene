# UNYO UNYO

Version 4

## Requires
* Scala (version >= 2.10)
* Java and JDK (version >= 1.7)
* sbt (See http://scalajp.github.io/sbt-getting-started-guide-ja/setup/)

## Build
* sbt assembly
* then UNYO-UNYO-assembly-x.x.x.jar will be generated

## コード内で使っている用語
### 座標系
* スクリーン座標系とワールド座標系の2種類がある
* スクリーン座標系はjava.awt.Graphicsと対応している
* ワールド座標系はノードたちが存在する座標系で、位置や倍率を変更してスクリーン座標系に対応させることができる
