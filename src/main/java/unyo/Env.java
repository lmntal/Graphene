package unyo;

import java.awt.Toolkit;
import java.awt.Dimension;

public class Env {
    private Env() {}

    public static int frameWidth() {
        return (int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 2 / 3);
    }

    public static int frameHeight() {
        return (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 2 / 3);
    }
}
