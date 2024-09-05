package Cell.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Popup extends JDialog implements ActionListener{
    private JButton button;

    public Popup(String title, String message) {
        setTitle(title);

        setSize(300, 150);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel s = new JLabel(message);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(s, gbc);

        button = new JButton("OK");
        button.addActionListener(this);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(button, gbc);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    public void showPopup(){
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button) {
            dispose();
        }
    }
}
