# Multi-Cell-Analysis ImageJ/Fiji Plugin
<p>MCA is a functional imaging analysis toolkit for ImageJ. The plugin is intended for use with confocal or multiphoton calcium imaging datasets.</p>

MCA has the following functions available for use:  
- Motion correction [^1]
- Relative fluorescence stack conversion
- Polygonal/point selection and grouping of ROIs
- Gaussian signal filtering
- Peak detection
- Cell Segmentation[^2]  
  
The main module of MCA is the cell manager which is heavily inspired by ImageJ's built in ROI Manager. The cell manager is intended to add functionality specific for functional imaging datasets to ImageJ as well as improved visualization of cellular ROIs and grouping ROIs. 


## Installation  
The MCA Plugin can currently be installed by downlownding the latest Jar from the releases tab. MCA uses the [`IJ-OpenCV-Plugins`](https://github.com/joheras/IJ-OpenCV) for working between ImageJ and OpenCV. We also use the ImageJ plugin for [`StarDist`](https://sites.imagej.net/StarDist/). Both of these required plugins can be installed by updating ImageJ and selecting their respective update sites along with [`CSBDeep`](https://sites.imagej.net/CSBDeep/) which is required for StarDist. This can be completed by following these steps:  
1. Start Fiji
2. Select `Help>Update...` from the menu bar.
3. Wait for Fiji to update and then click on `Manage update sites`.
4. Search for the `CSBDeep`, `StarDist`, and `IJ-OpenCV-Plugins` and enable the checkbox next to the sites.
5. Click the `Close` button and then `Apply changes` to install the required plugins and dependencies.
6. Download the latest `MCA.jar` from the releases tab
7. Drag and drop the `MCA.jar` file into your Fiji plugins folder which can be found at `../Fiji.app/Plugins/`.
8. The Cell manager can be accessed from `Analyze>> Tools>> Cell Manager`.  
  
In the future, MCA will be available for download on the ImageJ update site.
  
[^1]: Based on ImageJ implementation of  OpenCV Template Matching algorithm. [Template Matching](https://sites.google.com/site/qingzongtseng/template-matching-ij-plugin)
[^2]: Uses the [StartDist](https://github.com/stardist/stardist-imagej)-ImageJ plugin for cell segmentation  