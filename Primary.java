import java.util.ArrayList;

public class Primary {
    static boolean seedSequential;//whether to seed all at once (F) or sequential (T)
    static double timeSeed;//time frames between each metastasis seeding

    static double p0;//initial mean proliferation rate
    static double p=0;//current proliferation rate
    static double delP;//change in proliferation rate to next metastasis
    static double het0;//initial heterogeneity
    static double delHet;//change in heterogeneity to next metastasis

    static void initPrimary(){//initialize primary features
        initSeedTime();
        p=p0;
    }

    static void delTraits(){//change the traits of cells coming from primary
        //decrease proliferation rate by delP
        p-=((p>=delP && delP>=0) || (p<=1.+delP && delP<0))?delP:(p<delP && delP>=0)?p:0;
        //increase heterogeneity by delHet
        het0+=((delHet<0 && Math.abs(delHet)<=het0 && het0>=0)|| delHet>0 )?delHet:(Math.abs(delHet) >=het0 && het0>=0)?-het0:0;
    }

    static void initSeedTime(){
        int sigI = 0;//first add up the relative sizes of each metastasis
        for (int i=1; i<=Pars.nM0; i++){
            sigI=sigI+i;
        }
        timeSeed = Pars.totBurden/(25.f*sigI);//calculate time to seed
    }

    static ArrayList seedMets(int nSeed){//define cells to metastasis from primary
        ArrayList pSeeds = new ArrayList();//list of cell seeds
        for (int i=0;i<nSeed;i++){
            Pseed pseed;
            double seedT = Functions.boundedGaussian(p,het0,0,1);
            pseed = new Pseed(seedT);
            pSeeds.add(pseed);
        }
        return pSeeds;
    }
}

class Pseed {//defines each seed for a new metastasis
    double p;

    /*****************
     * CONSTRUCTOR:
     ****************/
    Pseed(double p) {
        this.p = p;
    }
}
