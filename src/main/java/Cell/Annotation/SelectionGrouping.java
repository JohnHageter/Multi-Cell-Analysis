package Cell.Annotation;

import Cell.Utils.CellData;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Roi;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SelectionGrouping implements DialogListener {
    private Map<String, Roi> roiMap = new HashMap<>();

    public void applyGroup(ArrayList<CellData> cells, Roi groupingRoi, String groupName) {
        for (CellData cell : cells) {
            int centerX = cell.getCenterX();
            int centerY = cell.getCenterY();
            if(groupingRoi.contains(centerX, centerY)) {
                cell.addGroup(groupName);
                cell.setGroupRoi(groupingRoi);
                roiMap.put(groupName, groupingRoi);
            }
        }
    }

    public Roi getRoiForGroup(String group) {
        return roiMap.get(group);
    }



//    public boolean inputParameters() throws BackingStoreException {
//        loadParameters();
//        return createDialog();
//    }
//
//    private boolean createDialog() throws BackingStoreException {
//        boolean okPressed = false;
//        boolean canceled = false;
//
//        while (!okPressed && !canceled) {
//            this.gd = new GenericDialog("Group ROIs by selection");
//            this.gd.addButton("Add group", e -> {
//                this.nGroups++;
//                try {
//                    updateDialog();
//                } catch (BackingStoreException ex) {
//                    throw new RuntimeException(ex);
//                }
//            });
//            this.gd.addButton("Remove group", e -> {
//                if (this.nGroups > 1) {
//                    this.nGroups--;
//                    try {
//                        updateDialog();
//                    } catch (BackingStoreException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                }
//            });
//
//            for (int i = 0; i < this.nGroups; i++) {
//                String groupName = (i < this.groupNames.size()) ? this.groupNames.get(i) : DEFAULT_NAME;
//                this.gd.addStringField("Group #" + (i + 1) + ": ", groupName);
//            }
//
//            this.gd.addDialogListener(this);
//            this.gd.showDialog();
//
//            if (this.gd.wasOKed()) {
//                updateGroupNamesFromDialog();
//                saveParameters();
//                okPressed = true;
//            }
//            if(this.gd.wasCanceled()){
//                canceled = true;
//                return canceled;
//            }
//
//        }
//
//        return okPressed;
//    }
//
//    private void updateDialog() throws BackingStoreException {
//        updateGroupNamesFromDialog();
//        saveParameters();
//        this.gd.dispose();
//    }
//
//    private void loadParameters() {
//        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
//        this.nGroups = prefs.getInt(PREF_NUM_GROUPS, this.nGroups);
//        this.groupNames.clear();
//        for (int i = 0; i < this.nGroups; i++) {
//            this.groupNames.add(prefs.get(PREF_GROUP_NAME + i, DEFAULT_NAME));
//        }
//    }
//
//    private void saveParameters() throws BackingStoreException {
//        Preferences prefs = Preferences.userNodeForPackage(GroupROIs.class);
//        prefs.clear();
//        prefs.putInt(PREF_NUM_GROUPS, this.nGroups);
//
//        for (int i = 0; i < this.groupNames.size(); i++) {
//            if (!Objects.equals(this.groupNames.get(i), "")) {
//                prefs.put(PREF_GROUP_NAME + i, this.groupNames.get(i));
//            }
//        }
//    }
//
//    private void updateGroupNamesFromDialog() {
//        this.groupNames.clear();
//        Vector<?> stringFieldsVector = this.gd.getStringFields();
//
//        for (Object field : stringFieldsVector) {
//            if (field instanceof TextField) {
//                TextField textField = (TextField) field;
//                String text = textField.getText().trim();
//                this.groupNames.add(text.isEmpty() ? DEFAULT_NAME : text);
//            }
//        }
//
//        while (this.groupNames.size() < this.nGroups) {
//            this.groupNames.add(DEFAULT_NAME);
//        }
//    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        return true;
    }


}
