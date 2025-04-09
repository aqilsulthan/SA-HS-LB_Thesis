package org.cloudbus.cloudsim.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.examples.getRandomValue;
import org.cloudbus.cloudsim.examples.helperClass;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class myLoadBalancing {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;
    private static List<Host> hostList;

    private static int numofTestVM = 30;
    private static int numofTestCloudlet = 4;

    public static void main(String[] args) {
        Log.printLine("Starting Load Balancing Simulator...");

        try {
            int num_user = 1;
            int hostId = 0;

            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0", hostId);
            hostId++;

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            vmlist = new ArrayList<Vm>();
            hostList = datacenter0.getHostList();

            long size = 10000; // Image size (MB)
            int pesNumber = 1; // Number of CPUs
            String vmm = "Xen";

            for (int i = 0; i < numofTestVM; i++) {
                int mips = 500 + (int) (Math.random() * 500); // Randomize MIPS between 1000 and 1500
                int ram = 512 + (int) (Math.random() * (2048 - 512)); // Randomize RAM between 512 and 2048
                long bw = 10000; // Randomize bandwidth between 1000 and 1500

                Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
                vmlist.add(vm);
            }

            helperClass.readRandomFile();
//            helperClass.setAlgorithmType(helperClass.SALoadBalancing);
//            helperClass.setAlgorithmType(helperClass.HSLoadBalancing);
            helperClass.setAlgorithmType(helperClass.HybridSAHSLoadBalancing);

            broker.submitVmList(vmlist);

            cloudletList = new ArrayList<Cloudlet>();

            int id = 0;
            long length = 1;
            long fileSize = 300;
            long outputSize = 300;

            UtilizationModel utilizationModel = new UtilizationModelFull();
            int datasetCloudlet = helperClass.getNumOfCloudlets();

            for (int i = 0; i < datasetCloudlet; i++) {
                long randint = (long) getRandomValue.getCPUValue();

                Cloudlet cloudlet = new Cloudlet(id, length * randint, pesNumber, fileSize, outputSize, utilizationModel,
                        utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);

                cloudletList.add(cloudlet);
                id++;
            }

            broker.submitCloudletList(cloudletList);
            VmLoadBalancer.SubmitTask(cloudletList);

            List<Food> foodList = VmLoadBalancer.Initialization(vmlist, 300);
            VmLoadBalancer.RUN(foodList, broker);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList, foodList);

            Log.printLine("Simulation finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static Datacenter createDatacenter(String name, int hostId) {
        List<Host> hostList = new ArrayList<Host>();
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;
        int ram = 65536;
        long storage = 15000000;
        int bw = 10000000;

        for (int vmid = 1; vmid <= 4; vmid++) {
            peList.add(new Pe(vmid, new PeProvisionerSimple(mips * (vmid * 1))));
        }

        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));
        hostId++;
        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));
        hostId++;
        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList)));

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static void printCloudletList(List<Cloudlet> list, List<Food> foodList) {
        int size = list.size();
        double waitTime = 0;
        Cloudlet cloudlet;
        double finish_total = 0;
        double start_total = 0;
        double exec_total = 0;
        double makespan = 0;
        double load = 0;

        String indent = " ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + " "
                + "Time" + indent + "Start Time" + indent + "Finish Time" + indent + "Wait Time");

        DecimalFormat dft = new DecimalFormat("###.##");

        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                double tempLoad = cloudlet.getCloudletLength();
                double finishTime = cloudlet.getFinishTime();
                if (finishTime > makespan) {
                    makespan = finishTime;
                }
                if (load < tempLoad) {
                    load = tempLoad;
                }

                finish_total += cloudlet.getActualCPUTime() + cloudlet.getStartTime();//
                waitTime += cloudlet.getWaitTime();//
                start_total += cloudlet.getStartTime();//
                exec_total += cloudlet.getActualCPUTime();//

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + dft.format(cloudlet.getStartTime())//
                        + indent + dft.format(cloudlet.getStartTime() + cloudlet.getActualCPUTime()) + indent + dft.format(cloudlet.getWaitTime())); //
            }
        }

        Log.printLine("Imbalance Degree: " + VmLoadBalancer.imbalance(foodList));
        Log.printLine("Migrated Task: " + VmLoadBalancer.getMigratedTask());
        Log.printLine("Makespan: " + dft.format(makespan));
        Log.printLine("Avg Wait Time: " + dft.format(waitTime / size));
        Log.printLine("Avg Start Time: " + dft.format(start_total / size));
        Log.printLine("Avg Finish Time: " + dft.format(finish_total / size));
        Log.printLine("Avg Exec Time: " + dft.format(exec_total / size));
        Log.printLine("Load : " + dft.format(load));

        Log.printLine("========== TASK ==========");
        Log.printLine("Total Task: " + helperClass.getNumOfCloudlets());
        Log.printLine("==========================");
    }
}
