package Cell.Frame;

import Cell.Analysis.Exporter;
import Cell.Annotation.SelectionGrouping;
import Cell.Processing.CalciumProcessor;
import Cell.Processing.MotionCorrection;
import Cell.UI.Popup;
import Cell.Utils.CellData;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import Cell.Utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import Cell.UI.WaitingUI;

import java.awt.Color;
import java.util.prefs.BackingStoreException;


public class CellManager extends JFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<CellData> {
    private static CellManager instance;
    private JList<String> cellList;
    private DefaultListModel<String> listModel;
    private int nButtons = 0;
    private GridBagLayout layout = new GridBagLayout();
    private static JPanel panel = new JPanel();
    private JPopupMenu pm;
    private JCheckBox showAll;
    private static GridBagConstraints gbc = new GridBagConstraints();
    private boolean showingGroups = false;

    public ArrayList<CellData> cells = new ArrayList<>();
    private Overlay allCellOverlay;
    private Overlay allGroupOverlay;

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

    public static CellManager getInstance() {
        if (instance == null){
            instance = new CellManager();
        }
        return instance;
    }

    public static void showCellManager() {
        CellManager manager = getInstance();
        manager.initializeUI();
        if (!manager.isVisible()) {
            manager.setVisible(true);
        }
        manager.toFront();
    }

    private void initializeUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addMouseListener(this);
        addMouseWheelListener(this);

        panel.setLayout(layout);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(1, 1, 1, 1);


        gbc.weightx = 1;
        gbc.weighty = 0;
        addButton("Register",               2, 0, 1, 1);
        addButton("Apply group(s)",         2, 1, 1, 1);
        addButton("Select multiple",        2, 2, 1, 1);
        addButton("Analyze",                2, 3, 1, 1);
        addButton("Convert stack to DF/F",  2, 4, 1, 1);
        addButton("Load from ROI Manager",  2, 5, 1, 1);
        addButton("Plot",                   2, 6, 1, 1);
        addButton("More...",                2, 8, 1, 1);
        addMoreMenu();

        addButton("Cells", 0, nButtons, 1, 1);
        nButtons--;
        addButton("Groups", 1, nButtons, 1, 1);
        nButtons--;

