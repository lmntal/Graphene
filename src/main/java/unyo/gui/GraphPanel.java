package unyo.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import unyo.gui.renderer.VisualGraph;
import unyo.gui.renderer.DefaultRenderer;
import unyo.gui.renderer.GraphicsContext;
import unyo.gui.renderer.DefaultMover;
import unyo.entity.Graph;
import unyo.Env;

@SuppressWarnings("serial")
public class GraphPanel extends JPanel {

    private final VisualGraph visualGraph = new VisualGraph();
    private final GraphicsContext graphicsContext = new GraphicsContext(new unyo.util.Dimension(Env.frameWidth(), Env.frameHeight()));

    private final DefaultMover mover = new DefaultMover();

    public GraphPanel() {
        super(true);

        MouseAdapter adapter = new MouseAdapter() {
            private boolean isDragging = false;
            private Point prevPoint = null;

            @Override
            public void mousePressed(MouseEvent e) {
                isDragging = true;
                prevPoint = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDragging) return;
                if (prevPoint == null) return;

                Point p = e.getPoint();
                graphicsContext.moveBy(new unyo.util.Point(prevPoint.x - p.x, prevPoint.y - p.y));
                prevPoint = p;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
                prevPoint = null;
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        mover.move(visualGraph);
                        repaint();
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();
    }

    public void setGraph(Graph graph) {
        visualGraph.rewrite(graph);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        DefaultRenderer r = new DefaultRenderer(g2d, graphicsContext);
        r.render(visualGraph);
    }
}
