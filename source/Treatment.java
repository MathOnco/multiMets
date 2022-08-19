class Treatment {

    /*************
     * treatments
     ************/
	static void txTimer(int frameNum, double totMetCells, double[] prevNumCells, int maxDose){
        Treatment.getTx(maxDose, totMetCells);
        prevNumCells[1]=prevNumCells[0];//move old value to position 1
        prevNumCells[0]=totMetCells;//set current value at position 0

		if(Pars.tx>=3){//adaptive therapy here (tx=3 and tx=4)
			if(Pars.TxAdmin==0){//if currently off-Tx
				if(Pars.TxAdmin0==0){//and previous tx was off
					Pars.tOnOffTime++;
				}
				else{//if tx was on
					System.out.println("Tx OFF");
					Pars.tOnOffTime=0;
				}
			}
			else{//if currently on-Tx
				if(Pars.TxAdmin0!=0){//and previously on-tx
					Pars.tOnOffTime++;
				}
				else{//if tx was off
					System.out.println("Tx ON");
					Pars.tOnOffTime=0;
				}
			}
		}
        for(int m=0;m<World.mets.size();m++){//write tx decisions to file
            Met met = (Met) World.mets.get(m);
            int fN=frameNum*Pars.frameTime/(24);
			int[] tempVector = new int[]{fN,met.cells.size(),Pars.TxAdmin,Pars.dose};
			Functions.writeIntVector(Pars.outFile+"/data"+m+"/txD.txt",tempVector,true);
        }
    }

    static void txOff(){//set tx to off
		Pars.TxAdmin=0;
	}
	static void txOn(int ad,int dose){//set tx to on
		Pars.TxAdmin=ad;
		Pars.dose=dose;
	}

	static void flagCellsChemo(Cell cell){
		double doseF=1./(1.+Math.pow(25.f/Pars.dose,1.5));//dose calculation
		double tempC = doseF*(cell.p0);//cell sensitivity
		if(cell.druggable && Met.diceRoller.nextDouble()<=tempC*1.f){
			cell.deathMeter=Pars.deathTime;
		}
	}

	static void flagCellsCycleIndep(Cell cell){
		double deathRate = Pars.maxRDeathRate*Math.exp(-5*(1-cell.p0));//determines death rate
		if(cell.druggable && Met.diceRoller.nextDouble()<deathRate){
			cell.deathMeter=Pars.deathTime;
		}
	}

	//Adaptive therapy trial criteria with drug ad, maxDose, tx on til reaching specific fraction of
	//current total number of cells (tmC)
    private static void ATtrial(int ad, int maxDose,double tmC,double frac){
	    Pars.TxAdmin0=Pars.TxAdmin;
		if(Pars.TxAdmin==99 || tmC>1.f*Pars.totCells0){
			txOn(ad,maxDose);
		}
		else if(tmC<frac*Pars.totCells0){
			txOff();
		}
    }

	static void getTx(int maxDose,double totMetCells){//assign tx by index
		switch(Pars.tx){
			case 1: {//cell-cycle dependent continuous
				txOn(1,maxDose);
				break;}
			case 2: {//cell-cycle independent continuous
				txOn(2,maxDose);
				break;}
			case 3: {//cell-cycle dependent adaptive
				ATtrial(1, maxDose, totMetCells,.5f);
				break;}
			case 4: {//cell-cycle independent adaptive
				ATtrial(2, maxDose, totMetCells,.5f);
				break;}
			default: {
				break;}
	    }
	}

}
