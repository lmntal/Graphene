package unyo.gui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.Dimension;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import unyo.runtime.LMNtalRuntime;
import unyo.Env;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {

    private static MainFrame mainFrame = null;
    public static MainFrame instance() {
        if (mainFrame == null) {
            mainFrame = new MainFrame();
        }
        return mainFrame;
    }


    private MainFrame() {
        super();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initMenuBar();
        initPanel();
        initListener();
    }

    private JMenuBar menuBar;
    public void initMenuBar() {
        menuBar = new JMenuBar();

        {
            JMenu menuFile = new JMenu("File");
            {
                JMenuItem openItem = new JMenuItem("Open File");
                openItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        MainFrame.this.openFileChooser();
                    }
                });
                openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
                menuFile.add(openItem);
            }

            menuBar.add(menuFile);
        }

        setJMenuBar(menuBar);
    }

    private GraphPanel graphPanel;
    public void initPanel() {
        Env env = new Env();
        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(env.frameWidth(), env.frameHeight()));
        graphPanel.setFocusable(true);
        graphPanel.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (runtime.hasNext()) {
                        unyo.entity.Graph graph = runtime.next();
                        graphPanel.setGraph(graph);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
        add(graphPanel);
    }

    public void initListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (runtime != null) {
                    runtime.exit();
                }
            }
        });
    }


    private LMNtalRuntime runtime = null;
    void openFileChooser() {
        FileFilter filter = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
        JFileChooser fileChooser = new JFileChooser("~/");
        fileChooser.addChoosableFileFilter(filter);

        int selected = fileChooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            runtime = new LMNtalRuntime(file, java.util.Arrays.asList("-O", "--hide-rule", "--hide-ruleset"));
            unyo.entity.Graph graph = runtime.next();
            graphPanel.setGraph(graph);
        }
    }

}
