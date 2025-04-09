package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Log;

public class AntColonyLB {

    private static final Random random = new Random();
    private static boolean isBalanced;
    private static double[] pheromones;
    private static int ants = 54;
    private static final double alpha = 3.0;
    private static final double beta = 2.0;
    private static final double gamma = 8.0;
    private static double Tp = Double.MAX_VALUE;

    public static boolean getStatus() {
        return isBalanced;
    }

    public static Food start(List<Food> foodList, Long task) {
        Food vmDest = foodList.get(0);

        // Perform ACO Algorithm
        initializePheromones(foodList);
        vmDest = findSolution(foodList, task);

        double avgProcessingTime = VmLoadBalancer.calculateAverageProcessingTime(foodList);
        double sd = VmLoadBalancer.calculateStandardDeviation(foodList, avgProcessingTime);

        isBalanced = sd <= avgProcessingTime;
        Log.printLine("[LoadInfo] : Assign Load " + task + " To VM => " + vmDest.getId());
        return vmDest;
    }

    public static void initializePheromones(List<Food> foodList) {
        pheromones = new double[foodList.size()];
        for (int i = 0; i < foodList.size(); i++) {
            pheromones[i] = foodList.get(i).getNumberOfPes() * foodList.get(i).getMips() + foodList.get(i).getBw(); // Simplified initialization
        }
    }

    public static Food findSolution(List<Food> foodList, Long task) {
        double[] probabilities = new double[foodList.size()];
        Food bestVm = null;
        double bestScore = Double.MIN_VALUE;
        double currentBestPerformance = Double.MAX_VALUE; // Set initial performance as very high

        for (int a = 0; a < ants; a++) {
            int currentVm = random.nextInt(foodList.size()); // Random start point for each ant
            for (int step = 0; step < foodList.size(); step++) {
                double totalProbability = calculateTotalProbability(foodList, currentVm, task);
                for (int i = 0; i < foodList.size(); i++) {
                    probabilities[i] = calculateProbability(foodList, currentVm, i, totalProbability, task);
                    double currentPerformance = calculatePerformance(foodList.get(i), task); // Calculate performance for VM

                    // Check and update the best VM and its score based on probability and performance
                    if (probabilities[i] > bestScore || (probabilities[i] == bestScore && currentPerformance < currentBestPerformance)) {
                        bestScore = probabilities[i];
                        bestVm = foodList.get(i);
                        currentBestPerformance = currentPerformance;
                    }
                }
            }

            // Update Tp to the current best performance found
            if (currentBestPerformance < Tp) {
                Tp = currentBestPerformance;
            }

            // Local pheromone update (Equation 8) using updated Tp
            pheromones[currentVm] = (1 - 0.5) * pheromones[currentVm] + 1.0 / Tp;
        }

        // Global pheromone update (Equation 9) using updated Tp for bestVm found
        for (int i = 0; i < pheromones.length; i++) {
            if (foodList.get(i).equals(bestVm)) { // Check if this is the bestVm found
                pheromones[i] = (1 - 0.5) * pheromones[i] + 0.1 / Tp;
            }
        }

        return bestVm;
    }

    private static double calculatePerformance(Food vm, long taskSize) {
        double availableMips = vm.getNumberOfPes() * vm.getMips();

        if (availableMips == 0) return Double.MAX_VALUE;

        double expectedTime = taskSize / availableMips;
        double dataTransferTime = taskSize / vm.getBw();
        double totalTime = expectedTime + dataTransferTime;
        return totalTime;
    }

    private static double calculateTotalProbability(List<Food> foodList, int currentVm, long task) {
        double sum = 0;
        for (Food vm : foodList) {
            double ev = vm.getNumberOfPes() * vm.getMips() + vm.getBw();
            double fitt = calculateFitness(foodList, vm, task);
            sum += Math.pow(pheromones[currentVm], alpha) * Math.pow(ev, beta) * Math.pow(fitt, gamma);
        }
        return sum;
    }

    private static double calculateProbability(List<Food> foodList, int currentVm, int vmIndex, double totalProbability, long task) {
        double ev = foodList.get(vmIndex).getNumberOfPes() * foodList.get(vmIndex).getMips() + foodList.get(vmIndex).getBw();
        double fitt = calculateFitness(foodList, foodList.get(vmIndex), task);
        double prob = (Math.pow(pheromones[currentVm], alpha) * Math.pow(ev, beta) * Math.pow(fitt, gamma)) / totalProbability;

        return prob;
    }

    private static double calculateFitness(List<Food> foodList, Food vm, long task) {
        double vmExecTime = vm.getCurrentLoad() / vm.getCapacity() + task / vm.getBw();
        double avgExecTime = calculateAverageProcessingTime(foodList);

        return 1.0 - (vmExecTime - avgExecTime) / (vmExecTime + avgExecTime); // Randomly simplified
    }

    public static double calculateAverageProcessingTime(List<Food> foodList) {
        double totalProcessingTime = 0;

        for (Food food : foodList) {
            double ProcessingTime = food.getCurrentLoad() / food.getCapacity();
            food.setTimeProcessing(ProcessingTime);

            totalProcessingTime += ProcessingTime;
        }
        return totalProcessingTime / foodList.size();
    }
}
