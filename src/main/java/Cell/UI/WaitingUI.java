package Cell.UI;

import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class WaitingUI extends JDialog implements ActionListener, KeyListener {
    private JButton button;

    public WaitingUI(String title, String text) {
        super(IJ.getInstance(), title, true); // Set modal to true
        IJ.protectStatusBar(false);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 1, 1, 1);

        JLabel label = new JLabel(text);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(label, gbc);

        button = new JButton("OK");
        button.addActionListener(this);
        button.addKeyListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(button);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    public void showDialog() {
        setVisible(true); // Show the dialog
    }

    public void close() {
        setVisible(false);
        dispose(); // Clean up resources
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("OK")) {
            close();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ESCAPE || e.getKeyChar() == KeyEvent.VK_ENTER) {
            close();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
