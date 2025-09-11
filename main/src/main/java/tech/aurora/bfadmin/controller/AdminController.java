package tech.aurora.bfadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tech.aurora.bfadmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AdminController {
  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

  @Autowired
  AdminService adminService;

  @Value("${ui.enable}")
  private boolean ui;

  @RequestMapping("/")
  public String getMainApp() {
    return "redirect:/dashboard";
  }

  @RequestMapping("/dashboard")
  public String getDashboard(Model model) {
    if (ui) {
      String helloMessage = adminService.getHelloMessage();
      
      // Get enhanced system status
      java.util.Map<String, Object> systemStatus = adminService.getSystemStatus();
      
      // Get table data for workers
      java.util.List<java.util.Map<String, Object>> executeWorkersTable = adminService.getExecuteWorkersTable();
      java.util.List<java.util.Map<String, Object>> storageWorkersTable = adminService.getStorageWorkersTable();
      
      model.addAttribute("helloMessage", helloMessage);
      model.addAttribute("systemStatus", systemStatus);
      model.addAttribute("executeWorkersTable", executeWorkersTable);
      model.addAttribute("storageWorkersTable", storageWorkersTable);
      
      logger.info("Dashboard accessed, system status: {}, {} execute workers, {} storage workers", 
                  systemStatus.get("systemHealthText"), executeWorkersTable.size(), storageWorkersTable.size());
      return "dashboard";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }
}
