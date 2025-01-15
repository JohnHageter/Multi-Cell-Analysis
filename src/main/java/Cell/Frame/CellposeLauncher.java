package Cell.Frame;
import ij.IJ;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.io.File;
import java.util.prefs.Preferences;

public class CellposeLauncher {
    public void runCellpose() {
        SwingUtilities.invokeLater(() -> {
            try {
                Preferences prefs = Preferences.userNodeForPackage(this.getClass());
                String defaultEnvPath = prefs.get("condaEnvPath", "");
                String envPath = defaultEnvPath.isEmpty() ? selectCondaEnvironment() : promptForCondaEnvironment(defaultEnvPath);

                if (envPath == null) {
                    IJ.log("No conda environment selected. Aborting.");
                    return;
                }
                prefs.put("condaEnvPath", envPath);

                String pluginsDir = IJ.getDirectory("plugins");
                if (pluginsDir == null) {
                    IJ.log("Could not determine plugin installation directory.");
                    return;
                }

                String modelPath = pluginsDir + "CellManager-CellposeModels\\H2BGCaMP";

                new Thread(() -> {
                    try {
                        String command = String.format(
                                "env_path=%s env_type=conda model= model_path=%s diameter=10 ch1=0 ch2=-1 additional_flags=--use_gpu",
                                envPath, modelPath
                        );

                        IJ.run("Cellpose ...", command);
                        IJ.log("Cellpose executed successfully.");
                    } catch (Exception error) {
                        IJ.log("BIOP, ImageScience, and Trackmate-Cellpose update sites must be enabled to run Cellpose.");
                        IJ.log(error.getMessage());
                    }
                }).start();

            } catch (Exception e) {
                IJ.log("An error occurred: " + e.getMessage());
            }
        });
    }

    private static String selectCondaEnvironment() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Conda Environment Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            return selectedDir.getAbsolutePath();
        }

        return null;
    }

    private String promptForCondaEnvironment(String defaultEnvPath) {
        String message = "Current Conda environment path:\n" + defaultEnvPath + "\n\nDo you want to use this path?";
        int option = JOptionPane.showConfirmDialog(null, message, "Conda Environment Path", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            return defaultEnvPath;
        } else {
            return selectCondaEnvironment();
        }
    }
}
