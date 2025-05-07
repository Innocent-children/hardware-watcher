package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class MemoryInfo {
    private String totalMemory;
    private String usedMemory;
    private String availableMemory;
    private double memoryUsagePercentage;
    private String swapTotal;
    private String swapUsed;
    private double swapUsagePercentage;
    private long memoryUsedBytes;
    private long memoryTotalBytes;
    private long swapUsedBytes;
    private long swapTotalBytes;
}
