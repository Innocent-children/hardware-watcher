package com.hardwarewatcher.dto;

import lombok.Data;

import java.util.List;

@Data
public class HardwareInfo {
    private CPUInfo CPUInfo;
    private MemoryInfo memoryInfo;
    private List<GPUInfo> gpuInfoList;
    private List<StorageInfo> storageInfoList;
    private List<NetworkInfo> networkInfoList;
    private String systemName;
    private String systemManufacturer;
    private String systemModel;
    private String osName;
    private String osVersion;
    private String uptime;
}
