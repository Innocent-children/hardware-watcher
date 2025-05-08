document.addEventListener('DOMContentLoaded', function () {
    // Set current year in footer
    document.getElementById('currentYear').textContent = new Date().getFullYear();

    // Initialize refresh interval
    let refreshInterval = 5000; // Default 5 seconds
    let refreshTimer;

    // DOM elements
    const refreshIntervalSelect = document.getElementById('refreshInterval');
    const manualRefreshBtn = document.getElementById('manualRefresh');

    // Event listeners
    refreshIntervalSelect.addEventListener('change', function () {
        refreshInterval = parseInt(this.value);
        setupRefreshTimer();
    });

    manualRefreshBtn.addEventListener('click', function () {
        fetchHardwareData();
        // Add animation class
        this.classList.add('refreshing');
        setTimeout(() => this.classList.remove('refreshing'), 1000);
    });

    // Initialize
    fetchHardwareData();
    setupRefreshTimer();

    function setupRefreshTimer() {
        if (refreshTimer) {
            clearInterval(refreshTimer);
        }
        refreshTimer = setInterval(fetchHardwareData, refreshInterval);
    }

    function updateFanInfo(fanInfoList) {
        if (!fanInfoList || fanInfoList.length === 0) {
            document.getElementById('fanContent').innerHTML = '<p>No fan information available</p>';
            return;
        }

        const fanContent = document.getElementById('fanContent');
        fanContent.innerHTML = ''; // 清空之前的风扇信息

        fanInfoList.forEach((fan, index) => {
            const fanElement = document.createElement('div');
            fanElement.className = 'hardware-item';

            fanElement.innerHTML = `
            <h3>${fan.name}</h3>
            <div class="info-grid">
                <div><span>Speed:</span> <span>${fan.speed} RPM</span></div>
                <div><span>Status:</span> <span>${fan.status}</span></div>
            </div>
        `;

            fanContent.appendChild(fanElement);
        });
    }

    function fetchHardwareData() {
        fetch('/api/hardware')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                updateLastUpdated();
                updateSystemInfo(data);
                updateCPUInfo(data.cpuInfo);
                updateMemoryInfo(data.memoryInfo);
                updateGPUInfo(data.gpuInfoList);
                updateStorageInfo(data.storageInfoList);
                updateNetworkInfo(data.networkInfoList);
                updateFanInfo(data.fanInfoList); // 更新风扇信息
            })
            .catch(error => {
                console.error('Error fetching hardware data:', error);
                showErrorNotification('Failed to fetch hardware data. Please try again.');
            });
    }

    function updateLastUpdated() {
        const now = new Date();
        document.getElementById('lastUpdated').textContent = now.toLocaleTimeString();
    }

    function updateSystemInfo(data) {
        document.getElementById('systemName').textContent = data.systemName || '-';
        document.getElementById('systemManufacturer').textContent = data.systemManufacturer || '-';
        document.getElementById('systemModel').textContent = data.systemModel || '-';
        document.getElementById('osName').textContent = data.osName || '-';
        document.getElementById('osVersion').textContent = data.osVersion || '-';
        document.getElementById('uptime').textContent = data.uptime || '-';
    }

    function updateCPUInfo(cpuInfo) {
        if (!cpuInfo) return;

        document.getElementById('cpuName').textContent = cpuInfo.name || '-';
        document.getElementById('cpuVendor').textContent = cpuInfo.vendor || '-';
        document.getElementById('cpuCores').textContent = `${cpuInfo.physicalCores || 0} physical / ${cpuInfo.logicalCores || 0} logical`;
        document.getElementById('cpuArch').textContent = cpuInfo.architecture || '-';
        document.getElementById('cpuCurrentFreq').textContent = cpuInfo.currentFreq || '-';
        document.getElementById('cpuMaxFreq').textContent = cpuInfo.maxFreq || '-';

        const cpuLoad = cpuInfo.cpuLoad ? Math.round(cpuInfo.cpuLoad * 100) : 0;
        document.getElementById('cpuLoad').textContent = `${cpuLoad}%`;
        document.getElementById('cpuLoadBar').style.width = `${cpuLoad}%`;
        document.getElementById('cpuLoadValue').textContent = `${cpuLoad}%`;

        const cpuTemp = cpuInfo.temperature || 0;
        const tempElement = document.getElementById('cpuTemp');
        tempElement.textContent = `${cpuTemp}°C`;
        updateTemperatureClass(tempElement, cpuTemp);

        // 添加电压信息
        document.getElementById('cpuVoltage').textContent = cpuInfo.voltage ? `${cpuInfo.voltage} V` : '-';

        // 更新核心负载和频率
        const coreLoadsContainer = document.getElementById('coreLoadsContainer');
        coreLoadsContainer.innerHTML = '';

        if (cpuInfo.coreLoads && cpuInfo.coreLoads.length > 0) {
            cpuInfo.coreLoads.forEach((load, index) => {
                const coreLoad = Math.round(load);
                const coreElement = document.createElement('div');
                coreElement.className = 'core-load';

                // 获取对应核心的频率
                const coreFreq = cpuInfo.coreFrequencies && index < cpuInfo.coreFrequencies.length ? cpuInfo.coreFrequencies[index] : 'N/A';

                coreElement.innerHTML = `
                <div class="core-load-label">Core ${index + 1}</div>
                <div class="core-load-bar">
                    <div class="core-load-fill" style="width: ${coreLoad}%"></div>
                </div>
                <div class="core-load-value">${coreLoad}%</div>
                <div class="core-frequency-value">${coreFreq}</div>
            `;

                coreLoadsContainer.appendChild(coreElement);
            });
        }
    }

    function updateMemoryInfo(memoryInfo) {
        if (!memoryInfo) return;

        document.getElementById('memTotal').textContent = memoryInfo.totalMemory || '-';
        document.getElementById('memUsed').textContent = memoryInfo.usedMemory || '-';
        document.getElementById('memAvailable').textContent = memoryInfo.availableMemory || '-';
        document.getElementById('swapTotal').textContent = memoryInfo.swapTotal || '-';
        document.getElementById('swapUsed').textContent = memoryInfo.swapUsed || '-';

        const memUsage = memoryInfo.memoryUsagePercentage ? Math.round(memoryInfo.memoryUsagePercentage) : 0;
        document.getElementById('memUsage').textContent = `${memUsage}%`;
        document.getElementById('memUsageBar').style.width = `${memUsage}%`;
        document.getElementById('memUsageValue').textContent = `${memUsage}%`;

        const swapUsage = memoryInfo.swapUsagePercentage ? Math.round(memoryInfo.swapUsagePercentage) : 0;
        document.getElementById('swapUsageBar').style.width = `${swapUsage}%`;
        document.getElementById('swapUsageValue').textContent = `${swapUsage}%`;
    }

    function updateGPUInfo(gpuInfoList) {
        const gpuContent = document.getElementById('gpuContent');

        if (!gpuInfoList || gpuInfoList.length === 0) {
            gpuContent.innerHTML = '<p>No GPU information available</p>';
            return;
        }

        gpuContent.innerHTML = ''; // Clear previous content

        gpuInfoList.forEach((gpu, index) => {
            const gpuElement = document.createElement('div');
            gpuElement.className = 'hardware-item';

            const vramUsage = gpu.vramUsagePercentage ? Math.round(gpu.vramUsagePercentage) : 0;
            const gpuLoad = gpu.gpuLoad ? Math.round(gpu.gpuLoad) : 0;

            gpuElement.innerHTML = `
                <h3>${gpu.name || `GPU ${index + 1}`} <span class="vendor">${gpu.vendor || ''}</span></h3>
                <div class="info-grid">
                    <div><span>VRAM:</span> <span>${gpu.usedVram || '-'} / ${gpu.vram || '-'}</span></div>
                    <div><span>Driver:</span> <span>${gpu.driverVersion || '-'}</span></div>
                    <div><span>Display:</span> <span>${gpu.displayMode || '-'}</span></div>
                </div>
                <div class="progress-container">
                    <div class="progress-label">VRAM Usage</div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${vramUsage}%"></div>
                    </div>
                    <div class="progress-value">${vramUsage}%</div>
                </div>
                <div class="progress-container">
                    <div class="progress-label">GPU Load</div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${gpuLoad}%"></div>
                    </div>
                    <div class="progress-value">${gpuLoad}%</div>
                </div>
            `;

            if (gpu.temperature) {
                const tempElement = document.createElement('div');
                tempElement.className = 'info-grid';
                tempElement.innerHTML = `
                    <div><span>Temperature:</span> <span class="temp-${getTemperatureClass(gpu.temperature)}">${gpu.temperature}°C</span></div>
                `;
                gpuElement.appendChild(tempElement);
            }

            gpuContent.appendChild(gpuElement);
        });
    }

    function updateStorageInfo(storageInfoList) {
        const storageContent = document.getElementById('storageContent');

        if (!storageInfoList || storageInfoList.length === 0) {
            storageContent.innerHTML = '<p>No storage information available</p>';
            return;
        }

        storageContent.innerHTML = ''; // Clear previous content

        storageInfoList.forEach((storage, index) => {
            const storageElement = document.createElement('div');
            storageElement.className = 'hardware-item';

            const usagePercentage = storage.usagePercentage ? Math.round(storage.usagePercentage) : 0;

            storageElement.innerHTML = `
                <h3>${storage.name || `Storage ${index + 1}`} <span class="model">${storage.model || ''}</span></h3>
                <div class="info-grid">
                    <div><span>Type:</span> <span>${storage.type || '-'}</span></div>
                    <div><span>Total:</span> <span>${storage.totalSpace || '-'}</span></div>
                    <div><span>Used:</span> <span>${storage.usedSpace || '-'}</span></div>
                    <div><span>Free:</span> <span>${storage.freeSpace || '-'}</span></div>
                </div>
                <div class="progress-container">
                    <div class="progress-label">Usage</div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${usagePercentage}%"></div>
                    </div>
                    <div class="progress-value">${usagePercentage}%</div>
                </div>
            `;

            if (storage.temperature && storage.temperature !== "N/A") {
                const tempElement = document.createElement('div');
                tempElement.className = 'info-grid';
                tempElement.innerHTML = `
                    <div><span>Temperature:</span> <span class="temp-${getTemperatureClass(parseFloat(storage.temperature))}">${storage.temperature}°C</span></div>
                `;
                storageElement.appendChild(tempElement);
            }

            storageContent.appendChild(storageElement);
        });
    }

    function updateNetworkInfo(networkInfoList) {
        const networkContent = document.getElementById('networkContent');

        if (!networkInfoList || networkInfoList.length === 0) {
            networkContent.innerHTML = '<p>No network information available</p>';
            return;
        }

        networkContent.innerHTML = ''; // Clear previous content

        networkInfoList.forEach((network, index) => {
            const networkElement = document.createElement('div');
            networkElement.className = 'hardware-item';

            const statusClass = network.isConnected ? 'connected' : 'disconnected';

            networkElement.innerHTML = `
                <h3>${network.displayName || network.name || `Network ${index + 1}`} 
                    <span class="network-status ${statusClass}" title="${network.isConnected ? 'Connected' : 'Disconnected'}"></span>
                </h3>
                <div class="info-grid">
                    <div><span>MAC:</span> <span>${network.macAddress || '-'}</span></div>
                    <div><span>IPv4:</span> <span>${network.ipv4 || '-'}</span></div>
                    <div><span>IPv6:</span> <span>${network.ipv6 || '-'}</span></div>
                    <div><span>Sent:</span> <span>${network.dataSent || '-'}</span></div>
                    <div><span>Received:</span> <span>${network.dataReceived || '-'}</span></div>
                    <div><span>Upload:</span> <span>${network.uploadSpeed ? network.uploadSpeed.toFixed(2) + ' KB/s' : '-'}</span></div>
                    <div><span>Download:</span> <span>${network.downloadSpeed ? network.downloadSpeed.toFixed(2) + ' KB/s' : '-'}</span></div>
                    <div><span>Speed:</span> <span>${network.speed || '-'}</span></div>
                    <div><span>MTU:</span> <span>${network.mtu || '-'}</span></div>
                </div>
            `;

            networkContent.appendChild(networkElement);
        });
    }

    function updateTemperatureClass(element, temperature) {
        element.className = '';

        if (temperature >= 80) {
            element.classList.add('temp-high');
        } else if (temperature >= 60) {
            element.classList.add('temp-medium');
        } else {
            element.classList.add('temp-low');
        }
    }

    function getTemperatureClass(temperature) {
        if (temperature >= 80) {
            return 'high';
        } else if (temperature >= 60) {
            return 'medium';
        } else {
            return 'low';
        }
    }

    function showErrorNotification(message) {
        const notification = document.createElement('div');
        notification.className = 'notification error';
        notification.textContent = message;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.classList.add('fade-out');
            setTimeout(() => notification.remove(), 500);
        }, 3000);
    }
});