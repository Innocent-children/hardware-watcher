package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class StorageInfo {
    private String name;
    private String model;
    private String serial;
    private String type;
    private String totalSpace;
    private String usedSpace;
    private String freeSpace;
    private double usagePercentage;
    private long readBytes;
    private long writeBytes;
    private long totalBytes;
    private long usedBytes;
    private long freeBytes;
    private double transferRate;
    private String temperature;
}
