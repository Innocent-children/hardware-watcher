package com.hardwarewatcher.dto;

import lombok.Data;

import java.util.List;

@Data
public class HardwareInfo {
    private CPUInfo cpuInfo;
    private List<FanInfo> fanInfoList; // 添加风扇信息列表
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
