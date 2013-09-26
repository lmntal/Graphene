package unyo.gui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import unyo.runtime.LMNtalRuntime;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {


    public MainFrame() {
        super();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initMenuBar();
        initPanel();
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
        graphPanel = new GraphPanel();
        add(graphPanel);
    }

    void openFileChooser() {
        FileFilter filter = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
        JFileChooser fileChooser = new JFileChooser("~/");
        fileChooser.addChoosableFileFilter(filter);

        int selected = fileChooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            LMNtalRuntime.execute(file, java.util.Arrays.asList("-O", "--hiderule", "--hideruleset"));
        }
    }

}
