import java.io.File;

/**
 * Main Program.
 * Created by J. A. Gallaher
 */
public class Main {
    private static final Main shell = new Main();

    //boolean flags
    static boolean detectDisease =false;//flag when to start treatment at Dx
    static boolean stopTreatment=false;//flag when to stop treatment

    static int numPars = 8;//number of parameters passed as arguments
    static int[] input = new int[numPars];//temporarily stores the input parameters

    public static void main(String[] args) {
        for(int i=0; i<numPars; i++){
            input[i]=Integer.parseInt(args[i]);//parse the input arguments
        }
        setInput(input);//set the input values to variables
        Pars.tx = Integer.parseInt(args[numPars]);//set the growth/treatment
        World.simIndex =Integer.parseInt(args[numPars+1]);//set the simulation index
        Pars.movieOn = Integer.parseInt(args[numPars+2])!= 0;//indicate whether to save gif files

        shell.initializeOutput();//set head and create output folders/files
        if(Pars.tx==0){Primary.initPrimary();}//setup primary details
        shell.writeSimDetails(input);//write sim details to screen
        shell.start();//start the simulation
    }

    static void setInput(int[] input){
        Pars.totBurden =input[0];//number of cells
        Pars.nM0 = input[1];//number of metastases
        Primary.p0 = input[2]/100.;//proliferation rate as a percent from min to max
        Primary.delP=0.01*input[3]/(Pars.nM0-1.0);//change in proliferation rate as a percent over each new metastasis
        Primary.seedSequential = input[4] != 0;//boolean if 1, seeds sequential, otherwise, simultaneous
        Pars.deathRate=input[5]/10000.;//cell turnover rate (random, applies to all cell states)
        Primary.het0=input[6]/100.;//initial heterogeneity - standard deviation of distribution to sample proliferation rates
        Primary.delHet =input[7]/100.;//change in percent of range of heterogeneity
    }

    private void start() {
        int frameNum = 0;
        World.writeGFilesAllMets();//write all graphics files
        World.initializeSim();//setup the initial metastases
        World.initializeTx();//setup treatment details
        while (true) {//breaks are within the loop
            World.totMetCells = World.countMetCells();//count total cells in met
            if (Pars.tx==0 && detectDisease) {//stop growth if disease is detected through primary, single met, or total met burden
                World.finalizeGrowth(frameNum);
                break;
            } else if(Pars.tx!=0 && stopTreatment) {//stop treatment if exceeding time, cells exceeding threshold or cells all gone
                break;
            } else{//otherwise continue
                World.update(frameNum);
                frameNum++;
            }
            //flag for stopping pre-tx...
            detectDisease=World.totMetCells>Pars.totBurden;

            //flag for stopping post-tx...
            stopTreatment=World.totMetCells<=0 || //all mets are gone
                    World.totMetCells>1.2*Pars.totBurden ||   //mets exceed 20% over threshold
                    frameNum >= Pars.killTime; //time is up
        }
    }

    /******************
     * INITIALIZATION
     *******************/
    private void initializeOutput(){//set no head and output directories
        System.setProperty("java.awt.headless", "true");

        Pars.outFile="../tum"+World.simIndex +"_"+Pars.tx;//update to be more descriptive?
        new File(Pars.outFile).mkdir();
    }

    private void writeSimDetails(int[] input){//write sim details to screen
        String[] strVector = new String[]{"totBurden","mets","p0","delP","tS","rDr","het0","delHet",};
        Functions.writeStringVector(Pars.outFile+"/params.txt",strVector,true);
        Functions.writeIntVector(Pars.outFile+"/params.txt",input,true);
        strVector = new String[]{"days","#mets","total#cells","tx"};
        Functions.writeStringVector(Pars.outFile+"/popTimeAll.txt",strVector,true);

        System.out.println(".............");
        if(Pars.tx==0){writeGrowthInfo();}else{
            writeTxInfo();
        }
        System.out.println(".............");
        System.out.println("time(w) #mets #cells");
    }

    private void writeGrowthInfo() {//write info for growth phase
        int s0=0;String m0="0";String ex="";int tS=(int) Math.round(Primary.timeSeed*Pars.frameTime/24.);
        int pp=(int) (100*Primary.p0);int dp=(int) (100*Primary.delP);double dR=Pars.deathRate;
        if(!Primary.seedSequential){s0=Pars.nM0;m0="metastases";ex=".";}//add seeding rate in here after start number
        else{s0=1;m0="metastasis";ex=" amongst "+Pars.nM0+" metastases, which are seeded every "+tS+" days.";}

        System.out.println("Initializing simulation "+World.simIndex +" with "+s0+" "+m0+",");
        System.out.println("which will grow until reaching a total of "+Pars.totBurden +" cells"+ex);
        System.out.println("The mean starting sensitivity is "+pp+"%, changing "+dp+"% each new metastasis.");
        System.out.println("The turnover rate is a death every ~"+dR+" hours, ");
        System.out.println("the initial intrametastatic heterogeneity is "+100*Primary.het0+"%,");
        System.out.println("and the change in intrametastatic heterogeneity for each new metastasis is "+100*Primary.delHet+"%.");
        System.out.println("Output will go into file: "+Pars.outFile);
    }
    private void writeTxInfo() {//write info for tx phase
        String m0="0";String txStr="";String txTypeStr="";
        if(Pars.nM0>1){m0="metastases";}
        else{m0="metastasis";}
        if(Pars.tx>=3){txStr="Adaptive treatment";}else{txStr="Continuous treatment";}
        if(Pars.tx==1 || Pars.tx==3){txTypeStr="dependent";}else{txTypeStr="independent";}
        int kT=(int) (Pars.killTime*Pars.frameTime/(24.));

        System.out.println("Initializing simulation "+World.simIndex +" with "+Pars.nM0+" "+m0+",");
        System.out.println("totalling "+Pars.totBurden +" cells.");
        System.out.println(txStr+" starts now with a cell-cycle "+txTypeStr+" drug.");
        System.out.println("The simulation will stop at "+kT+" days.");
        System.out.println("Output will go into file: "+Pars.outFile);
    }

}
