package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class NetworkInfo {
    private String name;
    private String displayName;
    private String macAddress;
    private String ipv4;
    private String ipv6;
    private String dataSent;
    private String dataReceived;
    private long bytesSent;
    private long bytesReceived;
    private double uploadSpeed;
    private double downloadSpeed;
    private String packetsSent;
    private String packetsReceived;
    private String speed;
    private String mtu;
    private boolean isConnected;
}
