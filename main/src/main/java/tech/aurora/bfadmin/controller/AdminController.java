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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
public class AdminController {
  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

  @Autowired
  AdminService adminService;
  
  @Autowired
  ObjectMapper objectMapper;

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
      // Get all operations from all queues
      java.util.List<java.util.Map<String, Object>> allOperations = adminService.getAllQueueOperations();
      java.util.List<String> queueNames = adminService.getQueueNames();
      
      model.addAttribute("operations", allOperations);
      model.addAttribute("queueNames", queueNames);
      model.addAttribute("activePage", "queues");
      model.addAttribute("currentPage", "queues");
      
      logger.info("Queues page accessed, found {} total operations across {} queues", allOperations.size(), queueNames.size());
      
      // Log raw JSON of operations for debugging
      try {
        String operationsJson = objectMapper.writeValueAsString(allOperations);
        logger.info("RAW OPERATIONS JSON: {}", operationsJson);
        
        // Also log individual operation samples
        if (!allOperations.isEmpty()) {
          logger.info("First operation sample: {}", allOperations.get(0));
          logger.info("Total operations count: {}", allOperations.size());
          
          // Log first few operations in detail
          for (int i = 0; i < Math.min(3, allOperations.size()); i++) {
            String singleOpJson = objectMapper.writeValueAsString(allOperations.get(i));
            logger.info("Operation {} JSON: {}", i, singleOpJson);
          }
        } else {
          logger.warn("Operations list is empty or null");
        }
      } catch (JsonProcessingException e) {
        logger.error("Failed to convert operations to JSON", e);
      }
      
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
  
  @RequestMapping("/api/all-queue-operations")
  @ResponseBody
  public java.util.List<java.util.Map<String, Object>> getAllQueueOperations() {
    logger.info("All queue operations requested via API");
    java.util.List<java.util.Map<String, Object>> operations = adminService.getAllQueueOperations();
    
    // Log raw JSON of API response
    try {
      String operationsJson = objectMapper.writeValueAsString(operations);
      logger.info("API RAW OPERATIONS JSON: {}", operationsJson);
    } catch (JsonProcessingException e) {
      logger.error("Failed to convert API operations to JSON", e);
    }
    
    return operations;
  }
  
  @RequestMapping("/dispatched")
  public String getDispatchedOperations(Model model) {
    if (ui) {
      // Get all dispatched operations
      java.util.List<java.util.Map<String, Object>> dispatchedOperations = adminService.getDispatchedOperations();
      
      model.addAttribute("operations", dispatchedOperations);
      model.addAttribute("activePage", "dispatched");
      model.addAttribute("currentPage", "dispatched");
      
      logger.info("Dispatched operations page accessed, found {} operations", dispatchedOperations.size());
      
      // Log raw JSON of dispatched operations for debugging
      try {
        String operationsJson = objectMapper.writeValueAsString(dispatchedOperations);
        logger.info("RAW DISPATCHED OPERATIONS JSON: {}", operationsJson);
        
        // Also log individual operation samples
        if (!dispatchedOperations.isEmpty()) {
          logger.info("First dispatched operation sample: {}", dispatchedOperations.get(0));
          logger.info("Total dispatched operations count: {}", dispatchedOperations.size());
        } else {
          logger.warn("Dispatched operations list is empty");
        }
      } catch (JsonProcessingException e) {
        logger.error("Failed to convert dispatched operations to JSON", e);
      }
      
      return "dispatched";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");  
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }
  
  @RequestMapping("/api/dispatched-operations")
  @ResponseBody
  public java.util.List<java.util.Map<String, Object>> getDispatchedOperationsApi() {
    logger.info("Dispatched operations requested via API");
    java.util.List<java.util.Map<String, Object>> operations = adminService.getDispatchedOperations();
    
    // Log raw JSON of API response
    try {
      String operationsJson = objectMapper.writeValueAsString(operations);
      logger.info("API RAW DISPATCHED OPERATIONS JSON: {}", operationsJson);
    } catch (JsonProcessingException e) {
      logger.error("Failed to convert API dispatched operations to JSON", e);
    }
    
    return operations;
  }
  
  @RequestMapping("/prequeue")
  public String getPrequeuedOperations(Model model) {
    if (ui) {
      // Get all prequeued operations
      java.util.List<java.util.Map<String, Object>> prequeuedOperations = adminService.getPrequeuedOperations();
      
      model.addAttribute("operations", prequeuedOperations);
      model.addAttribute("activePage", "prequeue");
      model.addAttribute("currentPage", "prequeue");
      
      logger.info("Prequeued operations page accessed, found {} operations", prequeuedOperations.size());
      
      // Log raw JSON of prequeued operations for debugging
      try {
        String operationsJson = objectMapper.writeValueAsString(prequeuedOperations);
        logger.info("RAW PREQUEUED OPERATIONS JSON: {}", operationsJson);
        
        // Also log individual operation samples
        if (!prequeuedOperations.isEmpty()) {
          logger.info("First prequeued operation sample: {}", prequeuedOperations.get(0));
          logger.info("Total prequeued operations count: {}", prequeuedOperations.size());
        } else {
          logger.warn("Prequeued operations list is empty");
        }
      } catch (JsonProcessingException e) {
        logger.error("Failed to convert prequeued operations to JSON", e);
      }
      
      return "prequeue";
    } else {
      model.addAttribute("status", "999");
      model.addAttribute("error", "Not Enabled");  
      model.addAttribute("message", "UI is not enabled. Set ui.enable=true in application.properties");
      return "error";
    }
  }
  
  @RequestMapping("/api/prequeued-operations")
  @ResponseBody
  public java.util.List<java.util.Map<String, Object>> getPrequeuedOperationsApi() {
    logger.info("Prequeued operations requested via API");
    java.util.List<java.util.Map<String, Object>> operations = adminService.getPrequeuedOperations();
    
    // Log raw JSON of API response
    try {
      String operationsJson = objectMapper.writeValueAsString(operations);
      logger.info("API RAW PREQUEUED OPERATIONS JSON: {}", operationsJson);
    } catch (JsonProcessingException e) {
      logger.error("Failed to convert API prequeued operations to JSON", e);
    }
    
    return operations;
  }
}
