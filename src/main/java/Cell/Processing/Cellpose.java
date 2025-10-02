package Cell.Processing;

import Cell.Frame.CellManager;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.Prefs;
import ij.gui.Roi;
import ij.io.RoiDecoder;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Cellpose {
    String cellposeEnv;
    String cellposeModelDir;
    String model;
    String customModelPath;
    String args;
    int timeout;
    File pythonExe;
    HashMap<String, File> modelPaths = new HashMap<>();

    public void run() {
        if (!cellposeEnvExists()) {
            boolean install = IJ.showMessageWithCancel(
                    "Cellpose not found",
                    "The Cellpose environment is not installed.\n\n" +
                            "Would you like to install it now?"
            );
            if (!install) {
                IJ.showStatus("Cellpose installation cancelled.");
                return;
            }

            try {
                installCellpose();
            } catch (Exception e) {
                IJ.error("Failed to install Cellpose:\n" + e.getMessage());
                return;
            }
        }

        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        } else if (imp.getNSlices() > 1) {
            IJ.error("Image stacks not supported in MCA.");
            return;
        }

        String cellpose = getCellposePath();
        String osName = System.getProperty("os.name").toLowerCase();
        IJ.log("OS: " + osName);
        if (osName.contains("win")) {
            this.pythonExe = new File(cellpose, "python.exe");
        } else {
            this.pythonExe = new File(cellpose, "bin/python");
        }

        try{
            File input = File.createTempFile("tmp_cellpose_in", ".tif");
            IJ.saveAsTiff(imp, input.getAbsolutePath());

            if (getDialog()) {
                checkModel();
                callCellpose(input);

                //output file is always based on input name. appended with "_rois.zip"
                importMaskAsCellData(input);
            } else {
                return;
            };
        } catch (Exception e) {
            IJ.error("Cellpose failed: " + e.getMessage());
        }
    }

    private void checkModel() throws IOException, InterruptedException {
        String home = IJ.getDirectory("home");
        this.cellposeModelDir = home + ".cellpose/models";

        File modelDir = this.model.equals("Custom") ? new File(customModelPath)
                : new File(this.cellposeModelDir, this.model);

        IJ.log(modelDir.getAbsolutePath());

        if (!modelDir.exists()) {
            IJ.log("Model not found in ~/.cellpose. Adding it...");
            File modelSource = this.model.equals("Custom") ? new File(customModelPath)
                    : new File(IJ.getDirectory("imagej") + "models/CellManager-CellposeModels/" + this.model);

            addCellposeModel(modelSource);
        } else {
            IJ.log("Model found: " + modelDir.getAbsolutePath());
        }
    }

    private boolean getDialog() {
        this.cellposeEnv = Prefs.get("cellpose.env", this.cellposeEnv);
        this.model = Prefs.get("cellpose.model", "H2B-GCaMP");
        this.customModelPath = Prefs.get("cellpose.customModelPath", "");
        this.args = Prefs.get("cellpose.args", "");
        this.timeout = (int) Prefs.get("cellpose.timeout", 0);

        String ijRoot = IJ.getDirectory("imagej");
        this.modelPaths.put("H2BGCaMP", new File(ijRoot, "models/CellManager-CellposeModels/H2BGCAMP"));
        this.modelPaths.put("cyto3",    new File(ijRoot, "models/CellManager-CellposeModels/cyto3"));
        this.modelPaths.put("Custom",   this.customModelPath != null ? new File(this.customModelPath) : null);


        GenericDialog gd = new GenericDialog("Cellpose");
        gd.addFileField("Cellpose envrionment", this.cellposeEnv);
        String[] models = {"H2BGCaMP", "cyto3", "cpsam", "Custom"};
        gd.addChoice("Model", models, this.model);
        gd.addFileField("Custom model path (if selected)", this.customModelPath, 50);
        gd.addStringField("Additional arguments", this.args, 50);
        gd.addNumericField("Timeout (minutes, 0=infinite)", this.timeout,0);
        gd.showDialog();
        if (gd.wasCanceled()) return false;

        this.cellposeEnv = gd.getNextString();
        this.model = gd.getNextChoice();
        this.customModelPath = gd.getNextString().trim();
        this.args = gd.getNextString().trim();
        this.timeout = (int) gd.getNextNumber();

        Prefs.set("cellpose.env", this.cellposeEnv);
        Prefs.set("cellpose.model", this.model);
        Prefs.set("cellpose.customModelPath", this.customModelPath);
        Prefs.set("cellpose.args", this.args);
        Prefs.set("cellpose.timeout", this.timeout);

        return gd.wasOKed();
    }

    private void importMaskAsCellData(File input) throws IOException {
        File zipOutput = new File(input.getCanonicalPath().replaceFirst("\\.tif$", "") + "_rois.zip");

        if (!zipOutput.exists()) {
            IJ.error("No ROI output found.");
            return;
        }

        IJ.log(zipOutput.getCanonicalPath());
        CellManager cm = CellManager.getInstance();
        if (cm == null) cm = new CellManager();

        List<Roi> rois = new ArrayList<>();
        File tmpDir;

        try {
            tmpDir = Files.createTempDirectory("cellpose_rois_").toFile();
        } catch (IOException e) {
            IJ.error("Failed to create temp dir for ROIs: " + e.getMessage());
            return;
        }

        try (ZipFile zf = new ZipFile(zipOutput)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.toLowerCase().endsWith(".roi")) continue;

                // Extract entry to temp .roi file
                File out = new File(tmpDir, new File(name).getName());
                try (InputStream is = zf.getInputStream(entry);
                     OutputStream os = Files.newOutputStream(out.toPath())) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                }


                try {
                    RoiDecoder rd = new RoiDecoder(out.getAbsolutePath());
                    Roi roi = rd.getRoi();
                    if (roi != null) {
                        rois.add(roi);
                    } else {
                        IJ.log("RoiDecoder returned null for: " + out.getAbsolutePath());
                    }
                } catch (Exception e) {
                    IJ.log("Failed to decode ROI " + out.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            IJ.error("Failed to read ROI zip: " + e.getMessage());
            return;
        }

        CellManager finalCm = cm;
        SwingUtilities.invokeLater(() -> {
            for (Roi r : rois) {
                try {
                    finalCm.addCell(r);
                    IJ.log("Added " + (r.getName() != null ? r.getName() : "unnamed ROI") + " to the Cell Manager.");
                } catch (Exception ex) {
                    IJ.log("addCell failed for ROI: " + ex.getMessage());
                }
            }
        });
    }

    private void callCellpose(File input) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(this.pythonExe.getAbsolutePath());
        command.add("-m");
        command.add("cellpose"); // entrypoint script
        command.add("--image_path");
        command.add(input.getCanonicalPath());
        command.add("--pretrained_model");
        if (this.model.equals("Custom") && !this.customModelPath.isEmpty()) {
            command.add(this.customModelPath);
        } else {
            command.add(this.model);
        }

        command.add("--save_rois");
        command.add("--verbose");

        if (!this.args.isEmpty()) {
            command.addAll(Arrays.asList(this.args.split(" ")));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                IJ.log("[cellpose] " + line);
//                    if (Thread.currentThread().isInterrupted()) {
//                        process.destroyForcibly(); // kill python if fiji is closed
//                        break;
//                    }
            }
        } catch (IOException e) {
            IJ.log("Error reading Cellpose output: " + e.getMessage());
        }

        int exit = (timeout > 0)
                ? process.waitFor(timeout * 60L, java.util.concurrent.TimeUnit.SECONDS) ? process.exitValue() : -1
                : process.waitFor();

        if (exit != 0) {
            throw new IOException("Cellpose exited with code " + exit);
        }
    }

    private static String getCellposePath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return new File(System.getenv("APPDATA"), "mamba/envs/cellpose").getAbsolutePath();
        } else {
            return new File(System.getProperty("user.home"), ".local/share/mamba/envs/cellpose").getAbsolutePath();
        }
    }

    private void installCellpose() throws IOException, InterruptedException {
        String ijDir = IJ.getDirectory("imagej");
        IJ.log(ijDir);
        String osName = System.getProperty("os.name").toLowerCase();
        String micromambaBinary;

        if (osName.contains("win")) {
            micromambaBinary = new File(ijDir, "plugins/MCA/micromamba-win-64.exe").getAbsolutePath();
        } else if (osName.contains("mac")) {
            micromambaBinary = new File(ijDir, "plugins/MCA/micromamba-osx-64").getAbsolutePath();
        } else if (osName.contains("nux")) {
            micromambaBinary = new File(ijDir, "plugins/MCA/micromamba-linux-64").getAbsolutePath();
        } else {
            throw new RuntimeException("Unsupported OS: " + osName);
        }

        File home = new File(System.getProperty("user.home"));
        File envDir = new File(home, ".micromamba/envs/cellpose");

        if (envDir.exists()) {
            IJ.log("Cellpose environment already exists at: " + envDir.getAbsolutePath());
            return;
        }

        IJ.log("Creating Cellpose environment using micromamba...");

        List<String> cmd = new ArrayList<>();
        cmd.add(micromambaBinary);
        cmd.add("create");
        cmd.add("-y");
        cmd.add("-n");
        cmd.add("cellpose");
        cmd.add("-c");
        cmd.add("conda-forge");
        cmd.add("cellpose=3.1.1.2");
        cmd.add("python=3.10");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        new Thread(() -> {
            try (BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    IJ.log("[micromamba] " + line);
                }
            } catch (IOException e) {
                IJ.log("Error reading micromamba output: " + e.getMessage());
            }
        }).start();

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Micromamba failed with exit code " + exitCode);
        }

        File cellposeDir = new File(System.getProperty("user.home"), ".cellpose/models");
        if (!cellposeDir.exists()) {
            cellposeDir.mkdirs();
        }

        IJ.log("Cellpose environment successfully installed at: " + envDir.getAbsolutePath());
    }

    private static boolean cellposeEnvExists() {
        return new File(getCellposePath()).exists();
    }

    public void addCellposeModel(File modelDir) throws IOException, InterruptedException {
        File cellposeDir = new File(System.getProperty("user.home"), ".cellpose/models");
        if (!cellposeDir.exists()) {
            cellposeDir.mkdirs();
        }

        List<String> command = new ArrayList<>();
        command.add(pythonExe.getAbsolutePath());
        command.add("-m");
        command.add("cellpose");
        command.add("--add_model");
        command.add(modelDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                IJ.log("[cellpose] " + line);
            }
        }

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to add model to Cellpose (exit code " + exitCode + ")");
        }
    }

}
