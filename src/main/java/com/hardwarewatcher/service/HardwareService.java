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

        hardwareInfo.setCpuInfo(getCPUInfo());
        hardwareInfo.setMemoryInfo(getMemoryInfo());
        hardwareInfo.setGpuInfoList(getGpuInfo());
        hardwareInfo.setStorageInfoList(getStorageInfo());
        hardwareInfo.setNetworkInfoList(getNetworkInfo());
        hardwareInfo.setFanInfoList(getFanInfo()); // 获取风扇信息

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

        long[] currentFreq = processor.getCurrentFreq();
        List<String> coreFreqList = new ArrayList<>();
        for (long freq : currentFreq) {
            coreFreqList.add(FormatUtil.formatHertz(freq));
        }
        cpuInfo.setCoreFrequencies(coreFreqList);

        // Core loads
        double[] load = processor.getProcessorCpuLoad(1000);
        List<Double> coreLoads = new ArrayList<>();
        for (double coreLoad : load) {
            coreLoads.add(coreLoad * 100);
        }
        cpuInfo.setCoreLoads(coreLoads);

        // Frequency - 修正这部分
        currentFreq = processor.getCurrentFreq();
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
        cpuInfo.setVoltage(sensors.getCpuVoltage()); // 获取CPU电压

        // Additional info
        cpuInfo.setArchitecture(processor.getProcessorIdentifier().getMicroarchitecture());
        cpuInfo.setProcessorId(processor.getProcessorIdentifier().getProcessorID());

        // Context switches and interrupts
        long[] ticks = processor.getSystemCpuLoadTicks();
        cpuInfo.setContextSwitches(ticks[CentralProcessor.TickType.IRQ.getIndex()]);
        cpuInfo.setInterrupts(ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]);

        return cpuInfo;
    }

    private List<FanInfo> getFanInfo() {
        List<FanInfo> fanInfos = new ArrayList<>();
        Sensors sensors = systemInfo.getHardware().getSensors();
        oshi.hardware.Fan[] oshiFans = sensors.getFans();

        if (oshiFans != null && oshiFans.length > 0) {
            for (oshi.hardware.Fan oshiFan : oshiFans) {
                FanInfo fanInfo = new FanInfo();
                fanInfo.setName(oshiFan.getName().isEmpty() ? "N/A" : oshiFan.getName());
                fanInfo.setSpeed(oshiFan.getSpeed());
                fanInfo.setStatus(oshiFan.getSpeed() > 0 ? "OK" : "N/A");
                fanInfos.add(fanInfo);
            }
        } else {
            // Fallback to sensors command
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("sensors");
                Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Fan")) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            String name = parts[0].trim();
                            String speedStr = parts[1].trim().replaceAll("[^0-9]", ""); // 提取数字部分
                            if (!speedStr.isEmpty()) {
                                int speed = Integer.parseInt(speedStr);
                                FanInfo fanInfo = new FanInfo();
                                fanInfo.setName(name);
                                fanInfo.setSpeed(speed);
                                fanInfo.setStatus(speed > 0 ? "OK" : "N/A");
                                fanInfos.add(fanInfo);
                            }
                        }
                    }
                }
                reader.close();
            } catch (IOException | NumberFormatException e) {
                System.err.println("Failed to read fan information using sensors command: " + e.getMessage());
            }
        }
        return fanInfos;
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
        List<oshi.hardware.GraphicsCard> oshiGpus = systemInfo.getHardware().getGraphicsCards();

        // Temporary list to hold NVIDIA data if needed for matching
        List<GPUInfo> nvidiaSmiInfos = new ArrayList<>();
        boolean nvidiaSmiQueried = false;

        for (oshi.hardware.GraphicsCard card : oshiGpus) {
            GPUInfo info = new GPUInfo();
            info.setName(card.getName());
            info.setVendor(card.getVendor());
            info.setDeviceId(card.getDeviceId());
            info.setVramTotalBytes(card.getVRam()); // OSHI VRam is in bytes
            // Format VRAM to MB string for the 'vram' field for compatibility, if needed by frontend
            if (card.getVRam() > 0) {
                info.setVram(FormatUtil.formatBytes(card.getVRam()).replace("iB", "B"));
            } else {
                info.setVram("N/A");
            }
            info.setDriverVersion(card.getVersionInfo());
            // Initialize other fields to default/null
            info.setUsedVram("N/A");
            info.setVramUsagePercentage(0.0);
            info.setGpuLoad(0.0); // OSHI doesn't directly provide a generic GPU load
            info.setTemperature(Double.NaN); // Use NaN to indicate not set

            if (card.getVendor().toLowerCase().contains("nvidia")) {
                if (!nvidiaSmiQueried) {
                    // Query nvidia-smi once and store results
                    try {
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
                                GPUInfo smiInfo = new GPUInfo();
                                smiInfo.setName(parts[0].trim());
                                smiInfo.setVram(parts[1].trim() + " MB"); // Keep this format for matching/comparison
                                smiInfo.setUsedVram(parts[2].trim() + " MB");
                                double total = Double.parseDouble(parts[1].trim());
                                double used = Double.parseDouble(parts[2].trim());
                                if (total > 0) {
                                    smiInfo.setVramUsagePercentage((used / total) * 100);
                                }
                                smiInfo.setGpuLoad(Double.parseDouble(parts[3].trim()));
                                smiInfo.setTemperature(Double.parseDouble(parts[4].trim()));
                                smiInfo.setDriverVersion(parts[5].trim()); // nvidia-smi driver version
                                smiInfo.setDisplayMode(parts[6].trim());
                                smiInfo.setVramTotalBytes((long) (total * 1024 * 1024));
                                smiInfo.setVramUsedBytes((long) (used * 1024 * 1024));
                                nvidiaSmiInfos.add(smiInfo);
                            }
                        }
                        nvidiaReader.close();
                    } catch (IOException | NumberFormatException e) {
                        System.err.println("Failed to read NVIDIA GPU info from nvidia-smi: " + e.getMessage());
                    }
                    nvidiaSmiQueried = true;
                }

                // Try to match OSHI card with nvidia-smi data
                // Simple matching by name for now, assuming names are somewhat consistent
                // More robust matching might involve PCI IDs if available and consistent
                for (GPUInfo smiInfo : nvidiaSmiInfos) {
                    if (smiInfo.getName().contains(info.getName()) || info.getName().contains(smiInfo.getName())) {
                        info.setVram(smiInfo.getVram()); // Use nvidia-smi's formatted VRAM
                        info.setUsedVram(smiInfo.getUsedVram());
                        info.setVramUsagePercentage(smiInfo.getVramUsagePercentage());
                        info.setGpuLoad(smiInfo.getGpuLoad());
                        info.setTemperature(smiInfo.getTemperature());
                        // Decide which driver version to keep, OSHI or nvidia-smi. OSHI's might be the kernel driver version.
                        // info.setDriverVersion(smiInfo.getDriverVersion()); // Optionally override
                        info.setDisplayMode(smiInfo.getDisplayMode());
                        info.setVramUsedBytes(smiInfo.getVramUsedBytes());
                        // VramTotalBytes from OSHI should be reliable, but can be overridden if nvidia-smi is preferred
                        // info.setVramTotalBytes(smiInfo.getVramTotalBytes());
                        break; // Found a match
                    }
                }
            }

            // Fallback for temperature using 'sensors' if not set by OSHI or nvidia-smi
            if (Double.isNaN(info.getTemperature()) || info.getTemperature() == 0.0) { // OSHI might return 0.0 for temp
                try {
                    ProcessBuilder sensorsProcessBuilder = new ProcessBuilder("sensors");
                    Process sensorsProcess = sensorsProcessBuilder.start();
                    BufferedReader sensorsReader = new BufferedReader(new InputStreamReader(sensorsProcess.getInputStream()));
                    String sensorsLine;
                    // Generic patterns, attempt to match based on common adapter names
                    // This is very basic and might pick up CPU temp if GPU temp isn't clearly identifiable
                    // Example patterns: "temp1", "edge", "junction" for AMD; "Package" for Intel iGPU (often shared with CPU)
                    // A more specific pattern for GPUs: (amdgpu|nouveau|nvidia).*\+(\d+\.\d+)°C
                    // For Intel iGPUs, temperature is often part of the CPU package temp and not listed separately by 'sensors'
                    // Or it might be under "Adapter: ISA adapter" for some integrated graphics.
                    Pattern tempPattern = Pattern.compile(
                        "(?i)(?:amdgpu|radeon|nouveau|nvidia|vga|gpu|display|gfx|igpu).*?(?:temp|junction|edge).*?\\+(\\d+\\.\\d+)°C",
                         Pattern.CASE_INSENSITIVE);
                    // A more generic pattern if the above fails for some cards
                    Pattern genericTempPattern = Pattern.compile("temp\\d+:\\s*\\+(\\d+\\.\\d+)°C");


                    String potentialGpuNameFromSensors = "";
                    double parsedTemp = Double.NaN;

                    while ((sensorsLine = sensorsReader.readLine()) != null) {
                        // Try to identify sections related to the current GPU vendor or name
                        if (info.getVendor().toLowerCase().contains("amd") && sensorsLine.toLowerCase().contains("amdgpu")) {
                             potentialGpuNameFromSensors = "amdgpu";
                        } else if (info.getVendor().toLowerCase().contains("intel") && (sensorsLine.toLowerCase().contains("pch") || sensorsLine.toLowerCase().contains("intel"))) {
                            // Intel temps are tricky with `sensors`. Often part of CPU or PCH.
                            // This condition is broad and might not be accurate.
                            potentialGpuNameFromSensors = "intel";
                        } else if (sensorsLine.toLowerCase().contains(info.getName().split(" ")[0].toLowerCase())) { // Match first word of GPU name
                            potentialGpuNameFromSensors = info.getName().toLowerCase();
                        }


                        Matcher matcher = tempPattern.matcher(sensorsLine);
                        if (matcher.find()) {
                            // If we have a potential GPU name, check if the line matches
                            if (!potentialGpuNameFromSensors.isEmpty() && sensorsLine.toLowerCase().contains(potentialGpuNameFromSensors)) {
                                parsedTemp = Double.parseDouble(matcher.group(1));
                                break; 
                            }
                        }
                        // If specific pattern didn't match or we don't have a clear GPU section, try generic
                        if (Double.isNaN(parsedTemp)) {
                             Matcher genericMatcher = genericTempPattern.matcher(sensorsLine);
                             if (genericMatcher.find()) {
                                 // This is a less reliable catch-all. Only use if no other temp found.
                                 // And try to associate it if the current card is the only one, or if no other temps are found.
                                 parsedTemp = Double.parseDouble(genericMatcher.group(1));
                                 // Don't break here, let it find more specific ones if possible
                             }
                        }
                    }
                    sensorsReader.close();
                    if (!Double.isNaN(parsedTemp)) {
                         info.setTemperature(parsedTemp);
                    }

                } catch (IOException | NumberFormatException e) {
                    System.err.println("Failed to read GPU temperature from sensors for " + info.getName() + ": " + e.getMessage());
                }
            }
            gpuInfos.add(info);
        }

        // Fallback: If OSHI returned no GPUs, try the old methods
        if (gpuInfos.isEmpty()) {
            System.err.println("OSHI returned no graphics cards. Falling back to legacy methods.");
            // 1. Try nvidia-smi (original broader query)
            if (!nvidiaSmiQueried) { // Avoid re-querying if already done
                 try {
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
                            GPUInfo smiInfo = new GPUInfo();
                            smiInfo.setName(parts[0].trim());
                            smiInfo.setVendor("NVIDIA"); // Assuming nvidia-smi means NVIDIA
                            smiInfo.setVram(parts[1].trim() + " MB");
                            smiInfo.setUsedVram(parts[2].trim() + " MB");
                            double total = Double.parseDouble(parts[1].trim());
                            double used = Double.parseDouble(parts[2].trim());
                             if (total > 0) {
                                smiInfo.setVramUsagePercentage((used / total) * 100);
                            }
                            smiInfo.setGpuLoad(Double.parseDouble(parts[3].trim()));
                            smiInfo.setTemperature(Double.parseDouble(parts[4].trim()));
                            smiInfo.setDriverVersion(parts[5].trim());
                            smiInfo.setDisplayMode(parts[6].trim());
                            smiInfo.setVramTotalBytes((long) (total * 1024 * 1024));
                            smiInfo.setVramUsedBytes((long) (used * 1024 * 1024));
                            gpuInfos.add(smiInfo); // Add directly to gpuInfos
                        }
                    }
                    nvidiaReader.close();
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Failed to read NVIDIA GPU info from nvidia-smi (fallback): " + e.getMessage());
                }
            }


            // 2. If still empty (no NVIDIA cards or nvidia-smi failed), try sensors for other GPUs (original sensors fallback)
            if (gpuInfos.isEmpty()) {
                try {
                    ProcessBuilder sensorsProcessBuilder = new ProcessBuilder("sensors");
                    Process sensorsProcess = sensorsProcessBuilder.start();
                    BufferedReader sensorsReader = new BufferedReader(new InputStreamReader(sensorsProcess.getInputStream()));
                    String sensorsLine;
                    // This pattern is from the original code, specific to amdgpu/nouveau
                    Pattern gpuTempPattern = Pattern.compile("(amdgpu|nouveau).*\\+(\\d+\\.\\d+)°C");
                    while ((sensorsLine = sensorsReader.readLine()) != null) {
                        Matcher matcher = gpuTempPattern.matcher(sensorsLine);
                        if (matcher.find()) {
                            GPUInfo info = new GPUInfo(); // Create a new one as we have no OSHI baseline
                            // Try to infer vendor/name from the pattern
                            String matchedName = matcher.group(1);
                            if (matchedName.equalsIgnoreCase("amdgpu")) {
                                info.setName("AMD GPU (from sensors)");
                                info.setVendor("AMD");
                            } else if (matchedName.equalsIgnoreCase("nouveau")) {
                                info.setName("NVIDIA GPU (Nouveau - from sensors)");
                                info.setVendor("NVIDIA (Nouveau)");
                            } else {
                                info.setName("Unknown GPU (from sensors)");
                            }
                            info.setTemperature(Double.parseDouble(matcher.group(2)));
                            // Set other fields to N/A or default
                             info.setVram("N/A");
                             info.setUsedVram("N/A");
                             info.setVramTotalBytes(0);
                             info.setVramUsedBytes(0);
                            gpuInfos.add(info);
                            // Original code assumed one GPU from sensors, this could be adapted if sensors output lists multiple clearly
                            // For now, let's keep it simple and potentially take the first one or all that match.
                            // The original code had 'break', taking only the first.
                            // If multiple are found by this regex, they would be added.
                        }
                    }
                    sensorsReader.close();
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Failed to read GPU temperature from sensors (fallback): " + e.getMessage());
                }
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
