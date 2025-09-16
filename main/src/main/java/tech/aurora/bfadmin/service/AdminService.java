package tech.aurora.bfadmin.service;

public interface AdminService {

  String getHelloMessage();
  
  String getValkeyStatus();
  
  String getWorkerKeys();
  
  String getExecuteWorkersWithValues();
  
  String getStorageWorkersWithValues();
  
  java.util.List<java.util.Map<String, Object>> getExecuteWorkersTable();
  
  java.util.List<java.util.Map<String, Object>> getStorageWorkersTable();
  
  java.util.List<java.util.Map<String, Object>> getServersTable();
  
  java.util.Map<String, Object> getSystemStatus();
  
  // Queue-related methods
  java.util.List<String> getQueueNames();
  
  java.util.List<java.util.Map<String, Object>> getQueueOperations(String queueName);
  
  java.util.List<java.util.Map<String, Object>> getAllQueueOperations();
  
  java.util.List<java.util.Map<String, Object>> getDispatchedOperations();
  
  java.util.List<java.util.Map<String, Object>> getPrequeuedOperations();
}
