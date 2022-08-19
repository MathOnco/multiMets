import java.util.*;
import java.awt.*;

public class Draw{
	//draws a background color
	static void background(Graphics g1, int sW, int sH){
		g1.setColor(Color.white);
	  	g1.fillRect(0,0,sW,sH);
	}

	//draws the average trait value defined on the coarse grid
	static void drawDensArray(Graphics2D g1,int meshNx, int meshNy, int binSize, int[][] densCells, double[][] aveCells,int m,int[] dims){
		int i0=(m%dims[0])*meshNx*binSize;
		int j0=(int) (meshNy*binSize*Math.floor(m/dims[0]));
		for (int i = 0; i < meshNx-1; i++) {
			for(int j = 0; j < meshNy-1; j++) {
				double tempP=aveCells[i][j];
				double tempR=1-tempP;
				Color col = (densCells[i][j]>0)?colorNew2DMap(tempP,tempR):Color.WHITE;
				g1.setColor(col);
				g1.fillRect((int) (i+0)*binSize+i0,(int) (j+0)*binSize+j0,binSize,binSize);
			}
		}
	}

	static void renderCells(Graphics g1, ArrayList cells, int trait){//draw all cells in met
		for (int i = cells.size(); --i >=0; ) {
			Color col;
			Cell cell = (Cell) cells.get(i);

			double tempT1=cell.p0;
			double tempT2=1-cell.p0;

			boolean dying=cell.deathMeter<9999;

			if(trait==1){
				col = colorNew2DMap(tempT1,tempT2);
			}
			else{
				col=druggability(dying,cell.druggable,!cell.quiescence);
			}
			cell.draw(g1,col);
		}
	}

	/**********************
	* COLOR SET FUNCTIONS
	***********************/

	private static Color colorNew2DMap(double x,double y){
		double xd,yd;
		double delD,delT,delR;
		double temp;
		Color col;
		double r00=Math.sqrt(x*x+y*y);
		double r11=Math.sqrt((x-1)*(x-1)+(y-1)*(y-1));
		if(r00<=r11) {
			xd=(x==0)?.00001:1./(1 + y/x);
			yd=1-xd;
			delD = Math.sqrt((x - xd) * (x - xd) + (y - yd) * (y - yd));//dist from point to diag
			delT = Math.sqrt((xd - 0) * (xd - 0) + (yd - 0) * (yd - 0));//dist from diag to 0,0
			delR = delD / delT;//relative dist from diag to make brightness
			if(yd >= .5) {
				temp = (yd - .5) / .5;
				col = new Color((float)(temp+(1-temp)*delR), (float)(delR), (float)(1-temp+temp*delR));
			}
			else{
				temp = (xd - .5) / .5;
				col =new Color((float)(delR), (float) (temp+(1-temp)*delR),(float) (1-temp+temp*delR));
			}
		}
		else{
			xd = 1. / (1 + (1 - x) / (1 - y));
			yd = 1 - xd;
			delD = Math.sqrt((x - xd) * (x-xd) + (y-yd)*(y-yd));
			delT = Math.sqrt((xd - 1) * (xd - 1) + (yd - 1) * (yd - 1));
			delR = delD/delT;
			if (yd >= .5){
				temp = (yd - .5) / .5;
				col =new Color((float)(temp - temp * delR), 0, (float)(1 - temp - (1 - temp) * delR));
			}
			else{
				temp = (xd - .5) / .5;
				col =new Color(0,(float) (temp-temp*delR), (float) (1-temp-(1-temp)*delR));
			}
		}
		return col;
	}

	private static Color druggability(boolean dying,boolean db,boolean pro){
		if(dying && db){
			return Color.red;
		}
		else if(dying){
			return Color.blue;
		}
		else if(db){
			return Color.black;
		}
		else if(!db && pro){
			return Color.green;
		}
		else{
			return Color.yellow;
		}
	}


}
