package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.helperClass;
import org.cloudbus.cloudsim.core.CloudSim;

public class VmLoadBalancer {

    private static List<Cloudlet> cloudletList = new ArrayList<>(); // List to hold cloudlets
    private static int migratedTask = 0; // Variable to track migrated tasks

    public static void SubmitTask(List<Cloudlet> list) {
        cloudletList = list;
    }

    public static void RUN(List<Food> foodList, DatacenterBroker broker) {
        int i = 1;

        Log.printLine("============== Assigning Task ================");
        for (int num = 0; num < foodList.size(); num++) {
            Food selectedVm = foodList.get(num);
            double load = cloudletList.get(num).getCloudletLength();

            Log.print("VM-" + selectedVm.getId() + " => ");
            Log.printLine(" Load: " + load);

            selectedVm.setCurrentLoad(load);
            selectedVm.setTotalLoad(load);
            broker.bindCloudletToVm(cloudletList.get(num).getCloudletId(), foodList.get(num).getId());
        }

        // ============== Remaining Task Available (Load Balance) =================
        for (int num = foodList.size(); num < cloudletList.size(); num++) {
            Log.printLine("============== Iterasi ke-" + i + " =================");
            Log.printLine("[LoadInfo] : Incoming Load => " + cloudletList.get(num).getCloudletLength());
            int vmDestId = Start(foodList, num, broker);

            if (vmDestId != -1) {
                Food vmDest = foodList.get(vmDestId);
                double waitTime = vmDest.getCurrentLoad() / vmDest.getMips();
                double clockTime = vmDest.getTime();
                double startTime = clockTime + waitTime;
                Cloudlet task = cloudletList.get(num);

                cloudletList.get(num).setStartTime(startTime);
                cloudletList.get(num).setWaitTime(waitTime);

                vmDest.setCurrentLoad(task.getCloudletLength());
                vmDest.setTotalLoad(task.getCloudletLength());

                Log.printLine("[LoadInfo] : Update Load => " + vmDest.getCurrentLoad());

                vmDest.setTime(startTime);

                for (Food vm : foodList) {
                    if (vm.getId() != vmDestId) {
                        double outgoingLoad = vm.getMips() * waitTime;
                        double updateLoad = vm.getCurrentLoad() - outgoingLoad;
                        if (updateLoad < 0) updateLoad = 0;
                        vm.setCurrentLoad(updateLoad);
                        vm.setTime(startTime);
                        Log.printLine("[LoadUpdate] : Update VM => " + vm.getId());
                        Log.printLine("[LoadUpdate] : Updated Load => " + vm.getCurrentLoad());
                    }
                }

                Log.printLine("");
                broker.bindCloudletToVm(task.getCloudletId(), vmDestId);
            } else {
                Log.printLine("");
            }
            i++;
        }
    }

