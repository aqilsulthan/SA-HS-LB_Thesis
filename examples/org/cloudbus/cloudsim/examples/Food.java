package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Vm;

public class Food extends Vm {
    private double capacity;
    private double timeProcessing;
    private double fitness;   // Used by Harmony Search & HS-SA
    private double cost;      // Used by Simulated Annealing
    private double currentLoad = 0;
    private double totalLoad = 0;
    private double clockTime = 0.1;

    public Food(Vm vm) {
        super(vm.getId(), vm.getUserId(), vm.getMips(), vm.getNumberOfPes(),
              vm.getRam(), vm.getBw(), vm.getSize(), vm.getVmm(), vm.getCloudletScheduler());
    }

    // Setters
    public void setTime(double time) {
        this.clockTime = time;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public void setTimeProcessing(double timeProcessing) {
        this.timeProcessing = timeProcessing;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setCurrentLoad(double load) {
        this.currentLoad = load;
    }

    public void setTotalLoad(double load) {
        this.totalLoad += load;
    }

    public void removeCurrentLoad(double load) {
        this.currentLoad -= load;
    }

    // Getters
    public double getCapacity() {
        return this.capacity;
    }

    public double getCurrentLoad() {
        return this.currentLoad;
    }

    public double getTotalLoad() {
        return this.totalLoad;
    }

    public double getTime() {
        return this.clockTime;
    }

    public double getFitness() {
        return this.fitness;
    }

    public double getCost() {
        return this.cost;
    }

    public double getTimeProcessing() {
        return this.timeProcessing;
    }
}
