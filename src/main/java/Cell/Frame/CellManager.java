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
import java.awt.Color;

import Cell.Utils.Test;
import Cell.Utils.Utils;
import Cell.UI.WaitingUI;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.plugin.frame.RoiManager;

import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;


public class CellManager extends JFrame implements ActionListener, ItemListener, MouseListener, MouseWheelListener, ListSelectionListener, Iterable<CellData> {
    private static CellManager instance;
    private JList<String> cellList;
    private JList<String> groupList;
    private DefaultListModel<String> listModel;
    private int nButtons = 0;
    private GridBagLayout layout = new GridBagLayout();
    private static JPanel panel = new JPanel();
    private JPopupMenu pm;
    private Color defaultColor;
    private JCheckBox showAll;
    private static GridBagConstraints gbc = new GridBagConstraints();
    private boolean showingGroups = false;

    public ArrayList<CellData> cells = new ArrayList<>();
    private Overlay allCellOverlay;
    private Overlay allGroupOverlay;
    private int defaultlwd;


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

        groupList = new JList<>();
        groupList.setModel(listModel);
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
        addButton("Register",                   2, 0, 1, 1);
        addButton("Apply group(s)",             2, 1, 1, 1);
        addButton("Analyze",                    2, 3, 1, 1);
        addButton("Convert stack to DF/F",      2, 4, 1, 1);
        addButton("Add cell [`]",               2, 5, 1, 1);
        addButton("Delete cell",                2, 6, 1, 1);
        addButton("Load from ROI Manager",      2, 7, 1, 1);
        addMoreMenu();

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        // Cells list
        JPanel cellsPanel = new JPanel(new BorderLayout());
        cellList.setModel(listModel);
        cellList.addListSelectionListener(this);
        cellList.addKeyListener(IJ.getInstance());
        cellList.addMouseListener(this);
        cellList.addMouseWheelListener(this);
        JScrollPane cellsScrollPane = new JScrollPane(cellList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cellsPanel.add(cellsScrollPane, BorderLayout.CENTER);

        // Groups list
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupList.setModel(listModel);
        groupList.addListSelectionListener(this);
        groupList.addKeyListener(IJ.getInstance());
        groupList.addMouseListener(this);
        groupList.addMouseWheelListener(this);
        JScrollPane groupsScrollPane = new JScrollPane(groupList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        groupsPanel.add(groupsScrollPane, BorderLayout.CENTER);

        cardPanel.add(cellsPanel, "Cells");
        cardPanel.add(groupsPanel, "Groups");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = nButtons + 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        layout.setConstraints(cardPanel, gbc);
        panel.add(cardPanel, gbc);


        showAll = new JCheckBox("Show all");
        showAll.addItemListener(this);
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        panel.add(showAll, gbc);

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;

        JPanel buttonPanel = getButtonPanel(cardLayout, cardPanel);
        layout.setConstraints(buttonPanel, gbc);
        panel.add(buttonPanel, gbc);
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

    private JPanel getButtonPanel(CardLayout cardLayout, JPanel cardPanel) {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton cellsButton = new JButton("Cells");
        JButton groupsButton = new JButton("Groups");
        JButton moreButton = new JButton("More...");

        ActionListener updateButtonAppearance = e -> {
            String command = e.getActionCommand();
            if (command.equals("Cells")) {
                cellsButton.setBackground(Color.LIGHT_GRAY);
                groupsButton.setBackground(null);
                cardLayout.show(cardPanel, "Cells");
            } else if (command.equals("Groups")) {
                groupsButton.setBackground(Color.LIGHT_GRAY);
                cellsButton.setBackground(null);
                cardLayout.show(cardPanel, "Groups");
            }
        };

        cellsButton.addActionListener(updateButtonAppearance);
        cellsButton.setActionCommand("Cells");
        groupsButton.addActionListener(updateButtonAppearance);
        groupsButton.setActionCommand("Groups");

        moreButton.addActionListener(e -> addMoreMenu());
        cellsButton.setBackground(Color.LIGHT_GRAY);

        buttonPanel.add(cellsButton);
        buttonPanel.add(groupsButton);
        buttonPanel.add(moreButton);
        return buttonPanel;
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
        addMenuItem("Test");
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
                if(WindowManager.getCurrentImage()!= null){
                    WaitingUI waitingUI = new WaitingUI("Motion Correction", "Select template ROI");
                    waitingUI.setTask(() -> {
                        MotionCorrection mc = new MotionCorrection(IJ.getImage().getRoi());
                        mc.normXCorr(IJ.getImage());
                    });
                } else {
                    IJ.noImage();
                    return;
                }
                break;
            case "Apply group(s)":
                if(WindowManager.getCurrentImage() != null){
                    groupSelection();
                } else {
                    IJ.noImage();
                }
                break;
            case "Select multiple":
                logAction("Select multiple");
                break;
            case "Analyze":
                if (WindowManager.getCurrentImage()!= null) {
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
            case "Add cell [`]":
                if(WindowManager.getCurrentImage() != null) {

                }
            case "Delete cell":
                delete();
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
            case "Debug tests":
                //Test.testGaussian();
                //Test.testSpikeDetection();
                Test.testBlur();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + label);
        }
    }

    public void addCell(Roi roi) {
        addCell(roi,false, null, -1);
    }

    boolean addCell(boolean namePrompt) {
        return addCell(null, namePrompt, null, -1);
    }

    boolean addCell(Roi roi, boolean namePrompt, Color color, int lwd) {
        if(listModel == null){
            popupError("<<Cell Manager uninitialized>>");
        }
        ImagePlus imp = roi==null?getImage():WindowManager.getCurrentImage();
        if(roi == null) {
            if(imp == null) {
                return false;
            }
            roi = imp.getRoi();
            if(roi ==null){
                popupError("No active selection");
                return false;
            }
        }
        if ((roi instanceof PolygonRoi) && ((PolygonRoi) roi).getNCoordinates()==0){
            return false;
        }
        if(color == null && roi.getStrokeColor()!=null){
            color = roi.getStrokeColor();
        } else if(color == null && defaultColor!=null){
            color = defaultColor;
        }
        if(lwd<0){
            int sw = (int)roi.getStrokeWidth();
            lwd = sw>1?sw:defaultlwd;
        }
        if(lwd>100) {lwd=1}
        int n = getCount();
        int saveCurrentSlice = imp!=null? imp.getCurrentSlice() : 0;
        if(n>0 && imp!=null && allowDuplicates) {
            Roi roi2 = (Roi)cells.get(n-1).getCellRoi();
            if(roi2!= null) {
                String label = (String)listModel.getElementAt(n-1);
                int slice2 = getSliceNumber(roi2, label);
                if(roi.equals(roi2) && (slice2==-1||slice2==imp.getCurrentSlice()) && imp.getID()==prevID){
                    return false;
                }
            }
        }
    }

    private void delete() {
        int count = getCount();
        if (count == 0) {
            popupError("The Cell manager is empty");
        }
        int[] indicies = getSelectedIndices();
        if(indicies.length == 0){
            String message = "Delete all items?";
            YesNoCancelDialog d = new YesNoCancelDialog(this, "Cell Manager", message);
            if(d.cancelPressed()){
                return;
            }
            if (!d.yesPressed()) {
                return;
            }
            indicies = getAllIndicies();
        }

        if (count == indicies.length){
            cells.clear();
            listModel.removeAllElements();
        } else {
            for (int i = count-1; i>=0; i--){
                boolean delete = false;
                for (int index : indicies) {
                    if (index == i) {
                        delete = true;
                    }
                    if (delete) {
                        if (EventQueue.isDispatchThread()) {
                            cells.remove(i);
                            cellList.remove(i);
                        } else {
                            deleteOnEDT(i);
                        }
                    }
                }
            }
        }

        updateShowAll();
    }

    private void updateShowAll() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {return;}
        if (showAll.isEnabled()){
            if (getCount() > 0) {
                CellData[] cells = getCellDataAsArray();
                Overlay ol = new Overlay();
                for (int i = 0; i < cells.length; i++) {
                    setOverlay(imp, ol);
                }
            }
        }
    }

    private void setOverlay(ImagePlus imp, Overlay overlay){
        if (imp == null) {
            return;
        }
        ImageCanvas ic = imp.getCanvas();
        if (ic == null) {
            if (imp.getOverlay()==null) {
                imp.setOverlay(overlay);
            }
            return;
        }
        ic.setShowAllList(overlay);
        imp.draw();
    }

    private void deleteOnEDT(final int i) {
        try {
            EventQueue.invokeAndWait(() -> {
                cells.remove(i);
                cellList.remove(i);
            });
        } catch (Exception ignored){
        }
    }

    private int[] getAllIndicies() {
        int count = getCount();
        int[] indicies = new int[count];
        for (int i =0; i < count; i++) {
            indicies[i] = i;
        }
        return indicies;
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
        return listModel !=null? listModel.getSize():0;
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
            return (String) listModel.getElementAt(index);
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
            List<String> cellGroups = cd.getGroups();
            if (cellGroups != null) {
                groups.addAll(cellGroups);
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
            if (cell.getGroups() != null) {
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
        if (WindowManager.getCurrentImage() == null) {
            return;
        } else {
            ImagePlus image = WindowManager.getCurrentImage();
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
