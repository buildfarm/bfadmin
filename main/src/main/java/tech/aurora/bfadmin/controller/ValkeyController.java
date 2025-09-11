package tech.aurora.bfadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import tech.aurora.bfadmin.service.AdminService;

@RestController
@RequestMapping("/valkey")
public class ValkeyController {
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping("/status")
    @ResponseBody
    public String getValkeyStatus() {
        return adminService.getValkeyStatus();
    }
    
    @GetMapping("/workers/execute")
    @ResponseBody
    public String getExecuteWorkers() {
        return adminService.getExecuteWorkersWithValues();
    }
    
    @GetMapping("/workers/storage")
    @ResponseBody
    public String getStorageWorkers() {
        return adminService.getStorageWorkersWithValues();
    }
}
