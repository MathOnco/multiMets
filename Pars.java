class Pars {

    /***********
     * imported
     * **********/
    static int totBurden;//total burden of all cells in all mets
    static int nM0;//number of metastases after growth period
    static double deathRate;//random cell turnover rate
    static int tx;//treatment value
    static boolean movieOn;//save movie pics?
	   
    /********************************************************************************************
    * OTHER PARAMETERS    **************************************************************************
    ********************************************************************************************/
    //output
    static String outFile;//folder to save results
    static boolean dataOn = true;//save data files?

    //layout
    static final int sizeW = 2025;//in pixels
    static final int sizeH = sizeW;//square
    static float widthMicm = 10125;//in microns

    //timescales (numbers in hours, converted to frames
    static int frameTime = 4;//time of each frame in hours
    static int movFrames= 14*24/frameTime;//time between movie frames
    static int dataFrames = 7*24/frameTime;//time between data frames
    static int txFreq = 7*24/frameTime;//time between monitoring tumor for tx decisions
    static int killTime = Math.round(4*365*24/(frameTime+0.f));//when to kill sim during tx phase

    //conversion factors
    static float micMToPix = sizeW/(widthMicm);//conversion of micrometer values to pixels

    //treatment
    static double totCells0;//total number of cells at start of treatment
    static int dose;//dose given with a max of 100
    static double maxRDeathRate=6./10.;//factor for cell-cycle independent drug action

    //mets
    static int numSeeds = 30;//number of starting cells of each met

    //cell properties
    private static final float cellDiam = 20;//cell size in microns
    static float rad= cellDiam*micMToPix/2;//calculate cell radius
    private static final float RadRat = 0.8f;//Ratio of radius' effective interaction
    static float effRad = rad*RadRat;//effective radius

    //proliferation dist properties
    static double pMin = Pars.frameTime/200.;//denominator is max intermitotic time in hours
    static double pMax = Pars.frameTime/50.;//denominator is min intermitotic time in hours

    //treatment
    static int TxAdmin = 0;//currently administered tx
    static int TxAdmin0=0;//previously administered tx
    static int tOnOffTime = 0;//counter of time for each cycle on-off
    static int deathTime = 18/Pars.frameTime;//numerator is time (hours) for cell to die and be removed
	    
}
