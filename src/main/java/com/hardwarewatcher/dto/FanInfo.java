package com.hardwarewatcher.dto;

import lombok.Data;

@Data
public class FanInfo {
    private String name; // 风扇名称
    private int speed; // 风扇转速（RPM）
    private String status; // 风扇状态（如 "OK" 或 "Failed"）
}
