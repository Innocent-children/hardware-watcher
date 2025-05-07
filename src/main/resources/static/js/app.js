// Update the page every 5 seconds
function startAutoRefresh() {
    setTimeout(function() {
        window.location.reload();
    }, 5000);
}

// Initialize all charts
function initializeCharts() {
    // CPU Core Load Chart
    if (document.getElementById('coreLoadChart')) {
        const coreLoads = JSON.parse(document.getElementById('coreLoadData').textContent);

        const coreLoadOptions = {
            series: [{
                name: 'Core Load',
                data: coreLoads.map((load, index) => ({
                    x: `Core ${index}`,
                    y: load
                }))
            }],
            chart: {
                type: 'bar',
                height: 350,
                foreColor: '#fff',
                toolbar: {
                    show: false
                }
            },
            plotOptions: {
                bar: {
                    borderRadius: 4,
                    horizontal: true,
                    distributed: true
                }
            },
            dataLabels: {
                enabled: false
            },
            colors: ['#3a86ff', '#3a86ff', '#3a86ff', '#3a86ff', '#3a86ff', '#3a86ff', '#3a86ff', '#3a86ff'],
            xaxis: {
                categories: coreLoads.map((_, index) => `Core ${index}`),
                labels: {
                    style: {
                        colors: '#adb5bd'
                    }
                }
            },
            yaxis: {
                labels: {
                    style: {
                        colors: '#adb5bd'
                    }
                }
            },
            grid: {
                borderColor: 'rgba(255, 255, 255, 0.1)'
            },
            tooltip: {
                theme: 'dark'
            }
        };

        const coreLoadChart = new ApexCharts(document.querySelector("#coreLoadChart"), coreLoadOptions);
        coreLoadChart.render();
    }
}

// Document ready
document.addEventListener('DOMContentLoaded', function() {
    startAutoRefresh();
    initializeCharts();
});