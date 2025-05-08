package com.hardwarewatcher.controller;

import com.hardwarewatcher.dto.HardwareInfo;
import com.hardwarewatcher.service.HardwareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HardwareController {

    private final HardwareService hardwareService;

    public HardwareController(HardwareService hardwareService) {
        this.hardwareService = hardwareService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        HardwareInfo hardwareInfo = hardwareService.getHardwareInfo();
        model.addAttribute("hardwareInfo", hardwareInfo);
        return "dashboard";
    }
}