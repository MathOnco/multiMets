import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

/**
 * The dividing Cell.
 */
public class Cell {
  float x, y;//x-y positions
  double p0;//proliferation rate within range (0-1)
  boolean quiescence;//quiescent proliferation state of cell
  boolean druggable;//determined by whether cell is on periphery
  int deathMeter, parent, generation;//countdown to death timer, parent and generation index
  double vDiv, xDiv;//division rate, position within cell-cycle (0-1)
   
  
  /*****************
  * CONSTRUCTOR: 
  ****************/
  Cell(float x, float y, int parent) {
    this.x = x;//x position
    this.y = y;//y position
    this.parent = parent;//id of parent cell
    this.quiescence = false;//quiescent state
    this.deathMeter = 9999;//9999 is not scheduled to die, anything less counts down in frames until 0=death
    this.druggable=false;//able to be killed by drug, determined by neighboring cells
  }

  /*****************
   * SET CELL
   **************/

  void setCell(double trait1){//sets new cell attributes
    this.generation=1;//generation of cell starting at 0 for growth simulation
    this.p0=trait1;//intermitotic time as percent within range
    this.xDiv = (Met.diceRoller.nextInt(100)+0.f)/100.;//start with random place in cell cycle
    this.vDiv = this.findDivRate();//proliferation rate
  }

  void setCellTumor(String[] splited){//info is imported from file from growth phase
    this.generation=Integer.parseInt(splited[4]);
    this.p0 = Double.parseDouble(splited[5]);
    this.xDiv = Float.parseFloat(splited[6]);
    this.vDiv = Float.parseFloat(splited[7]);
    this.quiescence = (Integer.parseInt(splited[8])==1);
  }

  double findDivRate(){
    return Pars.pMin+(Pars.pMax-Pars.pMin)*this.p0;
  }

  void divNewParams() {//update parameters on division
    this.generation += 1;
    this.xDiv = 0;
    this.vDiv=this.findDivRate();
  }

  /*************************************
  * Find available angles for division
  **************************************/
  ArrayList<Integer> finalBank = new ArrayList<Integer>();
  int[] angBank = new int[360];
  int detAng;

  //find an available angle for cell to divide into
  int getTheta(float[] nD, float[] nA, int nN){
    Random diceRoller = new Random();
    finalBank.clear();//clear final angle bank

    for(int i=0;i<360;i++){//add integer angles from 0-359
      angBank[i]=i;
    } 

    int angMin=0, angMax=0;
    double aCos=0;
    for (int q = 0; q<nN; q++){//go through all neighbors and check angle exclusions
      aCos = (this.quiescence)?Math.acos(nD[q]/(4.f*Pars.rad)):Math.acos(nD[q]/(4.f*Pars.effRad));
      angMin = (int) Math.floor(Math.toDegrees(nA[q]-aCos)-1);
      angMax = (int) Math.ceil(Math.toDegrees(nA[q]+aCos)+1);
      excludeAngleRange(angMin,angMax);
    }
    //add to bank all angles that can accommodate a cell
    for(int i=0;i<360;i++){
      if(angBank[i]!=999){
        finalBank.add(i);
      }
    }
    int fbs=finalBank.size();
    if(fbs==0){
      detAng=999;//will lead to totally random choice
    } else{
      detAng=finalBank.get(diceRoller.nextInt(fbs));//choose from available
    }
    return detAng;
  }

  void excludeAngleRange(int aMin, int aMax){//exclude integer angles over a range
    for (int y=aMin;y<=aMax;y++){
      int temp=y;
      if(temp<0) temp=temp+360;
      if(temp>=360)temp=temp-360;
      angBank[temp]=999;
    }
  }
   
  /***************************
  * GRAPHICS
  ******************************/

  void draw(Graphics g, Color col) {
    g.setColor(col);//fill color specified
    double radScale=2;//scale size of cell for easier visualization
    g.fillOval((int)(x - Pars.rad), (int)(y - Pars.rad), (int)(2 * Pars.rad*radScale),(int)(2 * Pars.rad*radScale));

    //outline color according to quiescence
    if(this.quiescence){ g.setColor(Color.gray); }
    else{ g.setColor(Color.black); }
    g.drawOval((int)(x - Pars.rad),  (int)(y - Pars.rad), (int)(2 * Pars.rad*radScale),(int)(2 * Pars.rad*radScale));
    
  } 

}


