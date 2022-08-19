import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class World {
    static ArrayList mets = new ArrayList();//list of metastases
    private static final int maxDose = 100;//maximum dose for adaptive therapy
    static int tSinceMet = 0;//count time since last met seeded to determine next
    static int simIndex;//index of simulation
    static double[] prevNumCells = new double[]{0, 0};//stores current & previous number of cells for tx decisions
    static int totMetCells;//total number of cells in all metastases

    /*****************
     * INITIALIZE:
     ****************/
    static void initializeSim(){//initialize metastases
        if(Pars.tx==0){
            if(!Primary.seedSequential){//if simultaneous seeding, seed all mets
                for(int m = 0; m<Pars.nM0; m++){setNewMet(m);}
            }
            else{setNewMet(0);}//just seed 1st met
        }
        else{//if tx!=0, seed mets from txt files
            int nM0=Functions.getInt("../tum"+World.simIndex +"_0/nMets_"+ simIndex +".txt");
            for (int i = 0; i < nM0; i++){
                setGrownMet(i);
            }
        }
    }

    static void initializeTx(){//count cells and initialize tx parameters
        totMetCells = countMetCells();//count total cells in met
        for (var m : mets) {
            Met met = (Met) m;
            met.collectData();
        }
        if(Pars.tx!=0){
            prevNumCells[0]=totMetCells;
            Pars.TxAdmin = 99;//assign 99 at start of treatment period
            Pars.totCells0 = totMetCells;//define initial number of cells
            System.out.println("Tx ON");
        }
    }

    /******************
     * UPDATING
     *******************/
    static void update(int frameNum){
        //write updated info to screen
        if (frameNum%Pars.dataFrames == 0) { writeDataToScreen(frameNum);}

        //apply treatment
        if (frameNum%(Pars.txFreq) == 0 && Pars.tx!=0) {
            Treatment.txTimer(frameNum, totMetCells, prevNumCells, maxDose);
        }

        //if seeding, seed new mets
        if(Pars.tx==0){
            World.tSinceMet++;
            if(mets.size()<Pars.nM0 & tSinceMet>=Primary.timeSeed){
                setNewMet(mets.size());
            }
        }
        //update each metastasis
        for (int i = 0; i < mets.size(); i++) {
            Met met = (Met) mets.get(i);
            if(met.cells.size()>0) met.frameUpdate();
        }
        writeData(frameNum);//write data to files
    }

    /******************
     * MET SEEDING
     *******************/

    private static void setNewMet(int i){//initialize a new met and add to arrayList mets
        Met met = new Met(i);
        mets.add(met);
        setFolders(i);

        ArrayList seeds = Primary.seedMets(Pars.numSeeds);//set seeds from primary
        met.setCellsNew(seeds);//set cells from seeds
        Primary.delTraits();//update traits from primary
        tSinceMet=0;//reset time til next met
        System.out.println("NEW metastasis seeded!");
    }

    private static void setGrownMet(int i){//set met from txt files
        Met met = new Met(i);
        mets.add(met);
        setFolders(i);
        met.getCellsInit(i);
    }

    /*****************
     * DATA
     ****************/

    static int countMetCells(){//update data for all cells from all mets
        int totMetCells = 0;
        //totSens=0;
        for (Object met1 : mets) {
            Met met = (Met) met1;
            met.collectData();
            totMetCells += met.cells.size();
        }
        return totMetCells;
    }

    /******************
     * Data & output
     ******************/
    static void writeDataToScreen(int frameNum){
        int fN = (int) (frameNum*Pars.frameTime/24.);
        System.out.println(fN/7+" "+mets.size()+ " " + totMetCells);
    }

    static void setFolders(int m){
        new File(Pars.outFile+"/data"+ m).mkdir();

        if(Pars.movieOn){
            new File(Pars.outFile + "/movie").mkdir();
            new File(Pars.outFile + "/movie"+m+"").mkdir();
            new File(Pars.outFile + "/movie"+m+"/1").mkdir();
            new File(Pars.outFile + "/movie"+m+"/2").mkdir();
        }
        //initialize and write headers for data output files
        String sFile = Pars.outFile + "/data" + m + "/popTime.txt";
        String[] strVector = new String[]{"days", "total#cells", "#pro","#dead"};
        Functions.writeStringVector(sFile, strVector, true);
        sFile = Pars.outFile + "/data" + m + "/divAveStd.txt";
        strVector = new String[]{"days", "aveDivRate", "aveDRPro","aveDRQui","stdDevDR"};
        Functions.writeStringVector(sFile, strVector, true);
        if(Pars.tx!=0){//only write if tx on
            sFile = Pars.outFile + "/data" + m + "/txD.txt";
            strVector = new String[]{"days", "totNumCells", "Tx","dose"};
            Functions.writeStringVector(sFile, strVector, true);
        }
    }

    static void writeData(int frameNum){
        int getTime=(int) (frameNum*Pars.frameTime/(24+0.f));

        if (frameNum % Pars.dataFrames == 0 && Pars.dataOn) {
            int[] tempVector = new int[]{getTime,mets.size(),totMetCells,Pars.TxAdmin};
            Functions.writeIntVector(Pars.outFile+"/popTimeAll.txt", tempVector,true);

            //write mets stuff - data and graphics
            for (int i = 0; i < mets.size(); i++) {
                Met met = (Met) mets.get(i);
                String sFile = Pars.outFile+"/data"+i+"/popTime.txt";
                if (frameNum % Pars.dataFrames == 0 && Pars.dataOn && met.cells.size()>0) {
                    met.collectData();
                    Data.writeAllData(frameNum, i, Pars.outFile);
                    tempVector = new int[]{getTime,met.cells.size(), met.numPro, met.numDead};
                    Functions.writeIntVector(sFile, tempVector,true);
                }
                if (frameNum % Pars.movFrames == 0 && Pars.movieOn && met.cells.size()>0) {
                    met.writeGFile(frameNum, i);
                }
            }
        }
        if(frameNum%Pars.movFrames==0 && Pars.movieOn){//draw all mets together
            drawArray(getTime);
        }
    }

    static void finalizeGrowth(int frameNum){
        System.out.println("Finalizing on frame " + frameNum);
        String filename=Pars.outFile+"/nMets_"+ simIndex +".txt";
        Functions.writeInt(filename,mets.size(),true);//write final number of metastases to file
        //write final configuration and details of all cells to restart under treatment
        for (int m = 0; m < mets.size(); m++) {
            Met met = (Met) mets.get(m);
            Data.writeCellsInit(met.cells, m);
        }
    }

    /********************
     * Graphics
     *********************/

    static void drawArray(int fNN){//arrange array of metastases to be displayed
        int[] dims=new int[2];
        if (Pars.nM0 == 1) {dims[0]=1;dims[1]=1;}
        if (Pars.nM0 == 2) {dims[0]=2;dims[1]=1;}
        if (Pars.nM0 == 3) {dims[0]=3;dims[1]=1;}
        if (Pars.nM0 == 4) {dims[0]=4;dims[1]=1;}
        if (Pars.nM0 == 5) {dims[0]=5;dims[1]=1;}
        if (Pars.nM0 == 6) {dims[0]=5;dims[1]=2;}
        if (Pars.nM0 == 7 || Pars.nM0 == 8) {dims[0]=5;dims[1]=2;}
        if (Pars.nM0 == 9 || Pars.nM0 == 10) {dims[0]=5;dims[1]=2;}
        if (Pars.nM0 == 11 || Pars.nM0 == 12) {dims[0]=4;dims[1]=3;}
        if (Pars.nM0 >= 13 && Pars.nM0 <= 15) {dims[0]=5;dims[1]=3;}
        if (Pars.nM0 == 16) {dims[0]=4;dims[1]=4;}
        if (Pars.nM0 >= 17 && Pars.nM0 <= 20) {dims[0]=5;dims[1]=4;}
        if (Pars.nM0 == 9) {dims[0]=3;dims[1]=3;}//reset 9 as 3x3
        BufferedImage bi = new BufferedImage(2*Met.nN*dims[0], 2*Met.nN*dims[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D g0 = bi.createGraphics();
        g0.setColor(Color.white);
        g0.fillRect(0,0,2*Met.nN*dims[0],2*Met.nN*dims[1]);

        //draw each met
        for(int m=0;m<mets.size();m++){
            Met met = (Met) mets.get(m);
            Draw.drawDensArray(g0,Met.nN,Met.nN,2,met.gridPop,met.gridAve,m,dims);
        }
        File f1 = new File(Pars.outFile+"/movie/"+fNN+".gif");
        try {//write to file
            ImageIO.write(bi, "gif", f1);}
        catch (IOException ex) {ex.printStackTrace();}
    }

    static void writeGFilesAllMets(){//write graphics files for individual metastases
        if (Pars.movieOn) {
            for (int i = 0; i < World.mets.size(); i++) {
                Met met = (Met) World.mets.get(i);
                met.writeGFile(0, i);
            }
        }
    }

}
