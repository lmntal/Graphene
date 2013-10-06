package unyo.gui;

import java.awt.Graphics;

import javax.swing.JPanel;

import unyo.gui.renderer.VisualGraph;
import unyo.gui.renderer.DefaultRenderer;
import unyo.gui.renderer.GraphicsContext;
import unyo.gui.renderer.DefaultMover;
import unyo.entity.Graph;

@SuppressWarnings("serial")
public class GraphPanel extends JPanel {

    private final VisualGraph visualGraph = new VisualGraph();
    private GraphicsContext graphicsContext = new GraphicsContext();

    private final DefaultMover mover = new DefaultMover();

    public GraphPanel() {
        super(true);

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

        DefaultRenderer r = new DefaultRenderer(g, graphicsContext);
        r.render(visualGraph);
    }
}
