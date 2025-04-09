package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Log;

public class HoneyBeeLB {

    private static boolean isBalanced;
    private static int employeeBee = 30;
    private static final int maxIterations = 1000; // Maximum number of iterations

    public static boolean getStatus() {
        return isBalanced;
    }

    public static Food start(List<Food> foodList, Long task) {
        // Perform HBB Algo
        Food vmDest = null;

        for (int iter = 0; iter < maxIterations; iter++) {
            employeePhase(foodList, employeeBee);
            vmDest = onlookerPhase(foodList, 50, 30, task);
            if (vmDest != null) {
                break;
            }
        }

        double avgProcessingTime = VmLoadBalancer.calculateAverageProcessingTime(foodList);
        double sd = VmLoadBalancer.calculateStandardDeviation(foodList, avgProcessingTime);

        isBalanced = sd <= avgProcessingTime;
        Log.printLine("[LoadInfo] : Assign Load " + task + " To VM => " + vmDest.getId());
        return vmDest;
    }

    public static List<Food> employeePhase(List<Food> foodList, int bees) {
        Random random = new Random();
        for (int i = 0; i < bees; i++) {
            int randomIndex = random.nextInt(foodList.size());
            Food selectedVm = foodList.get(randomIndex);
            double fitness = selectedVm.getCurrentLoad() / selectedVm.getCapacity();
            selectedVm.setFitness(fitness);
        }
        return foodList;
    }

    public static Food onlookerPhase(List<Food> foodList, int nsp, int nep, Long task) {
        List<Food> topFoodSources = new ArrayList<>();

        for (Food vm : foodList) {
            if (topFoodSources.size() < 3) {
                topFoodSources.add(vm);
                topFoodSources.sort((f1, f2) -> Double.compare(f2.getFitness(), f1.getFitness()));
            } else {
                if (vm.getFitness() > topFoodSources.get(2).getFitness()) {
                    topFoodSources.set(2, vm); // Replace with the new vm if it has higher fitness
                    topFoodSources.sort((f1, f2) -> Double.compare(f2.getFitness(), f1.getFitness()));
                }
            }
        }

        for (int i = 0; i < nsp; i++) {
            for (Food vm : topFoodSources) {
                performNeighborhoodSearch(vm, task);
            }
        }

        for (int i = 0; i < nep; i++) {
            for (Food vm : foodList) {
                performNeighborhoodSearch(vm, task);
            }
        }

        Food selectedVm = foodList.get(0); // Start with the first VM as having the highest fitness

        for (Food vm : foodList) {
            if (vm.getFitness() < selectedVm.getFitness()) {
                selectedVm = vm;
            }
        }
        return selectedVm;
    }

    public static void performNeighborhoodSearch(Food vm, Long task) {
        double newFitness = calculateNewFitness(vm, task);
        vm.setFitness(newFitness);
    }

    public static double calculateNewFitness(Food vm, Long task) {
        return (vm.getTotalLoad() + task) / vm.getCapacity();
    }
}
