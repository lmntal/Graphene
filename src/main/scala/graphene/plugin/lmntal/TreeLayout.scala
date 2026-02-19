package graphene.plugin.lmntal

import graphene.model._
import graphene.util._
import scala.collection.mutable

object TreeLayout {

  case class Params(verticalGap: Double, verticalStrength: Double)

  case class Context(
    parent: Map[Node, Option[Node]],
    orderedChildren: Map[Node, Seq[Node]],
    sameLevelNeighbors: Map[Node, Seq[Node]],
    noParent: Set[Node]
  )

  val EmptyContext = Context(Map.empty, Map.empty, Map.empty, Set.empty)

  def buildContext(graph: Graph): Context = {
    val parent = mutable.Map.empty[Node, Option[Node]]
    val sameLevel = mutable.Map.empty[Node, mutable.ArrayBuffer[Node]]
    val alive = graph.allNodes.toSet

    def addSameLevel(a: Node, b: Node): Unit = {
      sameLevel.getOrElseUpdate(a, mutable.ArrayBuffer.empty) += b
      sameLevel.getOrElseUpdate(b, mutable.ArrayBuffer.empty) += a
    }

    for (node <- graph.allNodes) {
      if (node.isRoot) {
        parent(node) = None
      } else node.attr match {
        case Mem => parent(node) = None
        case _ =>
          val ordered = graph.orderedNeighbors(node).filter(alive.contains)
          val fallback = if (ordered.nonEmpty) ordered else node.neighborNodes.filter(alive.contains)
          parent(node) = fallback.lastOption
      }
    }

    val handled = mutable.Set.empty[(Node, Node)]
    for ((node, pOpt) <- parent) {
      pOpt.foreach { p =>
        val pair = (node, p)
        if (!handled.contains(pair) && parent.getOrElse(p, None).contains(node)) {
          (node.attr, p.attr) match {
            case (HLAtom, _) =>
              parent(node) = None
              parent(p) = Some(node)
            case (_, HLAtom) =>
              parent(p) = None
              parent(node) = Some(p)
            case _ =>
              parent(node) = None
              parent(p) = None
              addSameLevel(node, p)
          }
          handled += pair
          handled += ((p, node))
        }
      }
    }

    val hlGroups = graph.allNodes.filter(_.attr == HLAtom).groupBy(_.id)
    for ((_, groupNodes) <- hlGroups) {
      val groupSet = groupNodes.toSet
      // グループ全体の親候補を一度だけ探す
      val connectedToHL = groupNodes.flatMap(_.neighborNodes).distinct
      
      // 親がグループに到達するノード（=グループの子孫）を除外して循環を防ぐ
      def isDescendantOfGroup(n: Node): Boolean = {
        var cur: Node = n
        val seen = mutable.Set.empty[Node]
        while (cur != null && !seen.contains(cur)) {
          if (groupSet.contains(cur)) return true
          seen += cur
          cur = parent.getOrElse(cur, None).orNull
        }
        false
      }

      // その中から、グループに到達しないアトムのみを抽出
      val candidatesNotPointingToGroup = connectedToHL.filter { p =>
        !groupSet.contains(p) && !isDescendantOfGroup(p)
      }
      
      // グループ内のすべてのHLアトムに同じ親を設定
      if (candidatesNotPointingToGroup.isEmpty) {
        // 親候補が見つからない場合、物理レイアウトを使用
        for (h <- groupNodes) {
          parent(h) = None
        }
      } else {
        // 候補の中でy座標が最も大きい（画面下方）ものを親にする
        val chosen = candidatesNotPointingToGroup.maxBy(_.view.rect.center.y)
        for (h <- groupNodes) {
          parent(h) = Some(chosen)
        }
      }
    }

    // 親指定の循環を検出して破壊（1サイクルにつき1ノードの親を外す）
    def breakParentCycles(): Unit = {
      val visiting = mutable.Set.empty[Node]
      val visited = mutable.Set.empty[Node]

      def follow(start: Node): Unit = {
        if (visited.contains(start)) return
        var current = start
        val path = mutable.ArrayBuffer.empty[Node]
        while (current != null && !visited.contains(current)) {
          if (visiting.contains(current)) {
            val idx = path.indexOf(current)
            if (idx >= 0) {
              val cycle = path.drop(idx)
              // 画面上方のノードを親なしにして循環を解消（下方向ドリフトを防ぐ）
              val cut = cycle.minBy(_.view.rect.center.y)
              parent(cut) = None
            }
            return
          }
          visiting += current
          path += current
          current = parent.getOrElse(current, None).orNull
        }
        for (n <- path) {
          visiting -= n
          visited += n
        }
      }

      for (n <- parent.keys) follow(n)
    }

    breakParentCycles()

    val orderedChildren = mutable.Map.empty[Node, Seq[Node]]
    for ((node, _) <- parent) {
      val children = parent.collect { case (child, Some(p)) if p eq node => child }.toSeq
      if (children.nonEmpty) {
        val ordered = graph.orderedNeighbors(node).filter(children.contains)
        val sorted =
          if (ordered.nonEmpty) ordered ++ children.filterNot(ordered.contains)
          else children
        orderedChildren(node) = sorted
      }
    }

    // Track nodes without assigned parents (for fallback to physics layout)
    val noParent = parent.collect { case (node, None) if !node.isRoot => node }.toSet

    Context(parent.toMap, orderedChildren.toMap, sameLevel.map { case (k, v) => k -> v.toSeq }.toMap, noParent)
  }