    public static int Start(List<Food> foodList, int index, DatacenterBroker broker) {
        Long task = cloudletList.get(index).getCloudletLength();
        int vmDestId = -1;
        boolean initialBalanceStatus;
        
        // Menghitung status keseimbangan sebelum melakukan penugasan
        double initialAvgProcessingTime = calculateAverageProcessingTime(foodList);
        double initialSD = calculateStandardDeviation(foodList, initialAvgProcessingTime);
        initialBalanceStatus = initialSD <= initialAvgProcessingTime;
        
        // Menugaskan task ke VM berdasarkan algoritma yang dipilih
        if (helperClass.getAlgorithmType() == helperClass.SALoadBalancing) {
            Food vmDest = SimulatedAnnealingLB.start(foodList, task);
            vmDestId = vmDest.getId();
        }
        if (helperClass.getAlgorithmType() == helperClass.HSLoadBalancing) {
            Food vmDest = HarmonySearchLB.start(foodList, task);
            vmDestId = vmDest.getId();
        }
        if (helperClass.getAlgorithmType() == helperClass.HybridSAHSLoadBalancing) {
            Food vmDest = HybridSAHSLB.start(foodList, task);
            vmDestId = vmDest.getId();
        }

        // Menghitung status keseimbangan setelah penugasan
        double avgProcessingTime = calculateAverageProcessingTime(foodList);
        double sd = calculateStandardDeviation(foodList, avgProcessingTime);
        boolean isBalanced = sd <= avgProcessingTime;

        System.out.println("[LoadResults] : " + "Sd: " + sd + " avgtime: " + avgProcessingTime);

        // Jika status berubah dari tidak seimbang ke seimbang, berarti migrasi berhasil
        if (!initialBalanceStatus && isBalanced) {
            migratedTask++;
            System.out.println("[LoadStatus] : Task migrated successfully. Total migrations: " + migratedTask);
        }

        // Jika masih tidak seimbang, coba proses migrasi
        if (!isBalanced) {
            System.out.println("[LoadStatus] : System is in an imbalance condition.");
            // Remove current load in VM if imbalance
            foodList.get(vmDestId).removeCurrentLoad(cloudletList.get(index).getCloudletLength());
            
            boolean migrationSuccessful = false;
            
            if (helperClass.getAlgorithmType() == helperClass.SALoadBalancing) {
                System.out.println("Starting Load Balancing.....");
                boolean saStatus = SimulatedAnnealingLB.getStatus();
                if (!saStatus) {
                    System.out.println("[LoadStatus] : System is in an imbalance condition.");
                    System.out.println("[LoadStatus] : Load difference too big");
                    System.out.println("[LoadStatus] : Assigning task to selected VM");
                    Food vmDest = SimulatedAnnealingLB.start(foodList, task);
                    migratedTask++;
                    System.out.println("[LoadStatus] : Task migrated successfully. Total migrations: " + migratedTask);
                    return vmDest.getId();
                } else {
                    System.out.println("[LoadStatus] : System is in a balanced condition.");
                    migrationSuccessful = true;
                }
            }
            
            if (helperClass.getAlgorithmType() == helperClass.HSLoadBalancing) {
                System.out.println("Starting Load Balancing.....");
                boolean hsStatus = HarmonySearchLB.getStatus();
                if (!hsStatus) {
                    System.out.println("[LoadStatus] : System is in an imbalance condition.");
                    System.out.println("[LoadStatus] : Load difference too big");
                    System.out.println("[LoadStatus] : Assigning task to selected VM");
                    Food vmDest = HarmonySearchLB.start(foodList, task);
                    migratedTask++;
                    System.out.println("[LoadStatus] : Task migrated successfully. Total migrations: " + migratedTask);
                    return vmDest.getId();
                } else {
                    System.out.println("[LoadStatus] : System is in a balanced condition.");
                    migrationSuccessful = true;
                }
            }
            
            if (helperClass.getAlgorithmType() == helperClass.HybridSAHSLoadBalancing) {
                boolean hsSaStatus = HybridSAHSLB.getStatus();
                if (!hsSaStatus) {
                    System.out.println("[LoadStatus] : System is in an imbalance condition.");
                    System.out.println("[LoadStatus] : Assigning task to selected VM");
                    Food vmDest = HybridSAHSLB.start(foodList, task);
                    migratedTask++;
                    System.out.println("[LoadStatus] : Task migrated successfully. Total migrations: " + migratedTask);
                    return vmDest.getId();
                } else {
                    System.out.println("[LoadStatus] : System is in a balanced condition.");
                }
            }
            
            // Jika migrasi berhasil mengubah status menjadi seimbang
            if (migrationSuccessful) {
                migratedTask++;
                System.out.println("[LoadStatus] : Task migrated successfully. Total migrations: " + migratedTask);
            }
        } else {
            System.out.println("[LoadStatus] : System is in a balanced condition.");
        }

        return vmDestId;
    }

    public static List<Food> Initialization(List<Vm> vmList, int bees) {
        Random random = new Random();
        List<Food> foodList = new ArrayList<>();

        for (int i = 0; i < vmList.size(); i++) {
            Vm selectedVm = vmList.get(i);
            Food food = new Food(selectedVm);
            foodList.add(food);
        }

        Log.printLine("============== VM Lists ================");
        for (int i = 0; i < bees; i++) {
            int randomIndex = random.nextInt(vmList.size());
            Food selectedVm = foodList.get(randomIndex);
            double capacity = selectedVm.getNumberOfPes() * selectedVm.getMips() + selectedVm.getBw();
            selectedVm.setCapacity(capacity);
        }

        for (Food vm : foodList) {
            Log.print("VM-" + vm.getId() + " => ");
            Log.print("PE: " + vm.getNumberOfPes() + " MIPS: " + vm.getMips() + " BW: " + vm.getBw());
            Log.printLine(" Capacity: " + vm.getCapacity());
        }

        return foodList;
    }

    public static double calculateAverageProcessingTime(List<Food> foodList) {
        double totalProcessingTime = 0;

        for (Food food : foodList) {
            double ProcessingTime = food.getTotalLoad() / food.getCapacity();
            food.setTimeProcessing(ProcessingTime);

            totalProcessingTime += ProcessingTime;
        }
        return totalProcessingTime / foodList.size();
    }

    public static double calculateStandardDeviation(List<Food> foodList, double avgProcessingTime) {
        double variance = foodList.stream()
                .mapToDouble(f -> Math.pow(f.getTimeProcessing() - avgProcessingTime, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    public static double imbalance(List<Food> foodList) {
        if (foodList == null || foodList.isEmpty()) {
            return 0; // Avoid division by zero if the list is empty or null
        }

        double maxTime = Double.MIN_VALUE;
        double minTime = Double.MAX_VALUE;
        double sumTime = 0;
        int count = 0;

        // Iterate through each Food object to find max, min, and average times
        for (Food vm : foodList) {
            double time = vm.getTimeProcessing();
            if (time > maxTime) {
                maxTime = time; // Update max time
            }
            if (time < minTime) {
                minTime = time; // Update min time
            }
            sumTime += time; // Sum up all times for average calculation later
            count++;
        }

        double avgTime = sumTime / count;
        DecimalFormat dft = new DecimalFormat("###.##");

        return (maxTime - minTime) / avgTime;
    }
    
    // Method to get the total number of migrated tasks
    public static int getMigratedTask() {
        return migratedTask;
    }
    
    // Reset the counter (might be useful for testing)
    public static void resetMigratedTaskCount() {
        migratedTask = 0;
    }
}