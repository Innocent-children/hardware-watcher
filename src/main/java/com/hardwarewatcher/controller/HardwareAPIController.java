package com.hardwarewatcher.controller;

import com.hardwarewatcher.dto.HardwareInfo;
import com.hardwarewatcher.service.HardwareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api")
public class HardwareAPIController {

    private final HardwareService hardwareService;

    public HardwareAPIController(HardwareService hardwareService) {
        this.hardwareService = hardwareService;
    }

    @GetMapping("/hardware")
    public HardwareInfo getHardwareInfo() {
        return hardwareService.getHardwareInfo();
    }
}