        showAll = new JCheckBox("Show all");
        showAll.addItemListener(this);
        gbc.gridx = 2;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(showAll, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = nButtons;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

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

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panel.revalidate();
                panel.repaint();
            }
        });
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

        switch (label) {
            case "Register":
                if(IJ.getImage()!= null){
                    WaitingUI waitingUI = new WaitingUI("Motion Correction", "Select template ROI");
                    waitingUI.setTask(() -> {
                        MotionCorrection mc = new MotionCorrection(IJ.getImage().getRoi());
                        mc.normXCorr(IJ.getImage());
                    });
                } else {
                    IJ.noImage();
                }
                break;
            case "Apply group(s)":
                if(IJ.getImage() != null){
                    groupSelection();
                } else {
                    IJ.noImage();
                }
                break;
            case "Select multiple":
                logAction("Select multiple");
                break;
            case "Analyze":
                if (IJ.getImage()!= null) {
                    Exporter exporter = new Exporter(cells);
                    try {
                        exporter.exportData();
                    } catch (BackingStoreException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    IJ.noImage();
                }
                break;
            case "Convert stack to DF/F":
                convertStackToDF();
                break;
            case "Load from ROI Manager":
                importFromROIManager();
                break;
            case "Plot":
                Plot.main(null);
                break;
            case "More...":
                JButton source = (JButton) e.getSource();
                pm.show(source, source.getWidth()/2, source.getHeight()/2);
                break;
            case "Groups":
                showGroups();
                break;
            case "Cells":
                showCells();
                break;
            case "Set standard name":
                nameCells();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + label);
        }
    }

    private void groupSelection() {
        String groupName;

        GenericDialog gd = new GenericDialog("Apply grouping");
        gd.addStringField("Group name: ", "");
        gd.showDialog();

        if (gd.wasOKed()) {
            groupName = gd.getNextString();
        } else {
            groupName = "";
        }

        IJ.setTool("polygon");
        WaitingUI waitingUI = new WaitingUI("Apply group", "Select template ROI");

        waitingUI.setTask(() -> {
            Roi groupingRoi = IJ.getImage().getRoi();
            if (groupingRoi == null) {
                IJ.error("ROI needed for cell grouping");
                return;
            }

            new SelectionGrouping().applyGroup(cells, groupingRoi, groupName);
            groupingRoi.setStrokeColor(Utils.randomColor());
            ImagePlus imp = IJ.getImage();
            Overlay overlay = imp.getOverlay();
            if (overlay == null) {
                overlay = new Overlay();
                imp.setOverlay(overlay);
            }

            overlay.add(groupingRoi);
            imp.updateAndDraw();
        });
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

        Set<String> roiNames = new HashSet<>();
        for (Roi roi : rois) {
            roiNames.add(roi.getName());
        }

        Iterator<CellData> iterator = cells.iterator();
        while (iterator.hasNext()) {
            CellData cd = iterator.next();
            if (!roiNames.contains(cd.getName())) {
                iterator.remove();
                listModel.removeElement(cd.getName());
            }
        }

        for (Roi roi : rois) {
            if (!roiNames.contains(roi.getName())) continue;

            boolean nameExists = false;
            for (CellData cd : cells) {
                if (Objects.equals(roi.getName(), cd.getName())) {
                    nameExists = true;
                    break;
                }
            }

            if (!nameExists) {
                CellData newCd = new CellData(roi);
                cells.add(newCd);
                listModel.addElement(newCd.getName());
            }
        }
    }

    private void popupError(String s) {
        new Popup("Error", s).showPopup();
    }

    private void popupWarning(String s) {
        new Popup("Warning", s).showPopup();
    }

    private void convertStackToDF() {
        if (WindowManager.getCurrentImage() == null) {
            IJ.noImage();
            return;
        }

        int begin = -1, end = -1;

        GenericDialog gd = new GenericDialog("Fluorescence conversion");
        gd.addMessage("Input baseline (ex. 0-60)");
        gd.addStringField("Baseline: ", "");
        gd.showDialog();

        if (gd.wasOKed()) {
            String input = gd.getNextString().trim();

            if (!input.contains("-")) {
                IJ.error("Input must be in the format 'begin-end'.");
                return;
            }

            String[] baselinePoints = input.split("-");

            if (baselinePoints.length != 2) {
                IJ.error("Input must contain a single '-' to separate the begin and end slice of the baseline");
                return;
            }

            try {
                begin = Integer.parseInt(baselinePoints[0].trim());
                end = Integer.parseInt(baselinePoints[1].trim());

                if (begin > end) {
                    IJ.error("Beginning slice cannot be greater than the ending slice.");
                    return;
                }
            } catch (NumberFormatException n) {
                IJ.error("Input must contain valid integers" ,n.getMessage());
            }

            if (IJ.getImage().getStack().size() < end || begin < 1) {
                IJ.error("Beginning and ending values must be within the number of slices in the stack");
                return;
            }

            if(begin == end) {
                IJ.error("Baseline duration must be greater than 0 slices");
                return;
            }

            int finalBegin = begin;
            int finalEnd = end;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    CalciumProcessor cp = new CalciumProcessor();
                    cp.convertToDF(IJ.getImage(), finalBegin, finalEnd);
                    //IJ.log("Conversion complete");
                    return null;
                }
            }.execute();
        } else {
            IJ.error("Baseline needed for conversion");
        }
    }

    private void logAction(String message) {
        IJ.log(message);
    }

    ImagePlus getImage() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp==null) {
            popupError("There are no images open.");
            return null;
        } else
            return imp;
    }

    public int getCount() {
        return listModel!=null?listModel.getSize():0;
    }

    public CellData getCellData(int index) {
        if (index<0 || index>getCount()) {
            return null;
        }

        return (CellData) cells.get(index);
    }

    public int getCellDataIndex(CellData cd) {
        int n = getCount();
        for (int i = 0; i < n; i++){
            CellData cd2 = (CellData)cells.get(i);
            if(cd == cd2) {
                return i;
            }
        }
        return -1;
    }

    public int getSelectedIndex() {
        return cellList.getSelectedIndex();
    }

    public synchronized CellData[] getCellDataAsArray() {
        CellData[] arr = new CellData[cells.size()];
        return cells.toArray(arr);
    }

    public String getName(int index) {
        if(index>=0 && index<getCount()){
            return (String)listModel.getElementAt(index);
        } else {
            return null;
        }
    }

    public int getIndex(String name) {
        CellData[] celldata = getCellDataAsArray();
        for (int i = 0; i < cells.size(); i++){
            if (name.equals(celldata[i].getName())) {
                return i;
            }
        }
        return -1;
    }

    public void select(ImagePlus imp, int index) {
        deselect();
        if(index<0) {
            return;
        }

        cellList.setSelectedIndex(index);
        if (imp==null) {
            imp = WindowManager.getCurrentImage();
        } else {
            //restore state
        }
    }

    private void deselect() {
        cellList.clearSelection();
    }

    public void deselect(CellData cd) {
        int[] indices = getSelectedIndices();
        if (indices.length ==1 && listModel.getSize()>0) {
            String label = listModel.getElementAt(indices[0]);
            if(label.equals(cd.getName())) {
                deselect();
            }
        }
    }

    private int[] getSelectedIndices() {
        return cellList.getSelectedIndices();
    }

    public void select(int index, boolean shiftKeyDown, boolean altKeyDown) {
        if(!(shiftKeyDown || altKeyDown)) {
            select(index);
        }
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            return;
        }
        CellData cd = (CellData)cells.get(index);
        if (cd!= null) {
            //???
        }
    }

    public void select(int index) {
        select(null, index);
    }

    private void showCells() {
        listModel.clear();
        for (CellData cell : cells) {
            listModel.addElement(cell.getName());
        }
        showingGroups = false;

        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) return;

        allCellOverlay = new Overlay();
        for (CellData cell : cells) {
            Roi cellRoi = cell.getCellRoi();
            if (cellRoi != null) {
                cellRoi.setStrokeColor(Color.RED);
                allCellOverlay.add(cellRoi);
            }
        }
        imp.setOverlay(allCellOverlay);
        imp.updateAndDraw();
    }

    private void showGroups() {
        listModel.clear();
        Set<String> groups = new HashSet<>();
        for (CellData cd : cells) {
            if (cd.getGroup() != null) {
                groups.add(cd.getGroup());
            }
        }
        for (String group : groups) {
            listModel.addElement(group);
        }
        showingGroups = true;

        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) return;

        allGroupOverlay = new Overlay();
        for (CellData cd : cells) {
            Roi groupRoi = cd.getGroupRoi();
            if (groupRoi != null) {
                groupRoi.setStrokeColor(Color.MAGENTA);
                groupRoi.setStrokeWidth(2.0f);
                allGroupOverlay.add(groupRoi);
            }
        }
        imp.setOverlay(allGroupOverlay);
        imp.updateAndDraw();
    }

    private void showAllCells() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if(imp == null) {
            return;
        }

        allCellOverlay = new Overlay();

        for (CellData cell : cells) {
            Roi cellRoi = cell.getCellRoi();
            if (cellRoi != null) {
                cellRoi.setStrokeColor(Color.RED);
                allCellOverlay.add(cellRoi);
            }
        }

        imp.setOverlay(allCellOverlay);
        imp.updateAndDraw();
    }

    private void removeOverlay() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            return;
        }

        imp.setOverlay(null);
        imp.updateAndDraw();
    }

    private void showAllGroups() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            return;
        }

        allGroupOverlay = new Overlay();

        // Loop through all cells, and if they belong to a group, display the group's ROI
        for (CellData cell : cells) {
            if (cell.getGroup() != null) {
                Roi groupRoi = cell.getGroupRoi();
                if (groupRoi != null) {
                    groupRoi.setStrokeColor(Color.MAGENTA);
                    allGroupOverlay.add(groupRoi);
                }
            }
        }

        imp.setOverlay(allGroupOverlay);
        imp.updateAndDraw();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();

        if (source == showAll && !showingGroups) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showAllCells();
            } else {
                removeOverlay();
            }
        } else if (source == showAll && showingGroups) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showAllGroups();
            } else {
                removeOverlay();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) e.consume();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x=e.getX();
        int y=e.getY();

        if (e.isPopupTrigger() || e.isMetaDown()) {
            pm.show(e.getComponent(),x,y);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        synchronized (this) {
            int index = cellList.getSelectedIndex();
            int rot = e.getWheelRotation();
            if (rot<-1) {rot = -1;}
            if (rot >1) {rot = 1;}
            index += rot;
            if (index<0) {index = 0;}
            if (index>getCount()) {index = getCount();}

            select(index);
        }
    }

    @Override
    public Iterator<CellData> iterator() {

        return new Iterator<CellData>() {
            private int index = -1;
            final CellManager cm = CellManager.getInstance();

            @Override
            public boolean hasNext() {
                return index + 1 < cm.getCount();
            }

            @Override
            public CellData next() {
                if (index+1<cm.getCount()) {
                    return cm.getCellData(++index);
                } else {
                    return null;
                }
            }
        };
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            instance = null;
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (getCount() == 0) {
            return;
        }

        int selectedIndex = cellList.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        CellData cd = cells.get(selectedIndex);
        ImagePlus image = IJ.getImage();
        if (image == null) {
            return;
        }

        if (!showingGroups) {
            Roi cellRoi = cd.getCellRoi();
            if (cellRoi != null) {
                cellRoi.setStrokeColor(Color.RED);
                IJ.getImage().setRoi(cellRoi);
                IJ.getImage().updateAndDraw();
            }
        } else {
            Roi groupRoi = cd.getGroupRoi();
            if (groupRoi != null) {
                groupRoi.setStrokeColor(Color.MAGENTA);
                IJ.getImage().setRoi(groupRoi);
                IJ.getImage().updateAndDraw();
            }
        }
    }

}
