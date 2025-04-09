package org.cloudbus.cloudsim.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class helperClass {

	public static int SALoadBalancing = 1;
	public static int HSLoadBalancing = 2;
	public static int HybridSAHSLoadBalancing = 3;
    public static int FCFS = 4;
    public static int LJF = 5;

    static int flagForAlgorithm;
    static int flagForPriority;
    static int numofCloudlet;

    public static int getNumOfCloudlets() {
        return numofCloudlet;
    }

    public static void readRandomFile() {
        String absoluteFilePath = new File("").getAbsolutePath();
        // for CPU
        Scanner scanCPU;

//        String strFilePath = absoluteFilePath.concat("/datasets/randomSimple/RandSimple1000.txt"); "D:\LB_SA_HS_SKRIPSI\cloudsim-3.0.3\CSDataset\SDSC\SDSC7395.txt"
        String strFilePath = absoluteFilePath.concat("/CSDataset/SDSC/SDSC7395.txt");

        File fileCPU = new File(strFilePath);
        try {
            scanCPU = new Scanner(fileCPU);
            while (scanCPU.hasNextDouble()) {
                double doubletemp = scanCPU.nextDouble();
                getRandomValue.setCPUValue(doubletemp);
                numofCloudlet++;
            }
            scanCPU.close();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
    }

    public static int getAlgorithmType() {
        return flagForAlgorithm;
    }

    public static void setAlgorithmType(int flag) {
        flagForAlgorithm = flag;
    }

    public static int getPriorityTask() {
        return flagForPriority;
    }

    public static void setPriorityTask(int priority) {
        flagForPriority = priority;
    }
}
