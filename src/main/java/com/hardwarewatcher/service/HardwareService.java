package com.hardwarewatcher.service;

import com.hardwarewatcher.dto.*;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HardwareService {

    private final SystemInfo systemInfo = new SystemInfo();
    private long[] prevTicks = new long[CentralProcessor.TickType.values().length];
    private long[] prevNetworkBytesReceived;
    private long[] prevNetworkBytesSent;

    public HardwareInfo getHardwareInfo() {
        HardwareInfo hardwareInfo = new HardwareInfo();

        hardwareInfo.setCPUInfo(getCPUInfo());
        hardwareInfo.setMemoryInfo(getMemoryInfo());
        hardwareInfo.setGpuInfoList(getGpuInfo());
        hardwareInfo.setStorageInfoList(getStorageInfo());
        hardwareInfo.setNetworkInfoList(getNetworkInfo());

        OperatingSystem os = systemInfo.getOperatingSystem();
        hardwareInfo.setSystemName(os.getFamily());
        hardwareInfo.setSystemManufacturer(systemInfo.getHardware().getComputerSystem().getManufacturer());
        hardwareInfo.setSystemModel(systemInfo.getHardware().getComputerSystem().getModel());
        hardwareInfo.setOsName(os.toString());
        hardwareInfo.setOsVersion(os.getVersionInfo().getVersion());
        hardwareInfo.setUptime(FormatUtil.formatElapsedSecs(os.getSystemUptime()));

        return hardwareInfo;
    }

    private CPUInfo getCPUInfo() {
        CPUInfo cpuInfo = new CPUInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        cpuInfo.setName(processor.getProcessorIdentifier().getName());
        cpuInfo.setVendor(processor.getProcessorIdentifier().getVendor());
        cpuInfo.setPhysicalCores(processor.getPhysicalProcessorCount());
        cpuInfo.setLogicalCores(processor.getLogicalProcessorCount());
        cpuInfo.setSystemLoad(processor.getSystemLoadAverage(1)[0] / 100);
        cpuInfo.setCpuLoad(processor.getSystemCpuLoad(1000));

        // Core loads
        double[] load = processor.getProcessorCpuLoad(1000);
        List<Double> coreLoads = new ArrayList<>();
        for (double coreLoad : load) {
            coreLoads.add(coreLoad * 100);
        }
        cpuInfo.setCoreLoads(coreLoads);

        // Frequency - 修正这部分
        long[] currentFreq = processor.getCurrentFreq();
        if (currentFreq != null && currentFreq.length > 0) {
            // 使用第一个核心的频率作为当前频率
            cpuInfo.setCurrentFreq(currentFreq[0] > 0 ? FormatUtil.formatHertz(currentFreq[0]) : "N/A");
        } else {
            cpuInfo.setCurrentFreq("N/A");
        }

        // 最大频率
        cpuInfo.setMaxFreq(FormatUtil.formatHertz(processor.getMaxFreq()));

        // Temperature (if available)
        Sensors sensors = systemInfo.getHardware().getSensors();
        cpuInfo.setTemperature(sensors.getCpuTemperature());

        // Additional info
        cpuInfo.setArchitecture(processor.getProcessorIdentifier().getMicroarchitecture());
        cpuInfo.setProcessorId(processor.getProcessorIdentifier().getProcessorID());

        // Context switches and interrupts
        long[] ticks = processor.getSystemCpuLoadTicks();
        cpuInfo.setContextSwitches(ticks[CentralProcessor.TickType.IRQ.getIndex()]);
        cpuInfo.setInterrupts(ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]);

        return cpuInfo;
    }

    private MemoryInfo getMemoryInfo() {
        MemoryInfo memoryInfo = new MemoryInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();

        memoryInfo.setTotalMemory(FormatUtil.formatBytes(memory.getTotal()));
        memoryInfo.setUsedMemory(FormatUtil.formatBytes(memory.getTotal() - memory.getAvailable()));
        memoryInfo.setAvailableMemory(FormatUtil.formatBytes(memory.getAvailable()));
        memoryInfo.setMemoryUsagePercentage((memory.getTotal() - memory.getAvailable()) * 100.0 / memory.getTotal());

        memoryInfo.setMemoryUsedBytes(memory.getTotal() - memory.getAvailable());
        memoryInfo.setMemoryTotalBytes(memory.getTotal());

        // Swap memory
        memoryInfo.setSwapTotal(FormatUtil.formatBytes(memory.getVirtualMemory().getSwapTotal()));
        memoryInfo.setSwapUsed(FormatUtil.formatBytes(memory.getVirtualMemory().getSwapUsed()));
        memoryInfo.setSwapUsagePercentage(memory.getVirtualMemory().getSwapUsed() * 100.0 / memory.getVirtualMemory().getSwapTotal());

        memoryInfo.setSwapUsedBytes(memory.getVirtualMemory().getSwapUsed());
        memoryInfo.setSwapTotalBytes(memory.getVirtualMemory().getSwapTotal());

        return memoryInfo;
    }

    private List<GPUInfo> getGpuInfo() {
        List<GPUInfo> gpuInfos = new ArrayList<>();

        try {
            // 尝试获取NVIDIA GPU信息
            ProcessBuilder nvidiaProcessBuilder = new ProcessBuilder(
                    "nvidia-smi",
                    "--query-gpu=name,memory.total,memory.used,utilization.gpu,temperature.gpu,driver_version,display_mode",
                    "--format=csv,noheader,nounits"
            );
            Process nvidiaProcess = nvidiaProcessBuilder.start();
            BufferedReader nvidiaReader = new BufferedReader(new InputStreamReader(nvidiaProcess.getInputStream()));

            String nvidiaLine;
            while ((nvidiaLine = nvidiaReader.readLine()) != null) {
                String[] parts = nvidiaLine.split(", ");
                if (parts.length >= 7) {
                    GPUInfo info = new GPUInfo();
                    info.setName(parts[0].trim());
                    info.setVram(parts[1].trim() + " MB");
                    info.setUsedVram(parts[2].trim() + " MB");

                    // 计算显存使用百分比
                    double total = Double.parseDouble(parts[1].trim());
                    double used = Double.parseDouble(parts[2].trim());
                    info.setVramUsagePercentage((used / total) * 100);

                    info.setGpuLoad(Double.parseDouble(parts[3].trim()));
                    info.setTemperature(Double.parseDouble(parts[4].trim()));
                    info.setDriverVersion(parts[5].trim());
                    info.setDisplayMode(parts[6].trim());

                    // 显存字节数
                    info.setVramTotalBytes((long) (total * 1024 * 1024));
                    info.setVramUsedBytes((long) (used * 1024 * 1024));

                    gpuInfos.add(info);
                }
            }
            nvidiaReader.close();
        } catch (IOException e) {
            System.err.println("Failed to read NVIDIA GPU info: " + e.getMessage());
        }

        // 如果没有NVIDIA GPU信息，尝试获取其他GPU的温度（通过sensors）
        if (gpuInfos.isEmpty()) {
            try {
                ProcessBuilder sensorsProcessBuilder = new ProcessBuilder("sensors");
                Process sensorsProcess = sensorsProcessBuilder.start();
                BufferedReader sensorsReader = new BufferedReader(new InputStreamReader(sensorsProcess.getInputStream()));

                String sensorsLine;
                Pattern gpuTempPattern = Pattern.compile("(amdgpu|nouveau).*\\+(\\d+\\.\\d+)°C");

                while ((sensorsLine = sensorsReader.readLine()) != null) {
                    Matcher matcher = gpuTempPattern.matcher(sensorsLine);
                    if (matcher.find()) {
                        GPUInfo info = new GPUInfo();
                        info.setName("AMD/Intel GPU");
                        info.setTemperature(Double.parseDouble(matcher.group(2)));
                        gpuInfos.add(info);
                        break; // 假设只有一个GPU
                    }
                }
                sensorsReader.close();
            } catch (IOException e) {
                System.err.println("Failed to read GPU temperature from sensors: " + e.getMessage());
            }
        }

        return gpuInfos;
    }

    private List<StorageInfo> getStorageInfo() {
        List<StorageInfo> storageInfoList = new ArrayList<>();
        List<OSFileStore> fileStores = systemInfo.getOperatingSystem().getFileSystem().getFileStores();

        for (OSFileStore fs : fileStores) {
            StorageInfo storageInfo = new StorageInfo();

            storageInfo.setName(fs.getName());
            storageInfo.setModel(fs.getLabel());
            storageInfo.setType(fs.getType());
            storageInfo.setTotalSpace(FormatUtil.formatBytes(fs.getTotalSpace()));
            storageInfo.setUsedSpace(FormatUtil.formatBytes(fs.getTotalSpace() - fs.getFreeSpace()));
            storageInfo.setFreeSpace(FormatUtil.formatBytes(fs.getFreeSpace()));
            storageInfo.setUsagePercentage((fs.getTotalSpace() - fs.getFreeSpace()) * 100.0 / fs.getTotalSpace());

            storageInfo.setTotalBytes(fs.getTotalSpace());
            storageInfo.setUsedBytes(fs.getTotalSpace() - fs.getFreeSpace());
            storageInfo.setFreeBytes(fs.getFreeSpace());

            // These would require additional monitoring over time
            storageInfo.setReadBytes(0);
            storageInfo.setWriteBytes(0);
            storageInfo.setTransferRate(0);
            storageInfo.setTemperature("N/A");

            storageInfoList.add(storageInfo);
        }

        return storageInfoList;
    }

    private List<NetworkInfo> getNetworkInfo() {
        List<NetworkInfo> networkInfoList = new ArrayList<>();
        NetworkIF[] networkIFs = systemInfo.getHardware().getNetworkIFs().toArray(new NetworkIF[0]);

        if (prevNetworkBytesReceived == null || prevNetworkBytesSent == null) {
            prevNetworkBytesReceived = new long[networkIFs.length];
            prevNetworkBytesSent = new long[networkIFs.length];
            for (int i = 0; i < networkIFs.length; i++) {
                prevNetworkBytesReceived[i] = networkIFs[i].getBytesRecv();
                prevNetworkBytesSent[i] = networkIFs[i].getBytesSent();
            }
            Util.sleep(1000); // Wait a second to calculate speeds
        }

        networkIFs = systemInfo.getHardware().getNetworkIFs().toArray(new NetworkIF[0]);

        for (int i = 0; i < networkIFs.length; i++) {
            NetworkIF net = networkIFs[i];
            NetworkInfo networkInfo = new NetworkInfo();

            networkInfo.setName(net.getName());
            networkInfo.setDisplayName(net.getDisplayName());
            networkInfo.setMacAddress(net.getMacaddr());
            networkInfo.setIpv4(String.join(", ", net.getIPv4addr()));
            networkInfo.setIpv6(String.join(", ", net.getIPv6addr()));

            long bytesReceived = net.getBytesRecv();
            long bytesSent = net.getBytesSent();

            networkInfo.setDataSent(FormatUtil.formatBytes(bytesSent));
            networkInfo.setDataReceived(FormatUtil.formatBytes(bytesReceived));
            networkInfo.setBytesSent(bytesSent);
            networkInfo.setBytesReceived(bytesReceived);

            // Calculate speeds (bytes per second)
            long deltaRecv = bytesReceived - prevNetworkBytesReceived[i];
            long deltaSent = bytesSent - prevNetworkBytesSent[i];

            networkInfo.setDownloadSpeed(deltaRecv / 1024.0); // KB/s
            networkInfo.setUploadSpeed(deltaSent / 1024.0);  // KB/s

            prevNetworkBytesReceived[i] = bytesReceived;
            prevNetworkBytesSent[i] = bytesSent;

            networkInfo.setPacketsSent(String.valueOf(net.getPacketsSent()));
            networkInfo.setPacketsReceived(String.valueOf(net.getPacketsRecv()));
            networkInfo.setSpeed(net.getSpeed() > 0 ? FormatUtil.formatValue(net.getSpeed(), "bps") : "Unknown");
            networkInfo.setMtu(String.valueOf(net.getMTU()));
            networkInfo.setConnected(!(net.getIPv4addr().length == 0) || !(net.getIPv6addr().length == 0));

            networkInfoList.add(networkInfo);
        }

        return networkInfoList;
    }
}
