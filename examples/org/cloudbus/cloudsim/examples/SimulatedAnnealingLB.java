package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Random;

public class SimulatedAnnealingLB {

    private static boolean isBalanced = false;
    private static double T = 1000;          // Initial temperature
    private static double alpha = 0.95;      // Cooling rate
    private static int L = 10;               // Iterations per temperature level

    public static boolean getStatus() {
        return isBalanced;
    }

    public static Food start(List<Food> foodList, Long task) {
        Random rand = new Random();

        Food current = getRandomFood(foodList);
        double currentCost = cost(current, task);
        current.setCost(currentCost);

        Food best = current;
        double bestCost = currentCost;

        T = 1000;

        while (T > 1e-3) {
            for (int i = 0; i < L; i++) {
                Food neighbor = getRandomFood(foodList);
                double neighborCost = cost(neighbor, task);
                neighbor.setCost(neighborCost);

                double delta = neighborCost - currentCost;

                if (delta < 0 || Math.exp(-delta / T) > rand.nextDouble()) {
                    current = neighbor;
                    currentCost = neighborCost;

                    if (currentCost < bestCost) {
                        best = current;
                        bestCost = currentCost;
                    }
                }
            }
            T *= alpha;
        }

        best.setCost(bestCost);

        double avgTime = VmLoadBalancer.calculateAverageProcessingTime(foodList);
        double sd = VmLoadBalancer.calculateStandardDeviation(foodList, avgTime);
        isBalanced = sd <= avgTime;

        return best;
    }

    private static double cost(Food vm, Long task) {
        return (vm.getTotalLoad() + task) / vm.getCapacity(); // Cost function (lower is better)
    }

    private static Food getRandomFood(List<Food> foodList) {
        Random rand = new Random();
        return foodList.get(rand.nextInt(foodList.size()));
    }
}
