package Cell.Utils;

import ij.gui.Roi;

import java.util.ArrayList;

public class GroupData {
    public Roi roi;
    public ArrayList<CellData> cells = new ArrayList<>();
    public String name;

    public GroupData(ArrayList<CellData> cells){
        this.cells = cells;
    }

    public GroupData(String name){
        this.name = name;
    }

    public Roi getRoi(){
        return this.roi;
    }

    public void addCell(CellData cell){
        cells.add(cell);
    }

    public void setName(String name){
        this.name = name;
        if (this.roi != null) {
            this.roi.setName(name);
        }
    }

    public void setRoi(Roi r){
        this.roi = r;
    }

    public ArrayList<CellData> getCellsInGroup(){
        return cells;
    }
}
