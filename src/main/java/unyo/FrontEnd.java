package unyo;

import unyo.gui.MainFrame;

public class FrontEnd {

    public static void main(String[] args) {
        MainFrame frame = MainFrame.instance();
        frame.setVisible(true);
        frame.pack();
    }

}
