package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class GPUInfo {
    private String name;
    private String vendor; // Added
    private String deviceId; // Added
    private String vendor;
    private String vram;
    private String usedVram;
    private double vramUsagePercentage;
    private double gpuLoad;
    private double temperature;
    private String driverVersion;
    private String displayMode;
    // private String resolution; // Commenting out as not directly available from OSHI GraphicsCard
    private long vramTotalBytes;
    private long vramUsedBytes;
}
