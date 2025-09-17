package tech.aurora.bfadmin.service.impl;

import tech.aurora.bfadmin.service.AdminService;
import tech.aurora.bfadmin.service.ValkeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.List;

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
      // Set<String> serverKeys = valkeyServiceImpl.getKeys("*server*"); // Disabled for performance
      logger.info("🔍 Keys containing 'server': Disabled for performance");
      
      // Set<String> capitalServerKeys = valkeyServiceImpl.getKeys("*Server*"); // Disabled for performance
      logger.info("🔍 Keys containing 'Server': Disabled for performance");
      
      // Set<String> allKeys = valkeyServiceImpl.getKeys("*"); // Disabled for performance
      // java.util.List<String> serverRelatedKeys = allKeys.stream() // Disabled
      logger.info("🔍 All server-related keys found: Using direct Servers key only");
      
      return valkeyServiceImpl.getServersAsTable("Servers");
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
      
      // Get server counts
      java.util.List<java.util.Map<String, Object>> servers = getServersTable();
      int serverCount = servers.size();
      status.put("serverCount", serverCount);
      
      // Count active servers
      int activeServer = (int) servers.stream()
        .filter(s -> s.get("status") != null && s.get("status").toString().startsWith("Active"))
        .count();
      
      status.put("activeServerCount", activeServer);
      status.put("serverClass", serverCount > 0 ? "success" : "warning");
      
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
  
  @Override
  public java.util.List<java.util.Map<String, Object>> getPrequeuedOperations() {
    logger.info("Prequeued operations requested");
    try {
      return valkeyService.getPrequeuedOperations();
    } catch (Exception e) {
      logger.error("Failed to get prequeued operations from Valkey", e);
      return new java.util.ArrayList<>();
    }
  }

  @Override
  public boolean deleteOperation(String queueName, String operationName) {
    logger.info("Delete operation requested for queue: {}, operation: {}", queueName, operationName);
    try {
      boolean deleted = false;
      int deletionAttempts = 0;
      
      // Based on the logs, operations are stored in sorted sets like {:2}cpu_queue_priority, {:0}cpu_queue_priority, etc.
      // Try to remove from queue sorted sets
      String[] queueKeys = {
        "{:0}" + queueName,
        "{:1}" + queueName,
        "{:2}" + queueName,
        "{:3}" + queueName,
        "{:4}" + queueName,
        "{:5}" + queueName
      };
      
      for (String queueKey : queueKeys) {
        if (valkeyService.hasKey(queueKey)) {
          logger.info("Checking sorted set key: {}", queueKey);
          
          // Get all values from the sorted set and find the one containing our operation name
          List<String> zsetValues = valkeyService.getZSetValues(queueKey);
          logger.info("Found {} values in sorted set {}", zsetValues.size(), queueKey);
          
          for (String zsetValue : zsetValues) {
            if (zsetValue.contains(operationName)) {
              logger.info("Found operation in sorted set, removing value: {}", zsetValue.substring(0, Math.min(100, zsetValue.length())) + "...");
              Long removed = valkeyService.removeFromZSet(queueKey, zsetValue);
              logger.info("Remove from sorted set {} returned: {}", queueKey, removed);
              if (removed > 0) {
                logger.info("Successfully removed operation from sorted set: {}", queueKey);
                deleted = true;
                deletionAttempts++;
                break; // Found and removed, no need to continue in this zset
              }
            }
          }
        } else {
          logger.debug("Sorted set key does not exist: {}", queueKey);
        }
      }
      
      // 1. Try to remove from prequeued operations list
      String prequeuedKey = "{Arrival}:PreQueuedOperations";
      if (valkeyService.hasKey(prequeuedKey)) {
        logger.info("Checking prequeued operations list: {}", prequeuedKey);
        List<String> items = valkeyService.getListAsString(prequeuedKey);
        logger.info("Found {} items in prequeued operations list", items.size());
        
        for (String item : items) {
          if (item.contains(operationName)) {
            logger.info("Found operation in prequeued list, removing: {}", item);
            Long removed = valkeyService.removeFromList(prequeuedKey, item);
            logger.info("Remove from list returned: {}", removed);
            if (removed > 0) {
              logger.info("Successfully removed operation from prequeued list");
              deleted = true;
              deletionAttempts++;
            }
          }
        }
      } else {
        logger.info("PreQueuedOperations key does not exist");
      }
      
      // 2. Try to remove from queued operations hash
      String queuedKey = "{Execution}:QueuedOperations";
      if (valkeyService.hasKey(queuedKey)) {
        logger.info("Checking queued operations hash: {}", queuedKey);
        Boolean removed = valkeyService.removeFromHash(queuedKey, operationName);
        logger.info("Remove from hash returned: {}", removed);
        if (removed) {
          logger.info("Successfully removed operation from queued operations hash");
          deleted = true;
          deletionAttempts++;
        }
      } else {
        logger.info("QueuedOperations hash key does not exist");
      }
      
      // 3. Try to remove from dispatched operations (by queue)
      String dispatchedKey = "{Execution}:DispatchedOperations:" + queueName;
      if (valkeyService.hasKey(dispatchedKey)) {
        logger.info("Checking dispatched operations hash: {}", dispatchedKey);
        Boolean removed = valkeyService.removeFromHash(dispatchedKey, operationName);
        logger.info("Remove from dispatched hash returned: {}", removed);
        if (removed) {
          logger.info("Successfully removed operation from dispatched operations hash");
          deleted = true;
          deletionAttempts++;
        }
      } else {
        logger.info("DispatchedOperations hash key does not exist for queue: {}", queueName);
      }
      
      // 4. Try direct key deletion as fallback
      String[] possibleKeys = {
        operationName,
        queueName + ":" + operationName,
        "{Arrival}:" + queueName + ":" + operationName,
        "{Execution}:" + queueName + ":" + operationName,
        "Operation:" + operationName,
        operationName + ":queued",
        operationName + ":operation"
      };
      
      for (String keyToTry : possibleKeys) {
        if (valkeyService.hasKey(keyToTry)) {
          logger.info("Found direct operation key: {}, attempting deletion", keyToTry);
          if (valkeyService.deleteKey(keyToTry)) {
            logger.info("Successfully deleted direct operation key: {}", keyToTry);
            deleted = true;
            deletionAttempts++;
          }
        }
      }
      
      logger.info("Deletion summary - attempts: {}, successful: {}, final result: {}", 
                  deletionAttempts, deleted, deleted);
      
      if (deleted) {
        logger.info("Successfully deleted operation: {} from queue: {}", operationName, queueName);
      } else {
        logger.warn("No matching keys found for operation: {} in queue: {}", operationName, queueName);
      }
      
      return deleted;
      
    } catch (Exception e) {
      logger.error("Failed to delete operation: {} from queue: {}", operationName, queueName, e);
      return false;
    }
  }
}
