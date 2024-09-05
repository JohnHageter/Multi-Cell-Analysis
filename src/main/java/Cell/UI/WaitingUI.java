package Cell.UI;

import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WaitingUI extends JDialog implements ActionListener {
    private JButton button;
    private Runnable task;

    public WaitingUI(String title, String text) {
        super(IJ.getInstance(), title, false);
        IJ.protectStatusBar(false);

        setSize(400, 200);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        setAlwaysOnTop(true);

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(button);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public void setTask(Runnable task) {
        this.task = task;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("OK")) {
            if (task != null) {
                new Thread(task).start();
            }
            dispose();
        }
    }
}
