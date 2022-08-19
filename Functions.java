import java.util.Random;
import java.io.*;
import java.util.Scanner;

class Functions {
	static private Random diceRoller = new Random();

    /***********************
     * Input
     ***********************/
    //get an integer from file
    static int getInt(String filename){
        int thisVal = 0;
        try {
            Scanner input=new Scanner(new File(filename));
            thisVal = input.nextInt();

        } catch (IOException ignored) {
        }
        return thisVal;
    }

    /***********************
     * Output
     ***********************/
    //write an integer to a file
    static void writeInt(String filename, int val,boolean newLine){
        try{
            BufferedWriter fout0 = new BufferedWriter(new FileWriter(filename,true));
            fout0.write(val+" ");
            if(newLine) fout0.write("\n");
            fout0.close();
        }
        catch(IOException e){
            System.out.println("There was a problem"+e);
        }
    }

	//write integer vector to files
    static void writeIntVector(String strF, int[] vector,boolean newLine){
        try{
            BufferedWriter fout0 = new BufferedWriter(new FileWriter(strF,true));
            for (int aVector : vector) {
                fout0.write(aVector + " ");
            }
            if(newLine) fout0.write("\n");
            fout0.close();
        }
        catch(IOException e){
            System.out.println("There was a problem"+e);
        }
    }

    //write string to files
    static void writeString(String strF, String str,boolean newLine){
        try{
            BufferedWriter fout0 = new BufferedWriter(new FileWriter(strF,true));
            fout0.write(str);
            if(newLine) fout0.write("\n");
            fout0.close();
        }
        catch(IOException e){
            System.out.println("There was a problem"+e);
        }
    }

    //write string vector to files
    static void writeStringVector(String strF, String[] vector,boolean newLine){
        try{
            BufferedWriter fout0 = new BufferedWriter(new FileWriter(strF,true));
            for (String aVector : vector) {
                fout0.write(aVector + " ");
            }
            if(newLine) fout0.write("\n");
            fout0.close();
        }
        catch(IOException e){
            System.out.println("There was a problem"+e);
        }
    }

    /******************
     * Sampling
     ******************/
    //sample from a bounded Gaussian distribution
    static double boundedGaussian(double mean, double dev, double min, double max){
        double gauss = diceRoller.nextGaussian();
        double val = dev*gauss+mean;
        while(val>max || val<min){
            gauss = diceRoller.nextGaussian();
            val = dev*gauss+mean;
        }
        return val;
    }

}