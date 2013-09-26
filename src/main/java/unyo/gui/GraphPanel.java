package unyo.gui;

import java.awt.Graphics;

import javax.swing.JPanel;

import unyo.gui.renderer.DefaultRenderer;

@SuppressWarnings("serial")
public class GraphPanel extends JPanel {

    public GraphPanel() {
        super(true);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        DefaultRenderer r = new DefaultRenderer(g);
        r.render();
    }
}
