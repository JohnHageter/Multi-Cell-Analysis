package Cell.Frame;

import Cell.Processing.CalciumProcessor;
import Cell.Processing.MotionCorrection;
import Cell.UI.WaitingUI;
import Cell.Utils.CellData;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import ij.IJ;

public class CellManager extends JFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<CellData> {
    private static CellManager instance;
    private JList<CellData> cellList;
    private int nButtons = 0;
    private GridBagLayout layout = new GridBagLayout();
    private static JPanel panel = new JPanel();
    private static GridBagConstraints gbc = new GridBagConstraints();
    private boolean allowRecording;

    public CellManager() {
        super("Cell Manager");
        if (instance != null) {
            instance.toFront();
            return;
        }

        instance = this;
        cellList = new JList<>();
        showCellManager();
    }

    private void showCellManager() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addMouseListener(this);
        addMouseWheelListener(this);

        panel.setLayout(layout);

        addButton("Register", 2, 0, 1, 1);
        addButton("Apply group(s)", 2, 1, 1, 1);
        addButton("Select multiple", 2, 2, 1, 1);
        addButton("Analyze", 2, 3, 1, 1);
        addButton("Convert stack to DF/F", 2, 4, 1, 1);
        addButton("Load", 2, 5, 1, 1);
        addButton("Save", 2, 6, 1, 1);
        addButton("More...", 2, 8, 1, 1);

        addButton("Cells", 0, nButtons, 1, 1);
        nButtons--;
        addButton("Groups", 1, nButtons, 1, 1);
        nButtons--;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = nButtons;

        JScrollPane roiPane = new JScrollPane(cellList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        layout.setConstraints(roiPane, gbc);
        panel.add(roiPane, gbc);

        this.add(panel);
        this.pack();
        this.setVisible(true);
    }

    void addButton(String name, int gridx, int gridy, int gridwidth, int gridheight) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;

        gbc.fill = GridBagConstraints.BOTH;
        JButton b = new JButton(name);
        b.addActionListener(this);
        b.addMouseListener(this);
        panel.add(b, gbc);
        nButtons++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label == null) {
            return;
        }
        allowRecording = true;

        switch (label) {
            case "Register":
                WaitingUI waitingUI = new WaitingUI("Motion Correction", "Select Rectangular ROI");
                waitingUI.showDialog();

                MotionCorrection mc = new MotionCorrection(IJ.getImage().getRoi());
                mc.normXCorr(IJ.getImage());
                logAction("Registration completed");
                break;
            case "Apply group(s)":
                logAction("Apply group(s)");
                break;
            case "Select multiple":
                logAction("Select multiple");
                break;
            case "Analyze":
                logAction("Analyze");
                break;
            case "Convert stack to DF/F":
                convertStackToDF();
                break;
            case "Load":
                logAction("Load");
                break;
            case "Save":
                logAction("Save");
                break;
            case "More...":
                logAction("More...");
                break;
            case "Groups":
                logAction("Switch to group tab");
                break;
            case "Cells":
                logAction("Switch to Cell tab");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + label);
        }
    }


    private void convertStackToDF() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                CalciumProcessor cp = new CalciumProcessor();
                cp.convertToDF(IJ.getImage(), 20, 80);
                return null;
            }

            @Override
            protected void done() {
                logAction("Conversion completed");
            }
        }.execute();
    }

    private void logAction(String message) {
        System.out.println(message); // Replace with your logging method
    }

    @Override
    public void itemStateChanged(ItemEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {}

    @Override
    public Iterator<CellData> iterator() {
        return null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {}
}
