package Cell.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

public class CellDataSaver {

    public static void saveCellData(String filePath, ArrayList<CellData> cellDataList) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(filePath)))) {
            ZipEntry zipEntry = new ZipEntry("cellData.dat");
            zos.putNextEntry(zipEntry);

            try (ObjectOutputStream oos = new ObjectOutputStream(zos)) {
                oos.writeObject(cellDataList);
            }

            zos.closeEntry();
            System.out.println("Cell data saved successfully to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<CellData> loadCellData(String filePath) {
        ArrayList<CellData> cellDataList = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(filePath)))) {
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null && zipEntry.getName().equals("cellData.dat")) {
                try (ObjectInputStream ois = new ObjectInputStream(zis)) {
                    cellDataList = (ArrayList<CellData>) ois.readObject();
                }
            }
            zis.closeEntry();
            System.out.println("Cell data loaded successfully from " + filePath);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return cellDataList;
    }
}
