import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class Met {
	int metIndex;//metastasis index
	ArrayList cells = new ArrayList();//list of cells

	static Random diceRoller = new Random();//random number generator
	int numPro, numDead;//number proliferating and dead cells
	private float angRad;//stores the angle at which to put a new cell

	/*****************
	 * CONSTRUCTOR:
	 ****************/
	Met(int m) {
		this.metIndex = m;
	}

	/********
	* neighbor lists
	*********/
	static int nN = Math.round(Pars.sizeW/18.f);//dimension of coarse grid, scaled to domain size
	static int sN = Pars.sizeW/nN;//number of coarse grid points in domain
	private final int maxO = 8*80;//number of cells possible in grid point
	int[][][] gridN = new int[nN][nN][maxO];//indexes of cells in each grid point
	int[][] gridPop = new int[nN][nN];//number of cells in each grid point
	double[][] gridAve = new double[nN][nN];//temp number to calculate ave proliferation rate per grid point

	private final int maxN=400;//max number in vectors to store neighbor info
	private final int[] neighborList = new int[maxN];//stores indexes of neighbors
	private final float[] neighborDist = new float[maxN];//stores distance to neighbors
	private final float[] neighborAng = new float[maxN];//stores angles to neighbors
	static int neighborNum=0;//number of neighbors

    /********************
    * initialize cells
    *********************/
	//set up a newly created cell
	void setCellsNew(ArrayList seeds){
		for (Object o : seeds) {
			Pseed seed = (Pseed) o;
			int x0 = (int) Functions.boundedGaussian(Pars.sizeW / 2., 15, 0, Pars.sizeW);
			int y0 = (int) Functions.boundedGaussian(Pars.sizeH / 2., 15, 0, Pars.sizeH);
			Cell cell = new Cell(x0, y0, -99);
			cells.add(cell);
			cell.setCell(seed.p);
		}
	}

	//set metastasis from initial cell configs stored in file
	void getCellsInit(int m){
		try {
			String inFile="../tum"+World.simIndex +"_0/cellsInit"+World.simIndex +"_"+m+".txt";
		  	FileInputStream inF = new FileInputStream(inFile);
		  	BufferedReader br = new BufferedReader(new InputStreamReader(inF));

		  	//find number of lines in file
		  	int lines=0;
		  	while(br.readLine()!=null) lines++;
		  	inF.close();

		  	//go through each line and set values to each new cell
		  	inF = new FileInputStream(inFile);
		  	br = new BufferedReader(new InputStreamReader(inF));
		  	String str;
		  	for (int i=0;i<lines;i++){
		  		str=br.readLine();
			  	String[] splited = str.split(" ");
			  	int parent = Integer.parseInt(splited[3]);
			  	Cell cell = new Cell(Float.parseFloat(splited[1]), Float.parseFloat(splited[2]),parent);
    	  		cells.add(cell);
    	  		cell.setCellTumor(splited);
		  	}
		  	inF.close();
		} catch (IOException e) {
			System.out.println("Error at initialization");
		}
	}

	/*******************
	* frame update
	*******************/
	void frameUpdate() {
		Collections.shuffle(cells);//shuffle indexes
		this.checkDeath();//check for any death and update
		this.assignGrid();//assign cells to coarse grid
		this.cellLoop();//go through cells during 1 time step
	}

	private void assignGrid(){
		//initialize grid to zeros
		for(int i=0;i<nN;i++){
			for(int j=0;j<nN;j++){
				gridPop[i][j]=0;
				gridAve[i][j]=0;
				for(int k=0;k<maxO;k++){
					gridN[i][j][k]=-1;
				}
			}
		}
		//populate grid with values from cells
		int gP;
		for(int i = cells.size(); --i >= 0; ){
			Cell cell = (Cell) cells.get(i);
			int gIndX = (int) Math.floor(cell.x/sN);
		    int gIndY = (int) Math.floor(cell.y/sN);
		    gIndX = (gIndX>=nN) ? nN-1 : Math.max(gIndX, 0);
		    gIndY = (gIndY>=nN) ? nN-1 : Math.max(gIndY, 0);
		    gP = gridPop[gIndX][gIndY];
		    gridN[gIndX][gIndY][gP]=i;
		    gridPop[gIndX][gIndY]++;
		    gridAve[gIndX][gIndY]+=cell.p0;
		}
		//finalize average value calculation
		for(int i=0;i<nN;i++){
			for(int j=0;j<nN;j++){
				if(gridPop[i][j]>0){
					gridAve[i][j]/=gridPop[i][j];
				}
			}
		}
	}

	/**********************************
	* checks
	**********************************/
	private void checkDeath(){
		for (int i = cells.size()-1; i>=0; i--) {    	  	
	   		Cell cell = (Cell) cells.get(i);

			if(cell.deathMeter==9999){//check for random cell death
				if(Pars.deathRate>0 && diceRoller.nextDouble()<Pars.deathRate){
					cell.deathMeter=Pars.deathTime;
				}
				if(Pars.TxAdmin==2){//check for cell-cycle independent death
					Treatment.flagCellsCycleIndep(cell);
				}
			}
	   		if(cell.deathMeter<9999){ //if death started, countdown til removal
	   			cell.deathMeter-=1;
	   		}
	   		if(cell.deathMeter<=0){//remove dead cells
	   			cells.remove(i);
	   		}
	   	} 
	}

	/*************
	*Other
	***********/

	private void cellLoop(){
		for(int i = cells.size(); --i >= 0; ){
		    Cell cell = (Cell) cells.get(i);
		   	findNeighbors(cell,i);

	        int openAngle=diceRoller.nextInt(360);
	        if (neighborNum > 0){openAngle = cell.getTheta(neighborDist, neighborAng, neighborNum);}

	        //Is the cell surrounded? -> quiescent, openAngle=999 is all angles open, cell.deathMeter<9999 is scheduled to die
	        if (openAngle == 999 || cell.deathMeter<9999){
				cell.quiescence = true;
	        }
	        else{ //or not? -> proliferating
                cell.quiescence = false;
                cell.vDiv=cell.findDivRate();
        		
        		//READY TO DIVIDE?
		        if(cell.xDiv>=1){
					if((Pars.TxAdmin==1) && cell.deathMeter==9999 && cell.druggable){//chemo can attack during division
						Treatment.flagCellsChemo(cell);
					}
					if(cell.deathMeter==9999){//otherwise, divide
						proliferate(openAngle,cell);
					}
		   	    }
			    else{//update the temporal position through the cell cycle
					cell.xDiv += cell.vDiv;
	        	}
	   
			} //end of pro
		}//end of cell loop
	}//end function cellLoop

	private void findNeighbors(Cell cell, int ind){
		//initialize neighbor vectors
		for(int n=0;n<maxN;n++){
	    	neighborList[n]=0;
	    	neighborDist[n]=0;
	    	neighborAng[n]=0;
	    }

	    neighborNum=0;
		int numZeroNs=0;
		double minNeiDist=sN*2;
	    int cellIndX = (int) Math.floor(cell.x/sN);//find grid point index for cell
	    int cellIndY = (int) Math.floor(cell.y/sN);
	    cellIndX = (cellIndX>=nN) ? nN-1 : cellIndX;
	    cellIndY = (cellIndY>=nN) ? nN-1 : cellIndY;
	    Integer nXT[]={0, 0,0,1, 1,1,-1,-1,-1};//this defines a Moore neighborhood grid around cell
	    Integer nYT[]={0,-1,1,0,-1,1, 0, 1,-1};
	    int[] nX;
	    int[] nY;
	    //coarse grid points at edges or corners have limited neighboring grid points
	    if(cellIndX==0 && cellIndY==0){
	    	nX=new int[]{nXT[0],nXT[2],nXT[3],nXT[5]};
	    	nY=new int[]{nYT[0],nYT[2],nYT[3],nYT[5]};
	    }
	    else if(cellIndX==nN-1 && cellIndY==nN-1){
	    	nX=new int[]{nXT[0],nXT[1],nXT[6],nXT[8]};
	    	nY=new int[]{nYT[0],nYT[1],nYT[6],nYT[8]};
	    }
	    else if(cellIndX==nN-1 && cellIndY==0){
			nX=new int[]{nXT[0],nXT[2],nXT[6],nXT[7]};
			nY=new int[]{nYT[0],nYT[2],nYT[6],nYT[7]};
	    }
	    else if(cellIndX==0 && cellIndY==nN-1){
			nX=new int[]{nXT[0],nXT[1],nXT[3],nXT[4]};
			nY=new int[]{nYT[0],nYT[1],nYT[3],nYT[4]};
	    }
	    else if(cellIndX==0){
	    	nX=new int[]{nXT[0],nXT[1],nXT[2],nXT[3],nXT[4],nXT[5]};
	    	nY=new int[]{nYT[0],nYT[1],nYT[2],nYT[3],nYT[4],nYT[5]};
	    }
	   	else if(cellIndX==nN-1){
	    	nX=new int[]{nXT[0],nXT[1],nXT[2],nXT[6],nXT[7],nXT[8]};
	    	nY=new int[]{nYT[0],nYT[1],nYT[2],nYT[6],nYT[7],nYT[8]};
	    }
	   	else if(cellIndY==0){
	    	nX=new int[]{nXT[0],nXT[2],nXT[3],nXT[5],nXT[6],nXT[7]};
	    	nY=new int[]{nYT[0],nYT[2],nYT[3],nYT[5],nYT[6],nYT[7]};
	    }
	    else if(cellIndY==nN-1){
	    	nX=new int[]{nXT[0],nXT[1],nXT[3],nXT[4],nXT[6],nXT[8]};
	    	nY=new int[]{nYT[0],nYT[1],nYT[3],nYT[4],nYT[6],nYT[8]};
	    }
	    else{
	    	nX=new int[]{nXT[0],nXT[1],nXT[2],nXT[3],nXT[4],nXT[5],nXT[6],nXT[7],nXT[8]};
	    	nY=new int[]{nYT[0],nYT[1],nYT[2],nYT[3],nYT[4],nYT[5],nYT[6],nYT[7],nYT[8]};
	    }

	    //identify cells in surrounding grid points
        for (int j=0;j<nX.length;j++){
        	int tBx=nX[j]+cellIndX;
        	int tBy=nY[j]+cellIndY;
			if(tBx>=0 && tBy>=0 && tBx<nN && tBy<nN){
				double thisDist=Math.sqrt((cell.x-(tBx+.5)*sN)*(cell.x-(tBx+.5)*sN)+(cell.y-(tBy+.5)*sN)*(cell.y-(tBy+.5)*sN));
				if(gridPop[tBx][tBy]<2){
					numZeroNs++;
					if(thisDist<=minNeiDist) {
						minNeiDist = thisDist;
					}
				}
        		for (int k=0;k<gridPop[tBx][tBy];k++){
        			int eI=gridN[tBx][tBy][k];
        			if(eI!=ind){//do not compare cell to itself
        				Cell cellOther = (Cell) cells.get(eI);
        				float xDist = cell.x-cellOther.x;
        				float yDist = cell.y-cellOther.y;
        				float rDist = (float) (Math.sqrt(xDist*xDist+yDist*yDist));
        				if(rDist<=4*Pars.effRad){//only compare neighbors that are within 2 effective cell diamters
        					neighborList[neighborNum]=eI;
        					neighborDist[neighborNum]=rDist;
        					float theta = (float) (Math.atan2(-yDist, xDist)+1.f*Math.PI);
        					neighborAng[neighborNum]= (theta<0) ? (float) (theta+Math.PI+Math.PI) : theta;
        					neighborNum=neighborNum+1;
        				}
        			}
        		}
        		
            }
        }
        //druggability is determined by above criteria
		cell.druggable=((numZeroNs>1||(numZeroNs==1&&minNeiDist<=0.5*sN*sN)) && !cell.quiescence);
	}

	void proliferate(int openAngle, Cell cell){//create new cell and define traits
		angRad = (float) Math.toRadians(openAngle);
		float xPos =(float) (cell.x+2*Pars.effRad*(Math.cos(angRad)));
		float yPos = (float) (cell.y+2*Pars.effRad*(-Math.sin(angRad)));
		Cell child = new Cell(xPos,yPos,cell.parent);
		cells.add(child);
		child.p0 = cell.p0;
		cell.divNewParams();
		child.divNewParams();
	}

	void collectData(){//collect data to write to files
		Data.initialize(this);//zero out data to be calculated

		for (int i = cells.size(); --i >= 0; ) {
			Cell cell = (Cell) cells.get(i);
			Data.addToAverage(cell,this);
		}
		Data.normalizeDivVals(cells.size(), numPro);

		for (int i = cells.size(); --i >=0; ){
			Cell cell = (Cell) cells.get(i);
			Data.sumToSquaredDiff(cell.p0);
		}
		Data.findStdDev(cells.size());
	}

	/********************
	* GRAPHICS
	*********************/

	private BufferedImage drawIt(int trait){//draw metastasis & all cells
		BufferedImage bi = new BufferedImage(Pars.sizeW, Pars.sizeH, BufferedImage.TYPE_INT_RGB);
	  	Graphics2D g0 = bi.createGraphics();

	  	//main drawing
	  	Draw.background(g0,Pars.sizeW,Pars.sizeH);
	  	Draw.renderCells(g0,cells,trait);

	    return bi;
	}

	void writeGFile(int frameNum, int met){
        int fNN=frameNum*Pars.frameTime/(24);
        if(this.cells.size()>0){
        	//image with cells colored according to traits
			BufferedImage bi1;
			bi1=drawIt(1);
			File f1 = new File(Pars.outFile+"/movie"+met+"/1/"+fNN+".gif");
			try {ImageIO.write(bi1, "gif", f1);}
			catch (IOException ex) {ex.printStackTrace();}

			//image with cells colored according to state (proliferating, quiescent, etc)
			BufferedImage bi2;
			bi2=drawIt(2);
			File f2 = new File(Pars.outFile+"/movie"+met+"/2/"+fNN+".gif");
			try {ImageIO.write(bi2, "gif", f2);}
			catch (IOException ex) {ex.printStackTrace();}
		}

	}

	
}
