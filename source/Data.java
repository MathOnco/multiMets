import java.io.*;
import java.util.ArrayList;

public class Data{

   	static float divAve,  divStd, divAvePro, divAveQui;//average, std dev of division times for all, proliferative, and quiescent cells
   	public static float tempDS;//intermediate value in calculating std dev
   	public static int totProlif, totQui;//counting total cells that are proliferating vs quiescent

	static void initialize(Met met){//find traits of total and each met
		totProlif = totQui = 0;
	   	divAve = divStd = divAvePro = divAveQui  = 0;
		met.numPro = 0;
		met.numDead = 0;
	}

	/**************
	* CALCULATIONS
	**************/

	static void addToAverage(Cell cell, Met met){
		//sum up over all cells
		divAve += cell.p0;
		divAvePro += (!cell.quiescence) ? cell.p0 : 0;
		divAveQui += (cell.quiescence) ? cell.p0 : 0;

		//sum up via each met
		met.numPro+=(!cell.quiescence) ? 1 : 0;
		met.numDead+=(cell.deathMeter<9999) ? 1 :0;
	}

	static void normalizeDivVals(int currentNumCells, int numP){
		divAve = divAve/(currentNumCells);
		divAvePro = divAvePro/numP;
		divAveQui = (currentNumCells-numP==0)?0:Pars.frameTime*divAveQui/(currentNumCells-numP);
	}

	static void sumToSquaredDiff(double div){
		tempDS += (div-divAve)*(div-divAve);
	}

	static void findStdDev(int numCells){ divStd = (float) (Math.sqrt(tempDS/numCells)); }

	/***************
	* WRITE TO FILES
	****************/
	static void writeAllData(int fNum, int m, String s){//write div values to files
		int fNN=fNum*Pars.frameTime/(24);
		String strF = s+"/data"+m+"/divAveStd.txt";
		String str = fNN+"	"+divAve+"	"+divAvePro+"   "+divAveQui+"   "+divStd;
		Functions.writeString(strF, str,true);
	}

	static void writeCellsInit(ArrayList cells, int m){//write all cell info to file at end of sim
		try{
			String strF=Pars.outFile+"/cellsInit"+World.simIndex +"_"+m+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(strF,true));
			for(int i=0; i<cells.size();i++){
				Cell cell = (Cell) cells.get(i);
				int cQ=(cell.quiescence)?1:0;
				fout.write(i+" "+Math.round(10000*cell.x)/10000.+" "+Math.round(10000*cell.y)/10000.+" "+cell.parent+" "+cell.generation+" "+Math.floor(100*cell.p0)/100.+" "+
					cell.xDiv+" "+Math.round(100*cell.vDiv)/100.+" "+cQ+"\n");
			}
			fout.close();
		}
		catch(IOException e){
			System.out.println("There was a problem"+e);
		}
	}


}
