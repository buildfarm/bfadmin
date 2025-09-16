package tech.aurora.bfadmin.service.impl;

import tech.aurora.bfadmin.service.AdminService;
import tech.aurora.bfadmin.service.ValkeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AdminServiceImpl implements AdminService {
  private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

  @Autowired
  private ValkeyService valkeyService;

  @Override
  public String getHelloMessage() {
    logger.info("Hello message requested");
    return "Buildfarm administration and management service.";
  }

  @Override
  public String getValkeyStatus() {
    logger.info("Valkey status requested");
    try {
      return valkeyService.getClusterInfo();
    } catch (Exception e) {
      logger.error("Failed to get Valkey status", e);
      return "Error getting Valkey status: " + e.getMessage();
    }
  }

  @Override
  public String getWorkerKeys() {
    logger.info("Worker keys requested");
    try {
      Set<String> workerKeys = valkeyService.getKeys("Worker*");
      StringBuilder result = new StringBuilder();
      if (workerKeys.isEmpty()) {
        result.append("No workers found");
      } else {
        result.append("Found ").append(workerKeys.size()).append(" worker(s):\n");
        workerKeys.forEach(key -> result.append("• ").append(key).append("\n"));
      }
      return result.toString();
    } catch (Exception e) {
      logger.error("Failed to get worker keys from Valkey", e);
      return "Error getting worker keys: " + e.getMessage();
    }
  }

  @Override
  public String getExecuteWorkersWithValues() {
    logger.info("Execute workers with values requested");
    try {
      Set<String> executeWorkerKeys = valkeyService.getKeys("Workers_execute*");
      StringBuilder result = new StringBuilder();
      
      if (executeWorkerKeys.isEmpty()) {
        result.append("No execute workers found");
      } else {
        result.append("Execute Workers (").append(executeWorkerKeys.size()).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        for (String key : executeWorkerKeys) {
          try {
            // Use the ValkeyService cast to access new methods
            String formattedValue = ((tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService).getFormattedValue(key);
            result.append("\n🔧 ").append(key).append("\n");
            
            // Truncate very long values for display
            if (formattedValue.length() > 500) {
              result.append(formattedValue.substring(0, 500)).append("\n... [truncated]");
            } else {
              result.append(formattedValue);
            }
            result.append("\n").append("-".repeat(30)).append("\n");
          } catch (Exception e) {
            result.append("Error reading value: ").append(e.getMessage()).append("\n");
          }
        }
      }
      return result.toString();
    } catch (Exception e) {
      logger.error("Failed to get execute workers from Valkey", e);
      return "Error getting execute workers: " + e.getMessage();
    }
  }

  @Override
  public String getStorageWorkersWithValues() {
    logger.info("Storage workers with values requested");
    try {
      Set<String> storageWorkerKeys = valkeyService.getKeys("Workers_storage*");
      StringBuilder result = new StringBuilder();
      
      if (storageWorkerKeys.isEmpty()) {
        result.append("No storage workers found");
      } else {
        result.append("Storage Workers (").append(storageWorkerKeys.size()).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        for (String key : storageWorkerKeys) {
          try {
            // Use the ValkeyService cast to access new methods
            String formattedValue = ((tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService).getFormattedValue(key);
            result.append("\n💾 ").append(key).append("\n");
            
            // Truncate very long values for display
            if (formattedValue.length() > 500) {
              result.append(formattedValue.substring(0, 500)).append("\n... [truncated]");
            } else {
              result.append(formattedValue);
            }
            result.append("\n").append("-".repeat(30)).append("\n");
          } catch (Exception e) {
            result.append("Error reading value: ").append(e.getMessage()).append("\n");
          }
        }
      }
      return result.toString();
    } catch (Exception e) {
      logger.error("Failed to get storage workers from Valkey", e);
      return "Error getting storage workers: " + e.getMessage();
    }
  }

  @Override
  public java.util.List<java.util.Map<String, Object>> getExecuteWorkersTable() {
    logger.info("Execute workers table requested");
    try {
      // Cast to ValkeyService to access the new method
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      return valkeyServiceImpl.getWorkersAsTable("Workers_execute*");
    } catch (Exception e) {
      logger.error("Failed to get execute workers table from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public java.util.List<java.util.Map<String, Object>> getStorageWorkersTable() {
    logger.info("Storage workers table requested");
    try {
      // Cast to ValkeyService to access the new method
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      return valkeyServiceImpl.getWorkersAsTable("Workers_storage*");
    } catch (Exception e) {
      logger.error("Failed to get storage workers table from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public java.util.List<java.util.Map<String, Object>> getServersTable() {
    logger.info("Servers table requested");
    try {
      // Cast to ValkeyService to access the new method
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      
      // First, let's check what server-related keys exist
      Set<String> serverKeys = valkeyServiceImpl.getKeys("*server*");
      logger.info("🔍 Keys containing 'server': {}", serverKeys);
      
      Set<String> capitalServerKeys = valkeyServiceImpl.getKeys("*Server*");  
      logger.info("🔍 Keys containing 'Server': {}", capitalServerKeys);
      
      Set<String> allKeys = valkeyServiceImpl.getKeys("*");
      java.util.List<String> serverRelatedKeys = allKeys.stream()
        .filter(key -> key.toLowerCase().contains("server") || key.contains("Server"))
        .collect(java.util.stream.Collectors.toList());
      logger.info("🔍 All server-related keys found: {}", serverRelatedKeys);
      
      return valkeyServiceImpl.getServersAsTable("Servers*");
    } catch (Exception e) {
      logger.error("Failed to get servers table from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public java.util.Map<String, Object> getSystemStatus() {
    logger.info("System status requested");
    java.util.Map<String, Object> status = new java.util.HashMap<>();
    
    try {
      // Test Valkey connection with error handling
      boolean valkeyConnected = false;
      try {
        valkeyConnected = valkeyService != null ? valkeyService.testConnection() : false;
      } catch (Exception e) {
        logger.error("Failed to test Valkey connection", e);
        valkeyConnected = false;
      }
      
      status.put("valkeyConnected", valkeyConnected);
      status.put("valkeyStatus", valkeyConnected ? "Connected" : "Disconnected");
      status.put("valkeyStatusClass", valkeyConnected ? "success" : "danger");
      
      // Get worker counts
      java.util.List<java.util.Map<String, Object>> executeWorkers = getExecuteWorkersTable();
      java.util.List<java.util.Map<String, Object>> storageWorkers = getStorageWorkersTable();
      
      int executeCount = executeWorkers.size();
      int storageCount = storageWorkers.size();
      int totalWorkers = executeCount + storageCount;
      
      status.put("executeWorkerCount", executeCount);
      status.put("storageWorkerCount", storageCount);
      status.put("totalWorkerCount", totalWorkers);
      
      // Worker status classes based on counts
      status.put("executeWorkerClass", executeCount > 0 ? "success" : "warning");
      status.put("storageWorkerClass", storageCount > 0 ? "success" : "warning");
      status.put("totalWorkerClass", totalWorkers > 0 ? "success" : "danger");
      
      // Count active vs expired workers
      int activeExecute = (int) executeWorkers.stream()
        .filter(w -> w.get("status") != null && w.get("status").toString().startsWith("Active"))
        .count();
      int activeStorage = (int) storageWorkers.stream()
        .filter(w -> w.get("status") != null && w.get("status").toString().startsWith("Active"))
        .count();
      
      status.put("activeExecuteCount", activeExecute);
      status.put("activeStorageCount", activeStorage);
      status.put("activeTotalCount", activeExecute + activeStorage);
      
      // Get total key count if Valkey is connected
      if (valkeyConnected) {
        try {
          // Cast to ValkeyService to access additional methods
          tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
            (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
          java.util.Set<String> allKeys = valkeyServiceImpl.getKeys("*");
          status.put("totalKeyCount", allKeys != null ? allKeys.size() : 0);
          status.put("keyCountClass", allKeys != null && allKeys.size() > 0 ? "info" : "secondary");
        } catch (Exception e) {
          logger.warn("Failed to get key count", e);
          status.put("totalKeyCount", "Unknown");
          status.put("keyCountClass", "secondary");
        }
      } else {
        status.put("totalKeyCount", "N/A");
        status.put("keyCountClass", "secondary");
      }
      
      // Overall system health
      boolean systemHealthy = valkeyConnected && totalWorkers > 0 && (activeExecute + activeStorage) > 0;
      status.put("systemHealthy", systemHealthy);
      status.put("systemHealthClass", systemHealthy ? "success" : "warning");
      status.put("systemHealthText", systemHealthy ? "Healthy" : "Degraded");
      
      // Always include error field (empty when no error)
      status.put("error", null);
      
    } catch (Exception e) {
      logger.error("Failed to get system status", e);
      status.put("error", "Error retrieving system status: " + e.getMessage());
      status.put("systemHealthy", false);
      status.put("systemHealthClass", "danger");
      status.put("systemHealthText", "Error");
    }
    
    return status;
  }

  @Override
  public java.util.List<String> getQueueNames() {
    logger.info("Queue names requested");
    try {
      // Cast to ValkeyService to access queue methods
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      return valkeyServiceImpl.getQueueNames();
    } catch (Exception e) {
      logger.error("Failed to get queue names from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  @Override  
  public java.util.List<java.util.Map<String, Object>> getQueueOperations(String queueName) {
    logger.info("Queue operations requested for queue: {}", queueName);
    try {
      // Cast to ValkeyService to access queue methods
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      return valkeyServiceImpl.getQueueOperations(queueName);
    } catch (Exception e) {
      logger.error("Failed to get queue operations for {} from Valkey", queueName, e);
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public java.util.List<java.util.Map<String, Object>> getAllQueueOperations() {
    logger.info("All queue operations requested");
    try {
      // Cast to ValkeyService to access queue methods
      tech.aurora.bfadmin.service.impl.ValkeyServiceImpl valkeyServiceImpl = 
        (tech.aurora.bfadmin.service.impl.ValkeyServiceImpl) valkeyService;
      return valkeyServiceImpl.getAllQueueOperations();
    } catch (Exception e) {
      logger.error("Failed to get all queue operations from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  public java.util.List<java.util.Map<String, Object>> getDispatchedOperations() {
    logger.info("Dispatched operations requested");
    try {
      return valkeyService.getDispatchedOperations();
    } catch (Exception e) {
      logger.error("Failed to get dispatched operations from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }
}
