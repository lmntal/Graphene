package graphene.plugin.lmntal

import graphene.model.{Graph, Node}
import graphene.util.{Line, Point, Rect}

import scala.collection.mutable

object TreeLayoutManager {

  // 各階層の垂直・水平方向の間隔
  private val DEFAULT_VERTICAL_SPACING = 120.0
  private val DEFAULT_HORIZONTAL_SPACING = 100.0

  /**
   * 指定されたノードを基点として木構造レイアウトを実行します。
   * @param graph 対象のグラフ
   * @param rootNode レイアウトの基点となるノード
   */
  def layout(graph: Graph, rootNode: Node): Unit = {
    // レイアウト前の平均エッジ長から、レイアウト間隔を決定する
    val (verticalSpacing, horizontalSpacing) = if (graph.allEdges.nonEmpty) {
      val totalLength = graph.allEdges.map { edge =>
        Line.from(edge.source.view.rect.center).to(edge.target.view.rect.center).length
      }.sum
      val average = totalLength / graph.allEdges.length
      (average, average)
    } else {
      // エッジがない場合はデフォルト値を使用
      (DEFAULT_VERTICAL_SPACING, DEFAULT_HORIZONTAL_SPACING)
    }

    // 1. 幅優先探索(BFS)で各ノードの深さと階層ごとのノードリストを作成
    val depths = mutable.Map[Node, Int]()
    val nodesByDepth = mutable.Map[Int, mutable.ArrayBuffer[Node]]()
    val queue = mutable.Queue[(Node, Int)]()

    // 基点ノードを深さ0として初期化
    queue.enqueue((rootNode, 0))
    depths(rootNode) = 0

    while (queue.nonEmpty) {
      val (current, depth) = queue.dequeue()
      nodesByDepth.getOrElseUpdate(depth, mutable.ArrayBuffer()) += current

      // 隣接ノードを探索
      current.neighborNodes.foreach { neighbor =>
        if (!depths.contains(neighbor)) {
          depths(neighbor) = depth + 1
          queue.enqueue((neighbor, depth + 1))
        }
      }
    }

    // 2. 各ノードの新しい座標を計算して位置を更新
    val rootInitialPos = rootNode.view.rect.point

    nodesByDepth.foreach { case (depth, nodes) =>
      val levelWidth = (nodes.length - 1) * horizontalSpacing
      val startX = rootInitialPos.x - levelWidth / 2
      val y = rootInitialPos.y + depth * verticalSpacing

      nodes.zipWithIndex.foreach { case (node, index) =>
        // fixed(固定)されているノードは移動しない
        if (!node.view.fixed) {
          val x = startX + index * horizontalSpacing
          val currentRect = node.view.rect
          // ノードのサイズは維持し、位置のみを更新
          node.view.rect = Rect(Point(x, y), currentRect.dim)
        }
      }
    }

    // TODO: 画面の再描画を要求する処理を呼び出す (例: panel.repaint())
  }
}
