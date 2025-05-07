package com.hardwarewatcher.dto;

import lombok.Data;

import java.util.List;

@Data
public class CPUInfo {
    private String name;
    private String vendor;
    private int physicalCores;
    private int logicalCores;
    private double systemLoad;
    private double cpuLoad;
    private List<Double> coreLoads;
    private double temperature;
    private String currentFreq;
    private String maxFreq;
    private List<Double> coreTemperatures;
    private String architecture;
    private String processorId;
    private long contextSwitches;
    private long interrupts;
    private List<String> coreFrequencies;
}
