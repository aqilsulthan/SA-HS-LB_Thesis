package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HybridSAHSLB {

    private static final int HMS = 5;          // Harmony Memory Size
    private static final double HMCR = 0.9;    // Harmony Memory Consideration Rate
    private static final double PAR = 0.3;     // Pitch Adjusting Rate
    private static final double BW = 0.001;    // Bandwidth
    private static final int MAX_ITER = 1000;

    private static double T0 = 1000;           // Initial Temperature
    private static double alpha = 0.95;        // Cooling rate
    private static int L = 3;                  // Iterations per temperature level

    private static boolean isBalanced;

    public static boolean getStatus() {
        return isBalanced;
    }

    public static Food start(List<Food> foodList, Long task) {
        Random rand = new Random();

        List<Food> harmonyMemory = new ArrayList<>();
        for (int i = 0; i < HMS; i++) {
            Food vm = getRandomFood(foodList);
            double fitness = calculateFitness(vm, task);
            vm.setFitness(fitness);
            harmonyMemory.add(vm);
        }

        double T = T0;

        for (int iter = 0; iter < MAX_ITER; iter++) {
            // Step 1: Create new harmony
            Food newHarmony = new Food(harmonyMemory.get(rand.nextInt(HMS)));
            double newFitness;

            if (rand.nextDouble() < HMCR) {
                Food memoryVm = harmonyMemory.get(rand.nextInt(HMS));
                newHarmony = new Food(memoryVm);

                if (rand.nextDouble() < PAR) {
                    double adjustment = (rand.nextDouble() * 2 - 1) * BW;
                    double adjustedLoad = memoryVm.getTotalLoad() + (task * adjustment);
                    newFitness = adjustedLoad / memoryVm.getCapacity();
                } else {
                    newFitness = (memoryVm.getTotalLoad() + task) / memoryVm.getCapacity();
                }
            } else {
                newHarmony = getRandomFood(foodList);
                newFitness = calculateFitness(newHarmony, task);
            }

            newHarmony.setFitness(newFitness);

            // Step 2: Decide whether to replace worst harmony
            int worstIdx = getWorstIndex(harmonyMemory);
            double delta = newFitness - harmonyMemory.get(worstIdx).getFitness();

            if (delta < 0 || rand.nextDouble() < Math.exp(-delta / T)) {
                harmonyMemory.set(worstIdx, newHarmony);
            }

            // Step 3: Cooling
            if ((iter + 1) % L == 0) {
                T *= alpha;
            }
        }

        Food best = harmonyMemory.get(0);
        for (Food vm : harmonyMemory) {
            if (vm.getFitness() < best.getFitness()) {
                best = vm;
            }
        }

        best.setFitness(calculateFitness(best, task));

        double avg = VmLoadBalancer.calculateAverageProcessingTime(foodList);
        double sd = VmLoadBalancer.calculateStandardDeviation(foodList, avg);
        isBalanced = sd <= avg;

        return best;
    }

    private static Food getRandomFood(List<Food> foodList) {
        Random rand = new Random();
        return foodList.get(rand.nextInt(foodList.size()));
    }

    private static int getWorstIndex(List<Food> list) {
        int idx = 0;
        double maxFitness = list.get(0).getFitness();
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).getFitness() > maxFitness) {
                maxFitness = list.get(i).getFitness();
                idx = i;
            }
        }
        return idx;
    }

    private static double calculateFitness(Food vm, Long task) {
        return (vm.getTotalLoad() + task) / vm.getCapacity();
    }
}
