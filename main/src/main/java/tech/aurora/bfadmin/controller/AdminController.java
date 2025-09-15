package tech.aurora.bfadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tech.aurora.bfadmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
      
      // Get table data for servers
      java.util.List<java.util.Map<String, Object>> serversTable = adminService.getServersTable();
      
      model.addAttribute("helloMessage", helloMessage);
      model.addAttribute("systemStatus", systemStatus);
      model.addAttribute("executeWorkersTable", executeWorkersTable);
      model.addAttribute("storageWorkersTable", storageWorkersTable);
      model.addAttribute("serversTable", serversTable);
      model.addAttribute("activePage", "dashboard");
      model.addAttribute("currentPage", "dashboard");
      
      logger.info("Dashboard accessed, system status: {}, {} execute workers, {} storage workers, {} servers", 
                  systemStatus.get("systemHealthText"), executeWorkersTable.size(), storageWorkersTable.size(), serversTable.size());
      return "dashboard";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }

  @RequestMapping("/queues")
  public String getQueues(Model model) {
    if (ui) {
      // Get available queue names
      java.util.List<String> queueNames = adminService.getQueueNames();
      
      model.addAttribute("queueNames", queueNames);
      model.addAttribute("selectedQueue", queueNames.isEmpty() ? "" : queueNames.get(0));
      model.addAttribute("activePage", "queues");
      model.addAttribute("currentPage", "queues");
      
      // If there are queues available, get operations for the first queue
      if (!queueNames.isEmpty()) {
        java.util.List<java.util.Map<String, Object>> operations = adminService.getQueueOperations(queueNames.get(0));
        model.addAttribute("operations", operations);
      } else {
        model.addAttribute("operations", new java.util.ArrayList<>());
      }
      
      logger.info("Queues page accessed, found {} queues", queueNames.size());
      return "queues";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");  
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }
  
  @RequestMapping("/api/queue-names")
  @ResponseBody
  public java.util.List<String> getQueueNames() {
    logger.info("Queue names requested via API");
    return adminService.getQueueNames();
  }
  
  @RequestMapping("/api/queue-operations")
  @ResponseBody
  public java.util.List<java.util.Map<String, Object>> getQueueOperations(@RequestParam String queueName) {
    logger.info("Queue operations requested for queue: {}", queueName);
    return adminService.getQueueOperations(queueName);
  }
}
