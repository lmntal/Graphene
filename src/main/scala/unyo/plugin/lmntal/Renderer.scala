package unyo.plugin.lmntal


class DefaultRenderer extends LMNtal.Renderer {

  import java.awt.{Graphics,Graphics2D,Color,BasicStroke}

  import unyo.core.gui.GraphicsContext
  import unyo.model._
  import unyo.swing.Graphics.GraphicsExt
  import unyo.util._
  import unyo.util.Geometry._


  lazy val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext

  // 1. エッジが出ていないノードを描画する
  // 2. エッジを描画する
  // 3. 残りのノードを描画する
  // の順で綺麗に描画できる(上下関係がおかしくなることが無い)
  // エッジが出ていて、かつ子ノードを持つノードはこの方法だとおかしくなる可能性がある
  def renderAll(gg: Graphics, graph: Graph): Unit = {
    val g = gg.asInstanceOf[Graphics2D];

    renderGrid(g)

    if (graph == null) return

    if (LMNtal.config.isAutoFocusEnabled) gctx.wRect = graph.rootNode.view.rect

    for (node <- graph.rootNode.childNodes) renderAloneNode(g, node)
    renderEdges(g, graph)
    for (node <- graph.rootNode.childNodes) renderNonAloneNode(g, node)
  }

  private def renderAloneNode(g: Graphics2D, node: Node): Unit = {
    if (node.neighborNodes.isEmpty) {
      renderNode(g, node)
      for (n <- node.childNodes) renderAloneNode(g, n)
    }
  }

  private def renderNonAloneNode(g: Graphics2D, node: Node): Unit = {
    def _renderNonAloneNode(g: Graphics2D, node: Node, isParentRendered: Boolean): Unit = {
      for (n <- node.childNodes) _renderNonAloneNode(g, n, isParentRendered && node.neighborNodes.isEmpty)
      if (!node.neighborNodes.isEmpty || !isParentRendered) renderNode(g, node)
    }
    _renderNonAloneNode(g, node, true)
  }

  private def renderGrid(g: Graphics2D): Unit = {
    val bx = gctx.wCenter.x - gctx.wSize.width / 2
    val ex = gctx.wCenter.x + gctx.wSize.width / 2
    val by = gctx.wCenter.y - gctx.wSize.height / 2
    val ey = gctx.wCenter.y + gctx.wSize.height / 2
    g.setColor(Palette.concrete)
    for (x <- (bx.toInt/100*100) to (ex.toInt/100*100, 100)) g.drawLine(x, by, x, ey)
    for (y <- (by.toInt/100*100) to (ey.toInt/100*100, 100)) g.drawLine(bx, y, ex, y)
  }

  private val font = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 16)

  private val atomStroke = new BasicStroke(4.0f)
  private val memStroke = new BasicStroke(2.0f)
  private val linkStroke = new BasicStroke(1.5f)

  private val memArc = Dim(6, 6)

  private def renderNode(g: Graphics2D, node: Node): Unit = {
    val view = node.view
    val rect = view.rect

    if (view.willDisappear) {
      val oldPaint = g.getPaint
      g.setPaint(new java.awt.RadialGradientPaint(rect.center.x.toInt, rect.center.y.toInt, 30, Array(0.0f, 1.0f), Array(Color.RED, new Color(255, 255, 255, 0))))
      g.fillOval(rect.pad(Padding(-30, -30, -30, -30)))
      g.setPaint(oldPaint)
    }

    if (view.didAppear) {
      val oldPaint = g.getPaint
      g.setPaint(new java.awt.RadialGradientPaint(rect.center.x.toInt, rect.center.y.toInt, 30, Array(0.7f, 1.0f), Array(Color.GREEN, new Color(255, 255, 255, 0))))
      g.fillOval(rect.pad(Padding(-30, -30, -30, -30)))
      g.setPaint(oldPaint)
    }

    node.attr match {
      case Atom => {
        g.setFont(font)
        g.setColor(node.view.color)
        g.drawString(node.name, rect.point)

        g.setColor(if (view.selected) Palette.asbestos else Color.WHITE)
        g.fillOval(rect)

        g.setStroke(atomStroke)
        g.setColor(node.view.color)
        g.drawOval(rect)
      }
      case HLAtom => {
        g.setFont(font)
        g.setColor(node.view.color)
        g.drawString(node.name, rect.point)
        g.fillOval(rect)
      }
      case Mem => {
        g.setColor(Color.WHITE)
        g.fillRoundRect(rect, memArc)

        g.setStroke(memStroke)
        g.setColor(node.view.color)
        g.drawRoundRect(rect, memArc)
      }
      case _ =>
    }
  }

  private def renderEdges(g: Graphics2D, graph: Graph): Unit =
    for (e <- graph.allEdges) renderEdge(g, e)

  private def renderEdge(g: Graphics2D, edge: Edge): Unit = {
    g.setColor(Palette.concrete)
    g.setStroke(linkStroke)
    g.drawLine(edge.source.view.rect.center, edge.target.view.rect.center)
  }
}
