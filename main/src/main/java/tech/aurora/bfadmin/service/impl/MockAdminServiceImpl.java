package tech.aurora.bfadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tech.aurora.bfadmin.service.AdminService;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "admin.service.mock", havingValue = "true", matchIfMissing = true)
public class MockAdminServiceImpl implements AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockAdminServiceImpl.class);
    
    public MockAdminServiceImpl() {
        logger.info("MockAdminServiceImpl initialized - using mock data for development");
    }
    
    @Override
    public String getHelloMessage() {
        return "Mock BuildFarm Admin Service - Development Mode";
    }
    
    @Override
    public String getValkeyStatus() {
        return "MOCK: Valkey cluster disabled for development";
    }
    
    @Override
    public String getWorkerKeys() {
        return "MOCK: worker_1, worker_2, worker_3";
    }
    
    @Override
    public String getExecuteWorkersWithValues() {
        return "MOCK: Execute workers mock data";
    }
    
    @Override
    public String getStorageWorkersWithValues() {
        return "MOCK: Storage workers mock data";
    }
    
    @Override
    public List<Map<String, Object>> getExecuteWorkersTable() {
        List<Map<String, Object>> workers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> worker = new HashMap<>();
            worker.put("name", "execute_worker_" + i);
            worker.put("status", "ACTIVE");
            worker.put("lastSeen", getRandomTimestamp(new Random(), 10));
            workers.add(worker);
        }
        return workers;
    }
    
    @Override
    public List<Map<String, Object>> getStorageWorkersTable() {
        List<Map<String, Object>> workers = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> worker = new HashMap<>();
            worker.put("name", "storage_worker_" + i);
            worker.put("status", "ACTIVE");
            worker.put("lastSeen", getRandomTimestamp(new Random(), 5));
            workers.add(worker);
        }
        return workers;
    }
    
    @Override
    public List<Map<String, Object>> getServersTable() {
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new HashMap<>();
        server.put("name", "mock_server");
        server.put("status", "RUNNING");
        server.put("port", "8980");
        servers.add(server);
        return servers;
    }
    
    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Basic system info
        status.put("mode", "DEVELOPMENT");
        
        // Valkey status (mock disabled)
        status.put("valkeyConnected", false);
        status.put("valkeyStatus", "Disabled (Mock Mode)");
        status.put("valkeyStatusClass", "warning");
        
        // Worker counts (mock data)
        status.put("executeWorkerCount", 3);
        status.put("storageWorkerCount", 2);
        status.put("activeExecuteCount", 3);
        status.put("activeStorageCount", 2);
        status.put("totalWorkerCount", 5);
        
        // System health
        status.put("systemHealthClass", "success");
        status.put("systemHealthText", "Healthy (Mock)");
        
        return status;
    }
    
    @Override
    public List<String> getQueueNames() {
        logger.debug("Getting mock queue names");
        return Arrays.asList(
            "prequeue",
            "cpu_queue",
            "gpu_queue",
            "memory_intensive_queue",
            "default_queue"
        );
    }
    
    @Override
    public List<Map<String, Object>> getQueueOperations(String queueName) {
        logger.debug("Getting mock operations for queue: {}", queueName);
        
        List<Map<String, Object>> operations = new ArrayList<>();
        Random random = new Random();
        
        // Generate mock operations based on queue type
        int operationCount = 5 + random.nextInt(15); // 5-20 operations
        
        for (int i = 1; i <= operationCount; i++) {
            Map<String, Object> operation = new HashMap<>();
            
            operation.put("name", String.format("operations/%s/op_%d", queueName, i));
            operation.put("stage", getRandomStage(random));
            operation.put("actionDigest", generateRandomDigest());
            operation.put("stdoutDigest", generateRandomDigest());
            operation.put("stderrDigest", generateRandomDigest());
            operation.put("profile", getRandomProfile(queueName));
            operation.put("platform", getRandomPlatform());
            operation.put("queuedTimestamp", getRandomTimestamp(random, 60));
            operation.put("workerName", String.format("worker_%s_%d", queueName, random.nextInt(10) + 1));
            operation.put("executeTimeout", String.format("%ds", 300 + random.nextInt(1200))); // 5-20 minutes
            operation.put("actionTimeout", String.format("%ds", 600 + random.nextInt(1800))); // 10-30 minutes
            
            operations.add(operation);
        }
        
        return operations;
    }
    
    @Override
    public List<Map<String, Object>> getAllQueueOperations() {
        logger.debug("Getting all queue operations (mock data)");
        
        List<Map<String, Object>> allOperations = new ArrayList<>();
        
        // Get operations from all queues
        for (String queueName : getQueueNames()) {
            List<Map<String, Object>> queueOperations = getQueueOperations(queueName);
            allOperations.addAll(queueOperations);
        }
        
        logger.debug("Returning {} total mock operations across all queues", allOperations.size());
        return allOperations;
    }
    
    private String getRandomStage(Random random) {
        String[] stages = {"QUEUED", "MATCH", "INPUT_FETCH_STAGE", "EXECUTING", "COMPLETED", "FAILED"};
        return stages[random.nextInt(stages.length)];
    }
    
    private String generateRandomDigest() {
        Random random = new Random();
        StringBuilder digest = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            digest.append(Integer.toHexString(random.nextInt(16)));
        }
        return digest.toString();
    }
    
    private String getRandomProfile(String queueName) {
        if (queueName.contains("cpu")) {
            return "cpu-intensive";
        } else if (queueName.contains("gpu")) {
            return "gpu-enabled";
        } else if (queueName.contains("memory")) {
            return "high-memory";
        } else {
            return "default";
        }
    }
    
    private String getRandomPlatform() {
        String[] platforms = {
            "container-image=docker://ubuntu:20.04",
            "container-image=docker://openjdk:11-jre-slim",
            "container-image=docker://gcc:9",
            "OSFamily=linux"
        };
        Random random = new Random();
        return platforms[random.nextInt(platforms.length)];
    }
    
    private String getRandomTimestamp(Random random, int maxMinutesAgo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime randomTime = now.minusMinutes(random.nextInt(maxMinutesAgo));
        return randomTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
