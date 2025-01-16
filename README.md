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
The MCA Plugin can currently be installed by downlownding the latest Jar from the releases tab and placing the .jar file in the plugins folder within ImageJ.  
  
You can also install MCA through the ImageJ update site with the following steps:  
1. Run ImageJ Update. `Help >> Update...`
2. In the ImageJ updater select "Add Unlisted Site"
   
[^1]: Based on ImageJ implementation of  OpenCV Template Matching algorithm. [Template Matching](https://sites.google.com/site/qingzongtseng/template-matching-ij-plugin)
[^2]: Uses the [StartDist](https://github.com/stardist/stardist-imagej)-ImageJ plugin for cell segmentation  

## Usage
All functions are available through the cell manager which can be accessed from `Analyze>> Tools>> Cell Manager`. The two tabs in the cell manager are made to represent individual cells that are labeled. Simply, these are identical to ImageJ ROIs. The groups tab contains polygonal selections which group specicfic cells together based on whether they are within the bounds of the polygonal selection or not. Many of the functions listed on the left side require an image to be open as input.
  
### Registration (Motion Correction)
The registration function corrects for moving images by using the OpenCV Template matching cross correlation algorithm. The input should be a single channel 8-bit grayscale stack.  
(Example)  

### Apply group(s)
This function enables the polygonal selection tool to draw an ROI encompassing multiple cells. The cells found within the roi will be labelled upon data generation. The group will then be found within the groups tab and can be adjusted until data generation.  
  
### Select multiple
This function enables the point selection tool to group cells which may not be able to be grouped by a simple selection. This group will also be present in the groups tab, but cannot be adjusted before data output.  
  
