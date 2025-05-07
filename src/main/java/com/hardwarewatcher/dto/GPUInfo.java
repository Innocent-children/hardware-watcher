package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class GPUInfo {
    private String name;
    private String vendor;
    private String vram;
    private String usedVram;
    private double vramUsagePercentage;
    private double gpuLoad;
    private double temperature;
    private String driverVersion;
    private String displayMode;
    private String resolution;
    private long vramTotalBytes;
    private long vramUsedBytes;
}
