[![DOI](https://zenodo.org/badge/846814008.svg)](https://doi.org/10.5281/zenodo.17610801)

# Multi-Cell-Analysis ImageJ/Fiji Plugin <img src="./Logo.png" width="200" title="MCA" alt="Multi-Cell-Analysis" align="right" vspace = "50">
<p>MCA is a functional imaging analysis toolkit for ImageJ. The plugin is intended for use with confocal or multiphoton calcium imaging datasets.</p>

MCA has the following functions available for use:  
- Motion correction [^2]
- Relative fluorescence stack conversion
- Polygonal/point selection and grouping of ROIs
- Gaussian signal filtering
- Peak detection
- Cell Segmentation[^3] [^4]  
  
The main module of MCA is the cell manager which is heavily inspired by ImageJ's built in ROI Manager. The cell manager is intended to add functionality specific for functional imaging datasets to ImageJ as well as improved visualization of cellular ROIs and grouping ROIs. 

For more information, please refer to our publication [^1]

## Installation  
The MCA Plugin can currently be installed through the ImageJ update site with the following steps:  
1. Run ImageJ Update. `Help >> Update...`
2. In the ImageJ updater select "Add Unlisted Site" and add `https://sites.imagej.net/MultiCellPlugins`  
   *Note: for access to StarDist2d you should enable the `Stardist`, `CSBDeep`, and `Tensorflow` update sites*
3. Continue with update and restart ImageJ

MCA requires a few dependencies which may conflict with other update sites. After you enable the update site during the ImageJ update, please ensure these dependencies are downloaded along with it.

- OpenCV-4.9.0-1.5.10
- OpenCV-platform-0.3.26-1.5.10
- Openblas-0.3.26-1.5.10
- Openblas-platform-0.3.26-1.5.10
- JavaCPP-1.5.10 (Already installed with Fiji)
- JavaCPP-platform-1.5.10
- Commons-math3-3.6.1 (Already installed with Fiji)
- Platform specific libraries (macos, linux64, x86_64)

To benefit from the full capabilities of MCA, please use the Fiji distribution of ImageJ with additional QOL plugins.

[^1]: Based on ImageJ implementation of OpenCV Template Matching. [Template Matching](https://sites.google.com/site/qingzongtseng/)
[^2]: Uses the [StartDist](https://github.com/stardist/stardist-imagej)-ImageJ plugin for cell segmentation
[^3]: Uses the [Cellpose](https://github.com/mouseland/cellpose) package
[^4]: [MCA: a Multicellular analysis Calcium Imaging Toolbox for ImageJ](https://pubmed.ncbi.nlm.nih.gov/40894801/)

## Usage
All functions are available through the cell manager which can be accessed from `Analyze>> Tools>> Cell Manager`.
  
##### Registration (Motion Correction)
The registration function corrects for moving images by using the OpenCV Template matching cross correlation algorithm. The input should be a single channel 8-bit grayscale stack. Click on the function button and draw an ROI around the area you want all subsequent images to be registered to. 

##### Polygonal Grouping
This function enables the polygonal selection tool to draw an ROI encompassing multiple cells. The cells found within the roi will be labelled upon data generation. The group will then be found within the groups tab and can be adjusted until data export. Input the group name click okay and the draw a polygon around the cells you which to group together. If your group spans multiple regions, hold shift when beginning selection of the second region.  
  
##### Point Grouping
This function enables the point selection tool to group cells which may not be able to be grouped by a simple selection. This group will be present and adjustable until data generation
  
##### Generate data
This function outputs data in imageJ to be saved as .csv files. You have the option to select peak detection methods or filter data before generating final data.

##### Convert Stack to DF/F
This function converts an image stack to df/f values pixel by pixel. You can set a baseline (f0) for calculation and chose to add a background subtraction to the stack beforehand.

##### Add cell
This button will add the current selection to the Cell Manager

##### Delete
Deletes the currently selected cell from the Cell Manager

##### Load from ROI Manager
This function syncs ROIs between the ROI manager and Cell Manager

##### Cellpose ...
This function runs the Cellpose ROI detection method. Upon first usage, you will be promped to install a distribution of micomamba which will install Cellpose on your system. If you wish to use your own version of Cellpose, you will either not be prompted to install micromamba if MCA can find Cellpose on your system. If MCA cannot find Cellpose on your system, click "No" when prompted to install cellpose and then point the "Cellpose env" setting to the Cellpose environment that you have installed.

MCA is bundled with 3 pre-trained cell detection models for Cellpose. They are listed in the "Model" section of the cellpose prompt. If you wish to use your own, select "custom" from the drop down menu and enter the path to your custom model.

##### Cells / Groups
These buttons switch between the cells and groups that you have within the Cell Manager.
