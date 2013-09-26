package unyo.gui;

import java.awt.Graphics;

import javax.swing.JPanel;

import unyo.gui.renderer.DefaultRenderer;
import unyo.gui.renderer.ViewContext;
import unyo.entity.Graph;

@SuppressWarnings("serial")
public class GraphPanel extends JPanel {

    private Graph graph = null;
    private ViewContext viewContext = new ViewContext();

    public GraphPanel() {
        super(true);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        if (graph == null) return;

        DefaultRenderer r = new DefaultRenderer(g, viewContext);
        r.render(graph);
    }
}
