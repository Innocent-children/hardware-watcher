// 硬件监控AJAX更新逻辑
document.addEventListener('DOMContentLoaded', function () {
    // 获取刷新按钮
    const refreshBtn = document.getElementById('refreshBtn');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const statusText = document.getElementById('statusText');

    // 刷新数据函数
    function refreshData() {
        // 显示加载状态
        refreshBtn.disabled = true;
        loadingSpinner.style.display = 'inline-block';
        statusText.textContent = '数据更新中...';

        // 发送AJAX请求
        fetch('/api/hardware')
            .then(response => {
                if (!response.ok) {
                    throw new Error('网络响应异常');
                }
                return response.json();
            })
            .then(data => {
                updateDashboard(data);

                // 更新最后刷新时间
                const now = new Date();
                document.getElementById('lastUpdated').textContent = now.toLocaleString();
                statusText.innerHTML = `最后更新: <span id="lastUpdated">${now.toLocaleString()}</span>`;
            })
            .catch(error => {
                console.error('获取数据失败:', error);
                statusText.textContent = '更新失败: ' + error.message;
            })
            .finally(() => {
                refreshBtn.disabled = false;
                loadingSpinner.style.display = 'none';
            });
    }

    // 更新DOM的函数
    function updateDashboard(data) {
        // 更新CPU信息
        document.getElementById('cpuName').textContent = data.cpuInfo.name;
        document.getElementById('cpuLoad').textContent = data.cpuInfo.cpuLoad.toFixed(1) + '%';
        document.getElementById('cpuLoadBar').style.width = data.cpuInfo.cpuLoad.toFixed(1) + '%';

        // 更新核心负载
        const coreContainer = document.getElementById('coreLoadsContainer');
        coreContainer.innerHTML = '';
        data.cpuInfo.coreLoads.forEach((load, index) => {
            coreContainer.innerHTML += `
                <div class="col-6 col-md-3">
                    <div class="core-box rounded p-2 text-center">
                        <small class="d-block text-muted">核心 ${index}</small>
                        <span class="d-block fw-bold">${load.toFixed(1)}%</span>
                        <div class="progress bg-dark mt-1" style="height: 5px;">
                            <div class="progress-bar bg-gradient-cpu" style="width: ${load.toFixed(1)}%"></div>
                        </div>
                    </div>
                </div>
            `;
        });

        // 更新内存信息
        document.getElementById('memoryUsage').textContent = data.memoryInfo.memoryUsagePercentage.toFixed(1) + '%';
        document.getElementById('memoryUsageBar').style.width = data.memoryInfo.memoryUsagePercentage.toFixed(1) + '%';
        document.getElementById('usedMemory').textContent = data.memoryInfo.usedMemory;
        document.getElementById('availableMemory').textContent = data.memoryInfo.availableMemory;
        document.getElementById('totalMemory').textContent = data.memoryInfo.totalMemory;

        // 可以继续添加其他硬件信息的更新...
    }

    // 绑定按钮事件
    refreshBtn.addEventListener('click', refreshData);

    // 可选：初始加载数据
    // refreshData();
});