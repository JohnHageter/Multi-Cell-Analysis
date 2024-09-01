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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class CellManager extends JFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<CellData> {
    private static CellManager instance;
    private JList<String> cellList;
    private DefaultListModel<String> listModel;
    private int nButtons = 0;
    private GridBagLayout layout = new GridBagLayout();
    private static JPanel panel = new JPanel();
    private JPopupMenu pm;
    private static GridBagConstraints gbc = new GridBagConstraints();
    private boolean allowRecording;

    public ArrayList<CellData> cells = new ArrayList<>();

    public CellManager() {
        super("Cell Manager");
        if (instance != null) {
            instance.toFront();
            return;
        }

        instance = this;
        cellList = new JList<>();
        listModel = new DefaultListModel<>();
        cellList.setModel(listModel);
        showCellManager();
    }

    private void showCellManager() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addMouseListener(this);
        addMouseWheelListener(this);

        panel.setLayout(layout);

        addButton("Register",               2, 0, 1, 1);
        addButton("Apply group(s)",         2, 1, 1, 1);
        addButton("Select multiple",        2, 2, 1, 1);
        addButton("Analyze",                2, 3, 1, 1);
        addButton("Convert stack to DF/F",  2, 4, 1, 1);
        addButton("Load",                   2, 5, 1, 1);
        addButton("Save",                   2, 6, 1, 1);
        addButton("More...",                2, 8, 1, 1);
        addMoreMenu();

        addButton("Cells", 0, nButtons, 1, 1);
        nButtons--;
        addButton("Groups", 1, nButtons, 1, 1);
        nButtons--;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = nButtons;

        listModel = new DefaultListModel<>();
        cellList.setModel(listModel);
        cellList.addListSelectionListener(this);
        cellList.addKeyListener(IJ.getInstance());
        cellList.addMouseListener(this);
        cellList.addMouseWheelListener(this);
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

    void addMoreMenu() {
        pm = new JPopupMenu();
        addMenuItem("Import from Roi Manager");
        addMenuItem("Set standard name");
    }

    void addMenuItem(String s) {
        JMenuItem mi = new JMenuItem(s);
        mi.addActionListener(this);
        pm.add(mi);
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
                importFromROIManager();
                break;
            case "Save":
                logAction("Save");
                break;
            case "More...":
                JButton source = (JButton) e.getSource();
                pm.show(source, source.getWidth()/2, source.getHeight()/2);
                break;
            case "Groups":
                logAction("Switch to group tab");
                break;
            case "Cells":
                logAction("Switched to cell tab");
                break;
            case "Import from Roi Manager":
                importFromROIManager();
                break;
            case "Set standard name":
                nameCells();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + label);
        }
    }

    private void nameCells() {
        GenericDialog gd = new GenericDialog("Rename Cells");
        gd.addMessage("Enter prefix for naming convention");
        gd.addStringField("Prefix", "");
        gd.showDialog();

        String prefix = gd.getNextString();

        for (int i = 0; i < listModel.size(); i++) {
            renameCell(i, prefix + "_" + i);
        }

    }

    private void renameCell(int index, String newName) {
        if(index >=0 && index < listModel.getSize()) {
            listModel.set(index, newName);
            cells.get(index).setName(newName);
        } else {
            IJ.log("Index out of bounds for renaming");
        }
    }

    private void importFromROIManager() {
        RoiManager rm;
        if (RoiManager.getInstance() == null) {
            rm = new RoiManager(true);
        } else {
            rm = RoiManager.getInstance();
        }

        ArrayList<Roi> rois = new ArrayList<>(Arrays.asList(rm.getRoisAsArray()));

        if (rois.isEmpty()) {
            popupError("Roi Manager is empty.");
            return;
        }

        for (Roi roi : rois) {
            CellData cd = new CellData(roi);
            cells.add(cd);
            listModel.addElement(cd.getName());
        }

    }

    private void popupError(String s) {
        new WaitingUI("Error", s).showDialog();
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
        IJ.log(message);
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
