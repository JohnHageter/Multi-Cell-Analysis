package Cell.Utils;

import ij.ImagePlus;

import java.awt.*;
import java.util.Random;

public class Utils {
    public static boolean isStack(ImagePlus imp) {
        return imp.hasImageStack();
    }

    public static Color randomColor() {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return new Color(red, green, blue);
    }
}
