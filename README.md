# multiMets

This is the code used to produce the results in [*The sum and the parts: dynamics of multiple and individual metastases during adaptive therapy*](https://www.biorxiv.org/content/10.1101/2022.08.04.502852v1) and the [interactive website]([https://jillagal.shinyapps.io/multiMets/]) containing the main results and movies produced from this code. In this work, we present a framework for estimating individual and collective components of a metastatic system through tumor response dynamics, which uses a system of off-lattice agent-based models to represent metastatic lesions within independent domains, all of which are subject to the same systemic therapy. The results are output to a folder with graphics files for the spatial output and data files for the collective tumor burden over time as well as individual folders for data and graphics of each individual metastasis. 

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
You will need Java to run this program. It is known to work with:

```
Java(TM) SE Runtime Environment (build 17.0.3.1)
Command line or other IDE software (we use IntelliJ IDEA)
```

### Install & Run

First download a ZIP file of the repository contents to your local machine. Double-click to unZIP. 

Go to the directory folder from the command line, and run the shell script*:

```
cd local-directory/multiMets-main/
./exe.sh
```

*You may need to change the file permissions before running the script in order to make it executable:

```
chmod +x exe.sh
```

### Changing The Input Variables
Several variables can be changed to reproduce the results from the paper. These can be modified in the exe.sh file or read from the provided arrayVars0.txt file.
The input variables in order include:
* total tumor burden
* number of metastases
* proliferation rate (from 0-100% of the range given in Pars class)
* the total change in proliferation rate over all metastases (0-100%)
* boolean sequential seeding (0=simultaneous, 1=sequential)
* the random death rate (divided by 10000 to given deaths per frame)
* initial intrametastasis heterogeneity (given as a % of the range)
* the change in heterogneity from previous to next metastasis (as % of range)

## Basic Code Structure
This section describes the general code structure to get you oriented, and each class file is expanded upon below. There are 2 phases for a complete run of the code. The first is the growth and seeding phase, where the metastases are seeding according to the specifications and the simulation stops and outputs data to be able to recreate the final configuration for all cells within each metastasis. Then, there is a treatment phase, during which the previous cell data is imported and a treatment strategy is implemented. This allows different treatments to be applied to the same set of metastases.

![flow chart](flow.png?raw=true "Flow chart")

This code uses a bash script (exe.sh) to compile and execute the main java program (Main.java). The Main class initializes the program and initializes the Primary class that aids in seeding the metastases and the World class where the arrayList of metastases (instances of the Met class) are stored along with graphics and output. Each instance of the Met class updates the time frame, stores the spatial configuration of a metastsis, stores an arrayList of cells that interact, and hosts the interactions between cells. The Cell class defines attributes and functions for an individual cell. The Pars class defines the parameters used in the simulation. The Data class contains variables and calculations for the data output to files, and the Draw class contains functions needed to produce the graphical output. The Functions class contains several generic functions used in the code.

### exe.sh
This is the bash script that deletes any old directories from previous runs, makes new directories, compiles the java files, and passes the arguments to run the java code.

### arrayVars0.txt
This file contains parameter sets to reproduce the displayed examples in the figures from the paper for testing and development purposes. This file (and its parameters) are imported as arguments via exe.sh. 

### Main.java
This is the main source file that sets the variables passed from the bash script, and initializes and runs the simulation. The output directories are created here, and the frame update is called with conditions for ending the simulation. 

### Pars.java
This file contains the parameters for the simulation.

### Primary.java 
This file contains functions and attributes for the Primary class. The primary is not explicitly modeled, but more as evolving traits that seed the metastases. 
* **initPrimary()**: this function calls the intialSeedTime() function and sets the initial proliferation rate for seeding the metastases.
* **delTraits()**: this function updates the trait distribution (mean proliferation rate, p, and std dev, het0) from which to seed the metastases.
* **initSeedTime()**: this function determines the timing for seeding the metastases based on the total burden and presumed final size of each metastasis.
* **seedMets(nSeed)**: this function returns traits for specific cells to be intialized in a new metastasis.
* **Pseed()**: this is a seed object to pass to intialize a new cell in a new netastasis.

### World.java
This file contains functions for the basic world around the set of metastases, initializing and updating the simulation. It also contains functions to output data and graphics. 
* **update()**: This function is called in the Main class and proceeds as follows:
  1. At the top of the frame, we write updated data to the screen
  2. Treatment is applied, if applicable.
  3. Any new metastases are seeding if time.
  4. Each metastasis is updated.
  5. Data is written to files.
* **setNewMet(i)**: new metastasis is seeded with index i given the current details from the Primary class.
* **setGrownMet(i)**: metastasis is seeded from file if during treatment phase with index i.
* **finalizeGrowth(frameNum)**: all cell configurations are saved to a file to reproduce for treatment phase.
* **drawArray(fNN)**: all metastases are draw together and written to a gif file.
* **writeGFilesAllMets()**: each individual metastasis with all cells is drawn and saved as gif file.

### Met.java
This file contains functions and attributes for the Met class, defining each metastasis by index in the constructor. 
* **setCellsNew(seeds)**: inputs from seeds whose traits are created in Primary.class, this creates cell objects for each seed.
* **getCellsInit(m)**: creates cell object during a simulation for treatment from output from a previous growth/seeding simulation  
* **frameUpdate()**: This function is called in the World class and proceeds as follows:
  1. At the top of the frame, we shuffle the indexes of cells.
  2. Then, we check for any random cell turnover, check for cell-cycle independent drug induced cell death, countdown the timers set for cells that are set to die, and remove dead cells.
  3. Then, assignGrid() is called that records information about cells within a coarsely-gridded structure to be accessed later. For each nieghborhood, a list of cell indexes and the total cell population is recorded.
  4. Finally, the cell loop is called that goes through all cells checking for quiesence due to lack of space from neighbors, and if not quiescent, and if already through the cell cycle (xDiv=1) it will either divide or have a possibility to be killed by a cell-cycle dependent drug. If not already throught the cell cycle (xDiv<1), the cell cycle updates.
* **findNeighbors(cell, ind)**: This function finds only the nearest neighbors of a cell by checking its neighborhood and the surrounding neighborhoods for cells and records their ID, their distance away from the cell, and the angle at which it resides from the cell. If the cell has neighbors, it calls the getTheta() function in the Cell class to find an empty angle to divide into or returns 999 if there are no empty angles where a cell can fit without overlap.
* **proliferate(openAngle,cell)**: This function uses the angle found from findNeighbors() to create a new cell at an angle given from the parental cell. It also assigns new trait values and resets other variables using the divNewParams() function found in the Cell class.
* **collectData()**: This function calculates average and standard deviation trait values over each metastasis.
* **drawIt()**: This function draws a background and renders all cells as defined in the Draw.class file.
* **writeGFile(frameNum,met)**: This function writes the graphics files, calling drawIt() twice: once to display cell colors by traits and once to display the cell state (proliferating, quiescent, dying). Then, these are written to gif files in folders specific to each metastasis and labeled with the associated frame number.

### Cell.java
This file contains specific functions and attributes for the Cell class.
* **setCell(trait1)**: This function sets up a newly created cell.
* **setCellTumor()**: This function sets up a cell from a file from a previously grown metastasis to be treated.
* **getTheta()**: This function records open angles into which a cell can divide in finalBank. It starts with 360 angles available and excludes angles due to the presence of neighboring cells using the excludeAngleRange() function and setting those angles to 999 depending on its angle and distance away. Any angle not excluded will be added to the list finalBank, and the angle for a new cell will be randomly chosen from this list.
* **draw(g,col)**: This function draws graphics g for a single cell, filled with color col and outlined a specific color depending on its state.

### Treatment.java
This file contains functions to apply treatment strategies to the set of metastases.
* **txTimer(frameNum,totMetCells,prevNumCells,maxDose)**: this function is for the adaptive therapy algorithm to keep track of the number of cells from the previous decision-making time point, and switch treatment on or off as needed.
* **ATtrial(ad,maxDose,tmC,frac)**: this function implements the adpative therapy trial protocol - as total number of cells (tmC) dips below a fraction (frac) of the original burden (totCells0), the treatment turns off, and it turns back on if reaching the original value again.
* **getTx(maxDose,totMetCells)**: the specific treatment is called according to its index.

### Data.java
This file contains variables and functions for calculating averages and standard deviations for traits output into the data folder.

### Draw.java
The functions for creating the graphical output in the movie folder are contained here. This includes color maps to display cell trait values.

### Functions.java
Several generic functions are stored here for writing to files and sampling from distributions. 

## Contributions & Feedback
Please contact me if you have any suggestions for improvement.

## Authors
* Jill Gallaher - Code, Investigation, Analysis, & Visualization
* Jill Gallaher, Maximilian Strobl, Jeffrey West, Mark Robertson-Tessi, Alexander R. A. Anderson - Conceptualization, Methodology
* Jill Gallaher, Maximilian Strobl, Jeffrey West, Jingsong Zhang, Mark Robertson-Tessi, Alexander R. A. Anderson - Writing, & Editing
* Robert Gatenby & Jingsong Zhang - Data Curation
* Alexander R. A. Anderson - Resources, Supervision, Funding Acquisition