  def forceFor(node: Node, ctx: Context, params: Params): Point = {
    if ((ctx eq EmptyContext) || node.isRoot) return Point.zero

    val self = node.view.rect.center

    val parentForce = ctx.parent.getOrElse(node, None).map { parent =>
      val targetY = parent.view.rect.center.y + params.verticalGap
      val dy = targetY - self.y
      Point(0.0, dy * params.verticalStrength)
    }.getOrElse(Point.zero)

    parentForce
  }

  def forceFor(node: Node, ctx: Context, params: Params, forceParams: graphene.plugin.lmntal.ForceParams): Point = {
    if ((ctx eq EmptyContext) || node.isRoot) return Point.zero

    val self = node.view.rect.center
    val physics = DefaultMover.forceFor(node, forceParams)

    val parentOpt = ctx.parent.getOrElse(node, None)
    val peersOpt = ctx.sameLevelNeighbors.get(node)

    // 親候補も同レベルの仲間もいない場合は、y方向を固定し、x方向のみ物理レイアウト
    if (parentOpt.isEmpty && peersOpt.forall(_.isEmpty)) {
      return Point(physics.x, 0.0)
    }

    // x方向の力：従来の力学モデルのx成分を使用
    val xForce = physics.x

    val parentYForce = parentOpt.map { parent =>
      val targetY = parent.view.rect.center.y + params.verticalGap
      val dy = targetY - self.y
      // デッドゾーン：目標位置から一定距離以内なら力を加えない
      val deadZone = params.verticalGap * 0.1
      if (math.abs(dy) < deadZone) {
        0.0
      } else if (dy > 0) {
        // 親より下にいる場合：上方向に引っ張る
        (dy - deadZone) * params.verticalStrength
      } else {
        // 親より上にいる場合：下方向に引っ張る
        (dy + deadZone) * params.verticalStrength
      }
    }.getOrElse(0.0)

    Point(xForce, parentYForce)
  }

  // 強制配置：子ノードを親ノードの真下にリンク順で並べる
  def arrangeChildren(graph: Graph): Unit = {
    val ctx = buildContext(graph)
    val nodeSpacing = 10.0     // ノード間の最小スペース（ノードサイズ1つ分）
    val verticalGap = 80.0     // 親子間の垂直距離
    
    // 各ノードの部分木の幅を計算
    def calculateSubtreeWidth(node: Node): Double = {
      val children = ctx.orderedChildren.getOrElse(node, Seq.empty)
      if (children.isEmpty) {
        nodeSpacing
      } else if (children.size == 1) {
        // 子が1つだけの場合は、その子の幅をそのまま使用
        calculateSubtreeWidth(children.head)
      } else {
        // 複数の子がある場合：各子の幅の合計
        children.map(calculateSubtreeWidth).sum
      }
    }
    
    // ノードとその子孫を配置
    def arrange(node: Node, centerX: Double, topY: Double): Unit = {
      val children = ctx.orderedChildren.getOrElse(node, Seq.empty)
      
      // ノード自身を中央に配置
      val currentCenter = node.view.rect.center
      val dx = centerX - currentCenter.x
      val dy = topY - currentCenter.y
      node.view.rect = node.view.rect.movedBy(Point(dx, dy))
      
      if (children.nonEmpty) {
        if (children.size == 1) {
          // 子が1つだけの場合：親の真下に配置
          val child = children.head
          val childY = topY + verticalGap
          arrange(child, centerX, childY)
        } else {
          // 複数の子がある場合：左から順に配置
          val childWidths = children.map(calculateSubtreeWidth)
          val totalWidth = childWidths.sum
          
          // 子ノードたちを左から順に配置
          var currentX = centerX - totalWidth / 2.0
          for ((child, width) <- children.zip(childWidths)) {
            val childCenterX = currentX + width / 2.0
            val childY = topY + verticalGap
            
            // 再帰的に子ノードとその子孫を配置
            arrange(child, childCenterX, childY)
            
            currentX += width
          }
        }
      }
    }
    
    // ルートノードから開始
    if (!graph.rootNode.isRoot) {
      arrange(graph.rootNode, 0.0, 0.0)
    } else {
      for (child <- graph.rootNode.childNodes) {
        arrange(child, child.view.rect.center.x, child.view.rect.center.y)
      }
    }
  }

}
