package tech.aurora.bfadmin.service;

public interface AdminService {

  String getHelloMessage();
  
  String getValkeyStatus();
  
  String getWorkerKeys();
  
  String getExecuteWorkersWithValues();
  
  String getStorageWorkersWithValues();
  
  java.util.List<java.util.Map<String, Object>> getExecuteWorkersTable();
  
  java.util.List<java.util.Map<String, Object>> getStorageWorkersTable();
  
  java.util.Map<String, Object> getSystemStatus();
}
