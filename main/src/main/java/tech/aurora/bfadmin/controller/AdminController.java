package tech.aurora.bfadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import tech.aurora.bfadmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
      
      // Only add basic attributes for fast page load
      model.addAttribute("helloMessage", helloMessage);
      model.addAttribute("activePage", "dashboard");
      model.addAttribute("currentPage", "dashboard");
      
      logger.info("Dashboard page loaded (data will be loaded asynchronously)");
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
      // Only add basic attributes for fast page load
      model.addAttribute("activePage", "queues");
      model.addAttribute("currentPage", "queues");
      
      logger.info("Queues page loaded (data will be loaded asynchronously)");
      
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
      // Only add basic attributes for fast page load
      model.addAttribute("activePage", "dispatched");
      model.addAttribute("currentPage", "dispatched");
      
      logger.info("Dispatched operations page loaded (data will be loaded asynchronously)");
      
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
      // Only add basic attributes for fast page load
      model.addAttribute("activePage", "prequeue");
      model.addAttribute("currentPage", "prequeue");
      
      logger.info("Prequeue operations page loaded (data will be loaded asynchronously)");
      
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
  
  // Async API endpoints for background data loading
  @RequestMapping("/api/dashboard/data")
  @ResponseBody
  public java.util.Map<String, Object> getDashboardDataAsync() {
    logger.info("Dashboard data requested via async API");
    if (!ui) {
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "UI is not enabled");
      return errorResponse;
    }
    
    try {
      java.util.Map<String, Object> data = new java.util.HashMap<>();
      
      // Get system status
      java.util.Map<String, Object> systemStatus = adminService.getSystemStatus();
      data.put("systemStatus", systemStatus);
      
      // Get worker tables
      java.util.List<java.util.Map<String, Object>> executeWorkersTable = adminService.getExecuteWorkersTable();
      java.util.List<java.util.Map<String, Object>> storageWorkersTable = adminService.getStorageWorkersTable();
      data.put("executeWorkersTable", executeWorkersTable);
      data.put("storageWorkersTable", storageWorkersTable);
      
      // Get servers table
      java.util.List<java.util.Map<String, Object>> serversTable = adminService.getServersTable();
      data.put("serversTable", serversTable);
      
      logger.info("Dashboard async data: {} execute workers, {} storage workers, {} servers", 
                  executeWorkersTable.size(), storageWorkersTable.size(), serversTable.size());
      
      return data;
    } catch (Exception e) {
      logger.error("Failed to get dashboard data async", e);
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "Failed to load dashboard data: " + e.getMessage());
      return errorResponse;
    }
  }
  
  @RequestMapping("/api/queues/data")
  @ResponseBody
  public java.util.Map<String, Object> getQueuesDataAsync() {
    logger.info("Queues data requested via async API");
    if (!ui) {
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "UI is not enabled");
      return errorResponse;
    }
    
    try {
      java.util.Map<String, Object> data = new java.util.HashMap<>();
      
      // Get all operations from all queues
      java.util.List<java.util.Map<String, Object>> allOperations = adminService.getAllQueueOperations();
      java.util.List<String> queueNames = adminService.getQueueNames();
      
      data.put("operations", allOperations);
      data.put("queueNames", queueNames);
      
      logger.info("Queues async data: {} total operations across {} queues", allOperations.size(), queueNames.size());
      
      return data;
    } catch (Exception e) {
      logger.error("Failed to get queues data async", e);
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "Failed to load queues data: " + e.getMessage());
      return errorResponse;
    }
  }
  
  @RequestMapping("/api/prequeue/data")
  @ResponseBody
  public java.util.Map<String, Object> getPrequeueDataAsync() {
    logger.info("Prequeue data requested via async API");
    if (!ui) {
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "UI is not enabled");
      return errorResponse;
    }
    
    try {
      java.util.Map<String, Object> data = new java.util.HashMap<>();
      
      // Get all prequeued operations
      java.util.List<java.util.Map<String, Object>> prequeuedOperations = adminService.getPrequeuedOperations();
      data.put("operations", prequeuedOperations);
      
      logger.info("Prequeue async data: {} operations", prequeuedOperations.size());
      
      return data;
    } catch (Exception e) {
      logger.error("Failed to get prequeue data async", e);
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "Failed to load prequeue data: " + e.getMessage());
      return errorResponse;
    }
  }
  
  @RequestMapping("/api/dispatched/data")
  @ResponseBody
  public java.util.Map<String, Object> getDispatchedDataAsync() {
    logger.info("Dispatched data requested via async API");
    if (!ui) {
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "UI is not enabled");
      return errorResponse;
    }
    
    try {
      java.util.Map<String, Object> data = new java.util.HashMap<>();
      
      // Get all dispatched operations
      java.util.List<java.util.Map<String, Object>> dispatchedOperations = adminService.getDispatchedOperations();
      data.put("operations", dispatchedOperations);
      
      logger.info("Dispatched async data: {} operations", dispatchedOperations.size());
      
      return data;
    } catch (Exception e) {
      logger.error("Failed to get dispatched data async", e);
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("error", "Failed to load dispatched data: " + e.getMessage());
      return errorResponse;
    }
  }

  @RequestMapping(value = "/api/queues/{queueName}/operations/{operationName}", method = RequestMethod.DELETE)
  @ResponseBody
  public ResponseEntity<java.util.Map<String, Object>> deleteOperation(
      @PathVariable String queueName, 
      @PathVariable String operationName) {
    
    logger.info("Delete operation requested - Queue: {}, Operation: {}", queueName, operationName);
    
    if (!ui) {
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "UI is not enabled");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    try {
      boolean deleted = adminService.deleteOperation(queueName, operationName);
      
      java.util.Map<String, Object> response = new java.util.HashMap<>();
      response.put("success", deleted);
      
      if (deleted) {
        response.put("message", "Operation deleted successfully");
        logger.info("Successfully deleted operation: {} from queue: {}", operationName, queueName);
        return ResponseEntity.ok(response);
      } else {
        response.put("message", "Operation not found or could not be deleted");
        logger.warn("Failed to delete operation: {} from queue: {}", operationName, queueName);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }
      
    } catch (Exception e) {
      logger.error("Error deleting operation: {} from queue: {}", operationName, queueName, e);
      
      java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
      errorResponse.put("success", false);
      errorResponse.put("message", "Internal server error: " + e.getMessage());
      
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }
}
