package Cell.Processing;

import Cell.Frame.CellManager;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.RoiDecoder;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Cellpose {
    private String cellposeEnv;
    private String cellposeModelDir;
    private String model;
    private String customModelPath;
    private String args;
    private int timeout;
    private Map<String, File> modelPaths = new HashMap<>();
    private String micromamba;

    public void run() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) { IJ.noImage(); return; }
        if (imp.getNSlices() > 1) { IJ.error("Image stacks not supported."); return; }

        try {
            boolean envExists = cellposeEnvExists();
            if (!envExists) {
                boolean install = IJ.showMessageWithCancel(
                        "Cellpose not found",
                        "Cellpose environment is not installed.\nWould you like to install it now?"
                );

                if (install) {
                    installCellpose();
                } else {
                    IJ.showStatus("Skipping installation. Please enter the path to your Cellpose Environment.");
                }
            }

            // Try to locate Python executable
            String cellposePath = cellposeEnv != null && !cellposeEnv.isEmpty()
                    ? cellposeEnv
                    : getCellposePath();

            File input = File.createTempFile(imp.getTitle(), ".tif");
            IJ.saveAsTiff(imp, input.getAbsolutePath());

            if (getDialog()) {
                if(!new File(this.cellposeEnv).exists()) {
                    IJ.error("Cellpose environment doesn't exist.\nCheck that the path is correct.");
                    return;
                }
                checkModel();
                runCellpose(input);
                importMaskAsCellData(input);
            }

        } catch (Exception e) {
            IJ.error("Cellpose failed: " + e.getMessage());
        }
    }

    private boolean cellposeEnvExists() {
        String home = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();
        File env;
        if (osName.contains("win")) {
            env = new File(home, "AppData/Roaming/mamba/envs/cellpose");
        } else {
            env = new File(home, ".mamba/envs/cellpose");
        }

        this.cellposeEnv = String.valueOf(env);

        return env.exists();
    }

    private void checkModel() throws IOException, InterruptedException {
        this.cellposeModelDir = IJ.getDirectory("home") + ".cellpose/models";
        File modelDir = "Custom".equals(this.model) ? new File(customModelPath)
                : new File(this.cellposeModelDir, this.model);

        if (!modelDir.exists()) {
            IJ.log("Model not found in ~/.cellpose. Adding it...");
            File source = "Custom".equals(this.model) ? new File(customModelPath)
                    : new File(IJ.getDirectory("imagej") + "models/CellManager-CellposeModels/" + this.model);
            addModel(source);
        } else {
            IJ.log("Model found: " + modelDir.getAbsolutePath());
        }
    }

    private boolean getDialog() {
        if(this.cellposeEnv.isEmpty()) {
            this.cellposeEnv = Prefs.get("cellpose.env", this.cellposeEnv);
        }

        this.model = Prefs.get("cellpose.model", "H2B-GCaMP");
        this.customModelPath = Prefs.get("cellpose.customModelPath", "");
        this.args = Prefs.get("cellpose.args", "");
        this.timeout = (int) Prefs.get("cellpose.timeout", 0);

        String ijRoot = IJ.getDirectory("imagej");
        this.modelPaths.put("H2BGCaMP", new File(ijRoot, "models/CellManager-CellposeModels/H2BGCAMP"));
        this.modelPaths.put("cyto3", new File(ijRoot, "models/CellManager-CellposeModels/cyto3"));
        this.modelPaths.put("Custom", customModelPath != null ? new File(customModelPath) : null);

        GenericDialog gd = new GenericDialog("Cellpose");
        gd.addFileField("Cellpose environment", this.cellposeEnv);
        gd.addChoice("Model", new String[]{"H2BGCaMP", "cyto3", "cpsam", "Custom"}, this.model);
        gd.addFileField("Custom model path (if selected)", this.customModelPath, 50);
        gd.addStringField("Additional arguments", this.args, 50);
        gd.addNumericField("Timeout (minutes, 0=infinite)", this.timeout, 0);
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

        CellManager cm = CellManager.getInstance();
        if (cm == null) cm = new CellManager();

        File tmpDir = Files.createTempDirectory("cellpose_rois_").toFile();
        List<Roi> rois = new ArrayList<>();

        try (ZipFile zf = new ZipFile(zipOutput)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().toLowerCase().endsWith(".roi")) continue;

                File out = new File(tmpDir, new File(entry.getName()).getName());
                try (InputStream is = zf.getInputStream(entry);
                     OutputStream os = Files.newOutputStream(out.toPath())) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                }

                try {
                    RoiDecoder rd = new RoiDecoder(out.getAbsolutePath());
                    Roi roi = rd.getRoi();
                    if (roi != null) rois.add(roi);
                } catch (Exception e) {
                    IJ.log("Failed to decode ROI " + out.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        CellManager finalCm = cm;
        SwingUtilities.invokeLater(() -> {
            for (Roi r : rois) {
                try {
                    finalCm.addCell(r);
                } catch (Exception ex) {
                    IJ.log("addCell failed for ROI: " + ex.getMessage());
                }
            }
        });
    }

    private void runCellpose(File input) throws Exception {
        String python = getPython(cellposeEnv);
        if (python == null) {
            IJ.error("Python executable not found in Cellpose environment: " + cellposeEnv);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(python);
        command.add("-m");
        command.add("cellpose");
        command.add("--image_path");
        command.add(input.getCanonicalPath());
        command.add("--pretrained_model");
        command.add("Custom".equals(this.model) && !this.customModelPath.isEmpty() ? this.customModelPath : this.model);
        command.add("--save_rois");
        command.add("--verbose");

        if (!this.args.isEmpty()) {
            Collections.addAll(command, this.args.split(" "));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) IJ.log("[cellpose] " + line);
        }

        int exit = timeout > 0 ?
                (process.waitFor(timeout * 60L, java.util.concurrent.TimeUnit.SECONDS) ? process.exitValue() : -1) :
                process.waitFor();

        if (exit != 0) throw new IOException("Cellpose exited with code " + exit);
    }

    private String getCellposePath() {
        return System.getProperty("os.name").toLowerCase().contains("win") ?
                new File(System.getenv("APPDATA"), "mamba/envs/cellpose").getAbsolutePath() :
                new File(System.getProperty("user.home"), ".local/share/mamba/envs/cellpose").getAbsolutePath();
    }

    private String getPython(String envDirPath) {
        if (envDirPath == null || envDirPath.isEmpty()) {
            return null;
        }

        File envDir = new File(envDirPath);
        if (!envDir.exists()) {
            return null;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        File pythonExe;

        if (osName.contains("win")) {
            pythonExe = new File(envDir, "python.exe");
        } else {
            pythonExe = new File(envDir, "bin/python");
        }

        if (pythonExe.exists() && pythonExe.canExecute()) {
            return pythonExe.getAbsolutePath();
        } else {
            IJ.log("Python executable not found in environment: " + envDirPath);
            return null;
        }
    }

    public void addModel(File modelDir) throws IOException, InterruptedException {
        File cellposeDir = new File(System.getProperty("user.home"), ".cellpose/models");
        if (!cellposeDir.exists()) cellposeDir.mkdirs();

        String python = getPython(cellposeEnv);
        if (python == null) {
            IJ.error("Python executable not found in Cellpose environment: " + cellposeEnv);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(python);
        command.add("-m");
        command.add("cellpose");
        command.add("--add_model");
        command.add(modelDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) IJ.log("[cellpose] " + line);
        }

        int exitCode = proc.waitFor();
        if (exitCode != 0) throw new RuntimeException("Failed to add model (exit code " + exitCode + ")");
    }

    private void installMicromamba() throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        File pluginDir = new File(IJ.getDirectory("imagej"), "plugins/MCA");
        if (!pluginDir.exists()) pluginDir.mkdirs();

        IJ.log("Installing Micromamba...");

        String binaryName;
        String downloadURL;

        if (os.contains("win")) {
            binaryName = "micromamba.exe";
            downloadURL = "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-win-64.exe";
        } else if (os.contains("mac")) {
            binaryName = "micromamba";
            downloadURL = "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-osx-64";
        } else if (os.contains("nux")) {
            binaryName = "micromamba";
            downloadURL = "https://github.com/mamba-org/micromamba-releases/releases/latest/download/micromamba-linux-64";
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }

        File micromambaFile = new File(pluginDir, binaryName);

        if (micromambaFile.exists()) {
            IJ.log("Micromamba already present at: " + micromambaFile.getAbsolutePath());
            return;
        }

        IJ.log("Downloading Micromamba from " + downloadURL);

        // Use Java download instead of shell commands
        try (InputStream in = new java.net.URL(downloadURL).openStream()) {
            Files.copy(in, micromambaFile.toPath());
        }

        if (!os.contains("win")) {
            micromambaFile.setExecutable(true);
        }

        IJ.log("Micromamba downloaded to: " + micromambaFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(micromambaFile.getAbsolutePath(), "--version");
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                IJ.log("[micromamba] " + line);
            }
        }

        int exit = proc.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Micromamba test run failed (exit code " + exit + ")");
        }

        IJ.log("Micromamba installation completed successfully.");
    }

    private void installCellpose() throws IOException, InterruptedException {
        File envDir = new File("");
        if (IJ.isLinux() || IJ.isMacintosh()) {
            envDir = new File(System.getProperty("user.home"), ".local/share/mamba/envs/cellpose");
        } else if (IJ.isWindows()) {
            envDir = new File(System.getProperty("user.home"), "AppData/Roaming/mamba/envs/cellpose");
        }


        if (cellposeEnvExists()) return;

        IJ.log("Checking for micromamba...");
        String micromamba = findMicromamba();
        if (micromamba == null) {
            installMicromamba();
            micromamba = findMicromamba();
            if (micromamba == null) throw new RuntimeException("Micromamba not found after installation");
        }

        IJ.log("Creating Cellpose environment...");
        List<String> cmd = new ArrayList<>();
        cmd.add(micromamba);
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

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) IJ.log("[micromamba] " + line);

        if (proc.waitFor() != 0) throw new RuntimeException("Micromamba environment creation failed");

        new File(System.getProperty("user.home"), ".cellpose/models").mkdirs();
        IJ.log("Cellpose environment installed at: " + envDir.getAbsolutePath());
    }

    private String findMicromamba() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String cmd = os.contains("win") ? "where" : "which";
            ProcessBuilder pb = new ProcessBuilder(cmd, "micromamba");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String path = reader.readLine();
            if (proc.waitFor() == 0 && path != null && !path.isEmpty()) return path.trim();

            // Fallback to plugins folder
            File pluginDir = new File(IJ.getDirectory("imagej"), "plugins/MCA");
            File[] files = pluginDir.listFiles();
            if (files != null) for (File f : files)
                if (f.getName().startsWith("micromamba") && f.canExecute()) return f.getAbsolutePath();
        } catch (Exception ignored) {}
        return null;
    }
}
