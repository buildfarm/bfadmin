package tech.aurora.bfadmin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import tech.aurora.bfadmin.service.ValkeyService;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ValkeyServiceImpl implements ValkeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValkeyServiceImpl.class);
    
    @Autowired
    private RedisTemplate<String, Object> valkeyTemplate;
    
    @Value("${valkey.cluster.nodes:localhost:7000}")
    private String clusterNodes;
    

    
    @Value("${buildfarm.queues.key.pattern:{Execution}:QueuedOperations}")
    private String queueKeyPattern;
    
    /**
     * Test the connection to Valkey cluster
     */
    @Override
    public boolean testConnection() {
        try {
            valkeyTemplate.opsForValue().set("test:connection", "ping", 10, TimeUnit.SECONDS);
            String result = (String) valkeyTemplate.opsForValue().get("test:connection");
            boolean isConnected = "ping".equals(result);
            logger.info("Valkey connection test: {}", isConnected ? "SUCCESS" : "FAILED");
            return isConnected;
        } catch (Exception e) {
            logger.error("Valkey connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get cluster information
     */
    @Override
    public String getClusterInfo() {
        try {
            // Get some basic info about the cluster
            Set<String> keys = valkeyTemplate.keys("*");
            int keyCount = keys != null ? keys.size() : 0;
            
            StringBuilder info = new StringBuilder();
            info.append("Cluster Nodes: ").append(clusterNodes).append("\n");
            info.append("Connection Status: ").append(testConnection() ? "Connected" : "Disconnected").append("\n");
            info.append("Total Keys: ").append(keyCount).append("\n");
            
            return info.toString();
        } catch (Exception e) {
            logger.error("Failed to get Valkey cluster info", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Store a key-value pair
     */
    @Override
    public void setValue(String key, Object value) {
        try {
            valkeyTemplate.opsForValue().set(key, value);
            logger.debug("Stored value in Valkey: {}", key);
        } catch (Exception e) {
            logger.error("Failed to store value in Valkey: {}", key, e);
            throw new RuntimeException("Failed to store value in Valkey", e);
        }
    }
    
    /**
     * Store a key-value pair with expiration
     */
    @Override
    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        try {
            valkeyTemplate.opsForValue().set(key, value, timeout, unit);
            logger.debug("Stored value in Valkey with expiration: {}", key);
        } catch (Exception e) {
            logger.error("Failed to store value with expiration in Valkey: {}", key, e);
            throw new RuntimeException("Failed to store value with expiration in Valkey", e);
        }
    }
    
    /**
     * Get a value by key
     */
    @Override
    public Object getValue(String key) {
        try {
            Object value = valkeyTemplate.opsForValue().get(key);
            logger.debug("Retrieved value from Valkey: {}", key);
            return value;
        } catch (Exception e) {
            logger.error("Failed to retrieve value from Valkey: {}", key, e);
            return null;
        }
    }
    
    /**
     * Delete a key
     */
    @Override
    public boolean deleteKey(String key) {
        try {
            Boolean deleted = valkeyTemplate.delete(key);
            logger.debug("Deleted key from Valkey: {}", key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            logger.error("Failed to delete key from Valkey: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get all keys matching a pattern
     */
    @Override
    public Set<String> getKeys(String pattern) {
        try {
            Set<String> keys = valkeyTemplate.keys(pattern);
            logger.debug("Retrieved {} keys from Valkey with pattern: {}", keys != null ? keys.size() : 0, pattern);
            return keys;
        } catch (Exception e) {
            logger.error("Failed to retrieve keys from Valkey with pattern: {}", pattern, e);
            return Set.of();
        }
    }
    
    /**
     * Check if key exists
     */
    @Override
    public boolean hasKey(String key) {
        try {
            Boolean exists = valkeyTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Failed to check key existence in Valkey: {}", key, e);
            return false;
        }
    }
    
    /**
     * Get the type of a Redis key
     */
    public String getKeyType(String key) {
        try {
            return valkeyTemplate.type(key).code();
        } catch (Exception e) {
            logger.error("Failed to get key type for: {}", key, e);
            return "none";
        }
    }
    
    /**
     * Get value handling different Redis data types
     */
    public Object getValueByType(String key) {
        try {
            String keyType = getKeyType(key);
            
            switch (keyType) {
                case "string":
                    return valkeyTemplate.opsForValue().get(key);
                    
                case "hash":
                    // Use raw operations to avoid Jackson serialization issues
                    return valkeyTemplate.execute((org.springframework.data.redis.core.RedisCallback<java.util.Map<String, String>>) connection -> {
                        try {
                            byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            java.util.Map<byte[], byte[]> rawHash = connection.hGetAll(keyBytes);
                            java.util.Map<String, String> result = new java.util.HashMap<>();
                            if (rawHash != null) {
                                rawHash.forEach((k, v) -> {
                                    String keyStr = new String(k, java.nio.charset.StandardCharsets.UTF_8);
                                    String valueStr = new String(v, java.nio.charset.StandardCharsets.UTF_8);
                                    result.put(keyStr, valueStr);
                                });
                            }
                            return result;
                        } catch (Exception e) {
                            logger.error("Error retrieving hash for key: {}", key, e);
                            return new java.util.HashMap<>();
                        }
                    });
                    
                case "list":
                    return valkeyTemplate.opsForList().range(key, 0, -1);
                    
                case "set":
                    return valkeyTemplate.opsForSet().members(key);
                    
                case "zset":
                    return valkeyTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                    
                case "none":
                    logger.warn("Key does not exist: {}", key);
                    return null;
                    
                default:
                    logger.warn("Unknown key type '{}' for key: {}", keyType, key);
                    return null;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve value by type from Valkey: {}", key, e);
            return null;
        }
    }
    
    /**
     * Get a formatted string representation of a key's value regardless of type
     */
    public String getFormattedValue(String key) {
        try {
            String keyType = getKeyType(key);
            Object value = getValueByType(key);
            
            if (value == null) {
                return "[Key not found or empty]";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Type: ").append(keyType.toUpperCase()).append("\n");
            
            switch (keyType) {
                case "string":
                    result.append("Value: ").append(value);
                    break;
                    
                case "hash":
                    java.util.Map<?, ?> hashValue = (java.util.Map<?, ?>) value;
                    result.append("Hash entries (").append(hashValue.size()).append("):\n");
                    hashValue.forEach((k, v) -> 
                        result.append("  ").append(k).append(" → ").append(v).append("\n"));
                    break;
                    
                case "list":
                    java.util.List<?> listValue = (java.util.List<?>) value;
                    result.append("List items (").append(listValue.size()).append("):\n");
                    for (int i = 0; i < listValue.size(); i++) {
                        result.append("  [").append(i).append("] ").append(listValue.get(i)).append("\n");
                    }
                    break;
                    
                case "set":
                    java.util.Set<?> setValue = (java.util.Set<?>) value;
                    result.append("Set members (").append(setValue.size()).append("):\n");
                    setValue.forEach(member -> 
                        result.append("  • ").append(member).append("\n"));
                    break;
                    
                case "zset":
                    java.util.Set<?> zsetValue = (java.util.Set<?>) value;
                    result.append("Sorted set entries (").append(zsetValue.size()).append("):\n");
                    zsetValue.forEach(entry -> 
                        result.append("  ").append(entry).append("\n"));
                    break;
                    
                default:
                    result.append("Raw value: ").append(value);
                    break;
            }
            
            return result.toString();
        } catch (Exception e) {
            logger.error("Failed to format value for key: {}", key, e);
            return "Error formatting value: " + e.getMessage();
        }
    }
    
    /**
     * Get individual workers from the Workers_execute or Workers_storage hash structures
     */
    public java.util.List<java.util.Map<String, Object>> getWorkersAsTable(String pattern) {
        java.util.List<java.util.Map<String, Object>> workers = new java.util.ArrayList<>();
        
        try {
            Set<String> workerKeys = getKeys(pattern);
            logger.info("Found {} keys matching pattern: {}", workerKeys.size(), pattern);
            
            for (String key : workerKeys) {
                try {
                    String keyType = getKeyType(key);
                    logger.info("Processing key: {} of type: {}", key, keyType);
                    
                    if ("hash".equals(keyType)) {
                        // Get all hash fields - these should be individual worker entries
                        Object hashData = getValueByType(key);
                        if (hashData instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, String> hash = (java.util.Map<String, String>) hashData;
                            
                            logger.info("Hash contains {} entries", hash.size());
                            
                            // Each hash field represents a worker
                            for (java.util.Map.Entry<String, String> entry : hash.entrySet()) {
                                String workerKey = entry.getKey();
                                String workerJson = entry.getValue();
                                
                                try {
                                    // Parse the JSON for each individual worker
                                    java.util.Map<String, Object> workerData = parseWorkerJson(workerKey, workerJson);
                                    if (workerData != null) {
                                        workers.add(workerData);
                                    }
                                } catch (Exception e) {
                                    logger.error("Error parsing worker JSON for key {}: {}", workerKey, workerJson, e);
                                    // Add error entry for this specific worker
                                    java.util.Map<String, Object> errorData = new java.util.HashMap<>();
                                    errorData.put("workerId", workerKey);
                                    errorData.put("endpoint", "Parse Error");
                                    errorData.put("expireAt", "N/A");
                                    errorData.put("workerType", "Error");
                                    errorData.put("firstRegisteredAt", "N/A");
                                    errorData.put("status", "Error");
                                    workers.add(errorData);
                                }
                            }
                        }
                    } else if ("string".equals(keyType)) {
                        // If it's a string, try to parse as JSON directly
                        Object stringData = getValueByType(key);
                        if (stringData != null) {
                            java.util.Map<String, Object> workerData = parseWorkerJson(key, stringData.toString());
                            if (workerData != null) {
                                workers.add(workerData);
                            }
                        }
                    } else {
                        logger.warn("Unexpected key type '{}' for worker key: {}", keyType, key);
                    }
                } catch (Exception e) {
                    logger.error("Error processing worker key: {}", key, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get workers as table for pattern: {}", pattern, e);
        }
        
        logger.info("Returning {} workers for pattern: {}", workers.size(), pattern);
        return workers;
    }
    
    public java.util.List<java.util.Map<String, Object>> getServersAsTable(String pattern) {
        java.util.List<java.util.Map<String, Object>> servers = new java.util.ArrayList<>();
        
        try {
            // Use exact key instead of pattern matching
            String serverKey = pattern; // pattern is now the exact key name
            java.util.Set<String> serverKeys = java.util.Set.of(serverKey);
            logger.info("🔍 Fetching Servers keys from Redis with pattern: {}", pattern);
            logger.info("🔑 Found {} server keys matching pattern '{}': {}", serverKeys.size(), pattern, serverKeys);
            
            
            // Check if the key exists before processing
            if (!hasKey(serverKey)) {
                logger.warn("Servers key does not exist: {}", serverKey);
                return servers;
            }
            for (String key : serverKeys) {
                try {
                    String keyType = getKeyType(key);
                    logger.info("Processing key: {} of type: {}", key, keyType);
                    
                    if ("hash".equals(keyType)) {
                        // Get all hash fields - these should be individual server entries
                        Object hashData = getValueByType(key);
                        if (hashData instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, String> hash = (java.util.Map<String, String>) hashData;
                            
                            logger.info("Hash contains {} entries", hash.size());
                            
                            // Each hash field represents a server
                            for (java.util.Map.Entry<String, String> entry : hash.entrySet()) {
                                String serverId = entry.getKey();
                                String serverJson = entry.getValue();
                                
                                try {
                                    logger.info("📄 Server JSON for key '{}': {}", serverId, serverJson);
                                    // Parse the JSON for each individual server
                                    java.util.Map<String, Object> serverData = parseServerJson(serverId, serverJson);
                                    if (serverData != null) {
                                        servers.add(serverData);
                                        logger.info("✅ Successfully parsed server: {}", serverData);
                                    }
                                } catch (Exception e) {
                                    logger.error("Error parsing server JSON for key {}: {}", serverId, serverJson, e);
                                    // Add error entry for this specific server
                                    java.util.Map<String, Object> errorData = new java.util.HashMap<>();
                                    errorData.put("serverId", serverId);
                                    errorData.put("endpoint", "Parse Error");
                                    errorData.put("expireAt", "N/A");
                                    errorData.put("serverType", "Error");
                                    errorData.put("firstRegisteredAt", "N/A");
                                    errorData.put("status", "Error");
                                    servers.add(errorData);
                                }
                            }
                        }
                    } else if ("string".equals(keyType)) {
                        // If it's a string, try to parse as JSON directly
                        Object stringData = getValueByType(key);
                        if (stringData != null) {
                            logger.info("📄 Server string data for key '{}': {}", key, stringData.toString());
                            java.util.Map<String, Object> serverData = parseServerJson(key, stringData.toString());
                            if (serverData != null) {
                                servers.add(serverData);
                                logger.info("✅ Successfully parsed server from string: {}", serverData);
                            }
                        }
                    } else {
                        logger.warn("Unexpected key type '{}' for server key: {}", keyType, key);
                    }
                } catch (Exception e) {
                    logger.error("Error processing server key: {}", key, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get servers as table for pattern: {}", pattern, e);
        }
        
        logger.info("Returning {} servers for pattern: {}", servers.size(), pattern);
        return servers;
    }
    
    /**
     * Parse individual worker JSON data
     */
    private java.util.Map<String, Object> parseWorkerJson(String workerId, String workerJson) {
        try {
            logger.info("Worker json: {}", workerJson);
            // Use simple JSON parsing for the worker data
            java.util.Map<String, Object> workerData = new java.util.HashMap<>();
            workerData.put("workerId", workerId);
            
            if (workerJson != null && !workerJson.trim().isEmpty()) {
                // Simple JSON parsing to extract fields
                String endpoint = extractJsonField(workerJson, "endpoint");
                String expireAt = extractJsonField(workerJson, "expireAt");
                String workerType = extractJsonField(workerJson, "workerType");
                String firstRegisteredAt = extractJsonField(workerJson, "firstRegisteredAt");
                String groupName = extractJsonField(workerJson, "groupName");
                
                workerData.put("endpoint", endpoint != null ? endpoint : "N/A");
                workerData.put("expireAt", formatTimestamp(expireAt));
                workerData.put("workerType", parseWorkerType(workerType));
                workerData.put("firstRegisteredAt", formatTimestamp(firstRegisteredAt));
                workerData.put("groupName", groupName != null ? groupName : "default");
                workerData.put("status", calculateStatus(expireAt));
                
                logger.debug("Parsed worker {}: endpoint={}, type={}, group={}, status={}", 
                           workerId, endpoint, workerType, groupName, workerData.get("status"));
            } else {
                workerData.put("endpoint", "N/A");
                workerData.put("expireAt", "N/A");
                workerData.put("workerType", "Unknown");
                workerData.put("firstRegisteredAt", "N/A");
                workerData.put("groupName", "default");
                workerData.put("status", "Unknown");
            }
            
            return workerData;
        } catch (Exception e) {
            logger.error("Error parsing worker JSON for {}: {}", workerId, workerJson, e);
            return null;
        }
    }
    
    /**
     * Parse individual server JSON data
     */
    private java.util.Map<String, Object> parseServerJson(String serverId, String serverJson) {
        try {
            logger.info("Server json: {}", serverJson);
            // Use simple JSON parsing for the server data
            java.util.Map<String, Object> serverData = new java.util.HashMap<>();
            serverData.put("serverId", serverId);
            
            if (serverJson != null && !serverJson.trim().isEmpty()) {
                // Simple JSON parsing to extract fields - servers have different field names
                String name = extractJsonField(serverJson, "name");
                String type = extractJsonField(serverJson, "type");
                String firstRegisteredAt = extractJsonField(serverJson, "firstRegisteredAt");
                String lastRegisteredAt = extractJsonField(serverJson, "lastRegisteredAt");
                
                // Map server fields to table format
                serverData.put("endpoint", name != null ? name : serverId); // Use name or serverId as endpoint
                serverData.put("serverType", mapServerType(type));
                serverData.put("firstRegisteredAt", formatTimestamp(firstRegisteredAt));
                serverData.put("expireAt", lastRegisteredAt != null ? formatTimestamp(lastRegisteredAt) : "N/A");
                serverData.put("groupName", "default"); // Servers don't seem to have groups
                serverData.put("status", "Active"); // Servers in the registry are considered active
                
                logger.debug("Parsed server {}: name={}, type={}, firstReg={}, lastReg={}", 
                           serverId, name, type, firstRegisteredAt, lastRegisteredAt);
            } else {
                serverData.put("endpoint", serverId);
                serverData.put("serverType", "Unknown");
                serverData.put("firstRegisteredAt", "N/A");
                serverData.put("expireAt", "N/A");
                serverData.put("groupName", "default");
                serverData.put("status", "Unknown");
            }
            
            return serverData;
        } catch (Exception e) {
            logger.error("Error parsing server JSON for {}: {}", serverId, serverJson, e);
            return null;
        }
    }
    
    /**
     * Map server type from Redis data to display format
     */
    private String mapServerType(String type) {
        if (type == null) {
            return "Unknown";
        }
        
        switch (type.toLowerCase()) {
            case "shard":
                return "Scheduler";
            case "cas":
                return "CAS";
            case "buildfarm":
                return "BuildFarm";
            default:
                return type; // Return as-is if not recognized
        }
    }
    
    /**
     * Extract a field value from JSON string using simple parsing
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"?([^\"\\s,}]+)\"?";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1).replaceAll("\"", "");
            }
        } catch (Exception e) {
            logger.warn("Failed to extract field '{}' from JSON: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Format timestamp from milliseconds to readable format
     */
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return "N/A";
        }
        
        try {
            long millis = Long.parseLong(timestamp);
            java.time.Instant instant = java.time.Instant.ofEpochMilli(millis);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault());
            return formatter.format(instant);
        } catch (Exception e) {
            return timestamp; // Return original if parsing fails
        }
    }
    
    /**
     * Format current timestamp in the same format as other timestamps
     */
    private String formatCurrentTimestamp() {
        java.time.Instant instant = java.time.Instant.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    /**
     * Parse worker type number to readable string
     */
    private String parseWorkerType(String workerType) {
        if (workerType == null || workerType.trim().isEmpty()) {
            return "Unknown";
        }
        
        try {
            int type = Integer.parseInt(workerType);
            switch (type) {
                case 1:
                    return "Execute";
                case 2:
                    return "Storage";
                default:
                    return "Type " + type;
            }
        } catch (Exception e) {
            return workerType;
        }
    }
    
    /**
     * Calculate worker status based on expiration time
     */
    private String calculateStatus(String expireAt) {
        if (expireAt == null || expireAt.trim().isEmpty()) {
            return "Unknown";
        }
        
        try {
            long expireMillis = Long.parseLong(expireAt);
            long currentMillis = System.currentTimeMillis();
            
            if (expireMillis > currentMillis) {
                return "Active";
            } else {
                return "Expired";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Get all queue names dynamically from Redis using *_queue_* pattern
     */
    public java.util.List<String> getQueueNames() {
        java.util.List<String> dynamicQueueNames = new java.util.ArrayList<>();
        
        try {
            logger.info("Discovering queue names from Redis using *_queue_* pattern");
            
            // Step 1: Find all keys matching *_queue_* pattern
            java.util.Set<String> queueKeys = getKeys("*_queue_*");
            logger.info("Found {} keys matching *_queue_* pattern: {}", queueKeys.size(), queueKeys);
            
            // Step 2: Extract unique queue names from the discovered keys
            java.util.Set<String> uniqueQueueNames = new java.util.HashSet<>();
            for (String key : queueKeys) {
                String queueName = extractQueueNameFromRedisKey(key);
                if (queueName != null && !queueName.isEmpty()) {
                    uniqueQueueNames.add(queueName);
                }
            }
            
            // Step 3: Convert to sorted list for consistent ordering
            dynamicQueueNames.addAll(uniqueQueueNames);
            java.util.Collections.sort(dynamicQueueNames);
            
            logger.info("Discovered {} unique queue names from Redis: {}", dynamicQueueNames.size(), dynamicQueueNames);
            
            // If no queues found in Redis, return empty list
            if (dynamicQueueNames.isEmpty()) {
                logger.warn("No queues found in Redis with *_queue_* pattern, returning empty queue list");
            }
            
        } catch (Exception e) {
            logger.error("Failed to discover queue names from Redis, returning empty queue list", e);
            dynamicQueueNames = new java.util.ArrayList<>();
        }
        
        return dynamicQueueNames;
    }

    /**
     * Get operations in a specific queue using configured key pattern
     */
    public java.util.List<java.util.Map<String, Object>> getQueueOperations(String queueName) {
        java.util.List<java.util.Map<String, Object>> operations = new java.util.ArrayList<>();
        
        try {
            logger.info("Getting operations for queue: {} using configured pattern", queueName);
            
            // Use the configured key pattern - for most queues, we'll use the standard pattern
            String queueKey;
            if ("prequeue".equals(queueName)) {
                // Special case for prequeue
                queueKey = "{Arrival}:PreQueuedOperations";
            } else {
                // Use configured pattern without appending queue name
                queueKey = queueKeyPattern;
            }
            
            logger.info("Looking for queue key: {}", queueKey);
            
            // Debug: Let's see what keys exist that might be related to queues
            java.util.Set<String> allKeys = getKeys("*Queue*");
            logger.info("All keys containing 'Queue': {}", allKeys);
            
            // Let's also check for keys with the queue name
            java.util.Set<String> queueRelatedKeys = getKeys("*" + queueName + "*");
            logger.info("Keys containing queue name '{}': {}", queueName, queueRelatedKeys);
            
            if (hasKey(queueKey)) {
                String keyType = getKeyType(queueKey);
                logger.info("Processing queue key: {} of type: {}", queueKey, keyType);
                
                if ("list".equals(keyType)) {
                    // Handle list-type queue
                    Object listData = getValueByType(queueKey);
                    if (listData instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> list = (java.util.List<String>) listData;
                        
                        for (int i = 0; i < list.size(); i++) {
                            String operationData = list.get(i);
                            java.util.Map<String, Object> operation = parseOperationData(operationData, i);
                            if (operation != null) {
                                operations.add(operation);
                            }
                        }
                    }
                } else if ("hash".equals(keyType)) {
                    // Handle hash-type queue
                    Object hashData = getValueByType(queueKey);
                    if (hashData instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, String> hash = (java.util.Map<String, String>) hashData;
                        
                        for (java.util.Map.Entry<String, String> entry : hash.entrySet()) {
                            String operationId = entry.getKey();
                            String operationData = entry.getValue();
                            java.util.Map<String, Object> operation = parseOperationData(operationData, operations.size());
                            if (operation != null) {
                                operation.put("operationId", operationId);
                                operations.add(operation);
                            }
                        }
                    }
                } else if ("string".equals(keyType)) {
                    // Handle single string value
                    Object stringData = getValueByType(queueKey);
                    if (stringData != null) {
                        java.util.Map<String, Object> operation = parseOperationData(stringData.toString(), 0);
                        if (operation != null) {
                            operations.add(operation);
                        }
                    }
                }
            } else {
                logger.warn("Queue key does not exist: {}", queueKey);
            }
            
            logger.info("Returning {} operations for queue: {}", operations.size(), queueName);
            
        } catch (Exception e) {
            logger.error("Failed to get operations for queue: {}", queueName, e);
        }
        
        return operations;
    }

    /**
     * Parse operation data from JSON string and extract fields for UI display
     */
    private java.util.Map<String, Object> parseOperationData(String operationData, int index) {
        try {
            java.util.Map<String, Object> operation = new java.util.HashMap<>();
            
            if (operationData != null && !operationData.trim().isEmpty()) {
                // Handle buildfarm's timestamp:json format first
                String jsonData = operationData;
                logger.debug("Original operation data: {}", operationData.length() > 100 ? operationData.substring(0, 100) + "..." : operationData);
                
                // Check for timestamp:json format (timestamp can be followed by newlines/whitespace before JSON)
                if (operationData.matches("^\\d+:.*")) {
                    int colonIndex = operationData.indexOf(':');
                    jsonData = operationData.substring(colonIndex + 1).trim();
                    logger.debug("Stripped timestamp prefix from operation data, new length: {}", jsonData.length());
                    logger.debug("First 100 chars of stripped JSON: {}", jsonData.length() > 100 ? jsonData.substring(0, 100) : jsonData);
                } else {
                    logger.debug("No timestamp prefix detected in operation data");
                }
                
                try {
                    // Clean and trim the JSON data
                    jsonData = jsonData.trim();
                    
                    // Additional validation and cleanup
                    if (!jsonData.startsWith("{")) {
                        throw new IllegalArgumentException("JSON data does not start with {");
                    }
                    if (!jsonData.endsWith("}")) {
                        logger.warn("JSON data appears to be truncated - does not end with }. Length: {}, ends with: '{}'", 
                                   jsonData.length(), 
                                   jsonData.length() > 50 ? jsonData.substring(jsonData.length() - 50) : jsonData);
                        throw new IllegalArgumentException("JSON data appears to be truncated");
                    }
                    
                    // Parse the JSON data properly for buildfarm structure
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    // Use more strict settings first
                    mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> parsedJsonData = mapper.readValue(jsonData, java.util.Map.class);
                    
                    logger.debug("Successfully parsed JSON, keys: {}", parsedJsonData.keySet());
                    
                    // Extract from buildfarm executeEntry structure
                    Object executeEntry = parsedJsonData.get("executeEntry");
                    logger.debug("executeEntry object: {}", executeEntry);
                    
                    if (executeEntry instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> executeEntryMap = (java.util.Map<String, Object>) executeEntry;
                        
                        // Extract operationName from executeEntry
                        String operationName = (String) executeEntryMap.get("operationName");
                        logger.debug("Extracted operationName from executeEntry: '{}'", operationName);
                        
                        // Extract actionDigest from executeEntry
                        Object actionDigestObj = executeEntryMap.get("actionDigest");
                        String actionDigestHash = null;
                        if (actionDigestObj instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> actionDigestMap = (java.util.Map<String, Object>) actionDigestObj;
                            actionDigestHash = (String) actionDigestMap.get("hash");
                        }
                        
                        // Extract platform information if available
                        String platform = "linux"; // default
                        Object platformObj = executeEntryMap.get("platform");
                        if (platformObj != null) {
                            if (platformObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> platformMap = (java.util.Map<String, Object>) platformObj;
                                // Try to extract platform properties
                                Object osProperty = platformMap.get("properties");
                                if (osProperty instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> properties = (java.util.Map<String, Object>) osProperty;
                                    Object osName = properties.get("OSFamily");
                                    if (osName != null) {
                                        platform = osName.toString().toLowerCase();
                                    }
                                } else {
                                    // If platform is just a map without properties, try to get a string representation
                                    platform = platformMap.toString();
                                }
                            } else {
                                // If platform is a string or other type
                                platform = platformObj.toString();
                            }
                        }
                        
                        // Extract request metadata for additional info
                        Object requestMetadataObj = executeEntryMap.get("requestMetadata");
                        String toolName = null;
                        String toolVersion = null;
                        if (requestMetadataObj instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> requestMetadata = (java.util.Map<String, Object>) requestMetadataObj;
                            Object toolDetailsObj = requestMetadata.get("toolDetails");
                            if (toolDetailsObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> toolDetails = (java.util.Map<String, Object>) toolDetailsObj;
                                toolName = (String) toolDetails.get("toolName");
                                toolVersion = (String) toolDetails.get("toolVersion");
                            }
                        }
                        
                        // Extract timestamp from the original timestamp:json format
                        String queuedTimestamp = null;
                        if (operationData.matches("^\\d+:.*")) {
                            int colonIndex = operationData.indexOf(':');
                            String timestampStr = operationData.substring(0, colonIndex);
                            try {
                                long timestamp = Long.parseLong(timestampStr);
                                queuedTimestamp = java.time.Instant.ofEpochMilli(timestamp).toString();
                            } catch (NumberFormatException e) {
                                logger.debug("Failed to parse timestamp from operation data: {}", timestampStr);
                            }
                        }
                        
                        // Determine worker name and status - for queue operations, they are queued
                        String workerName = "unknown-worker";
                        if (toolName != null) {
                            workerName = toolName + (toolVersion != null ? "-" + toolVersion : "");
                        }
                        
                        // Set the extracted values
                        operation.put("index", index + 1);
                        operation.put("operationName", operationName != null ? operationName : "operation-" + (index + 1));
                        operation.put("name", operationName != null ? operationName : "operation-" + (index + 1));
                        operation.put("stage", "QUEUED"); // For queue operations, they are typically in QUEUED state
                        operation.put("status", "Queued");
                        operation.put("queuedAt", queuedTimestamp != null ? queuedTimestamp : formatCurrentTimestamp());
                        operation.put("queuedTimestamp", queuedTimestamp != null ? queuedTimestamp : formatCurrentTimestamp());
                        operation.put("rawData", operationData.length() > 200 ? operationData.substring(0, 200) + "..." : operationData);
                        
                        // Add digest fields expected by the template
                        String finalActionDigest = (actionDigestHash != null && actionDigestHash.length() >= 8) ? actionDigestHash : generatePlaceholderDigest();
                        operation.put("actionDigest", finalActionDigest);
                        operation.put("stdoutDigest", null);
                        operation.put("stderrDigest", null);
                        operation.put("platform", platform);
                        operation.put("workerName", workerName);
                        
                        // Generate operationId from the operationName or use a fallback
                        String operationId = operationName != null ? operationName : "op-" + (index + 1);
                        operation.put("operationId", operationId);
                        
                        operation.put("hasMetadata", executeEntryMap.containsKey("requestMetadata"));
                        operation.put("hasExecuteResponse", false); // Queue operations typically don't have responses yet
                        
                        logger.debug("Successfully parsed buildfarm executeEntry: operationName='{}', actionDigest='{}'", 
                                   operationName, actionDigestHash);
                        return operation;
                    } else {
                        logger.warn("executeEntry is not a Map, type: {}", executeEntry != null ? executeEntry.getClass() : "null");
                    }
                } catch (Exception jsonException) {
                    logger.warn("Failed to parse JSON operation data, falling back to string extraction: {}", jsonException.getMessage());
                    logger.debug("Failed JSON data length: {}, starts with: '{}', ends with: '{}'", 
                               jsonData.length(), 
                               jsonData.length() > 10 ? jsonData.substring(0, 10) : jsonData,
                               jsonData.length() > 10 ? jsonData.substring(Math.max(0, jsonData.length() - 10)) : jsonData);
                    
                    // Check if JSON was truncated
                    if (jsonException.getMessage() != null && jsonException.getMessage().contains("truncated")) {
                        logger.warn("JSON appears to be truncated - this suggests the raw data from Redis was incomplete");
                    }
                    
                    // Log first 500 characters to see more structure
                    logger.debug("First 500 chars of failed JSON: {}", jsonData.length() > 500 ? jsonData.substring(0, 500) + "..." : jsonData);
                }
                
                // Fallback to improved string extraction for buildfarm format
                String operationName = extractBuildfarmOperationName(jsonData);
                String stage = extractJsonField(jsonData, "stage");
                String requestMetadata = extractJsonField(jsonData, "requestMetadata");
                String executeResponse = extractJsonField(jsonData, "executeResponse");
                String actionDigest = extractBuildfarmActionDigest(jsonData);
                String stdoutDigest = extractJsonField(jsonData, "stdoutDigest");
                String stderrDigest = extractJsonField(jsonData, "stderrDigest");
                String platform = extractJsonField(jsonData, "platform");
                String workerName = extractJsonField(jsonData, "workerName");
                
                // Extract timestamp from the original timestamp:json format for fallback too
                String queuedTimestamp = null;
                if (operationData.matches("^\\d+:.*")) {
                    int colonIndex = operationData.indexOf(':');
                    String timestampStr = operationData.substring(0, colonIndex);
                    try {
                        long timestamp = Long.parseLong(timestampStr);
                        queuedTimestamp = java.time.Instant.ofEpochMilli(timestamp).toString();
                    } catch (NumberFormatException e) {
                        logger.debug("Failed to parse timestamp from operation data in fallback: {}", timestampStr);
                    }
                }
                
                // Extract tool information from requestMetadata for better worker name
                String toolName = extractJsonField(jsonData, "toolName");
                if (workerName == null && toolName != null) {
                    workerName = toolName;
                }
                
                operation.put("index", index + 1);
                operation.put("operationName", operationName != null ? operationName : "operation-" + (index + 1));
                operation.put("name", operationName != null ? operationName : "operation-" + (index + 1));
                operation.put("stage", stage != null ? stage : "QUEUED");
                operation.put("status", stage != null ? determineOperationStatus(stage, operationData) : "Queued");
                operation.put("queuedAt", queuedTimestamp != null ? queuedTimestamp : formatCurrentTimestamp());
                operation.put("queuedTimestamp", queuedTimestamp != null ? queuedTimestamp : formatCurrentTimestamp());
                operation.put("rawData", operationData.length() > 200 ? operationData.substring(0, 200) + "..." : operationData);
                
                // Add digest fields expected by the template
                String finalActionDigest = (actionDigest != null && actionDigest.length() >= 8) ? actionDigest : generatePlaceholderDigest();
                operation.put("actionDigest", finalActionDigest);
                operation.put("stdoutDigest", (stdoutDigest != null && stdoutDigest.length() >= 8) ? stdoutDigest : null);
                operation.put("stderrDigest", (stderrDigest != null && stderrDigest.length() >= 8) ? stderrDigest : null);
                // Clean up platform field - avoid showing just opening brace
                String cleanPlatform = "linux"; // default
                if (platform != null && !platform.trim().isEmpty() && !platform.equals("{") && !platform.equals("{}")) {
                    cleanPlatform = platform.trim();
                }
                operation.put("platform", cleanPlatform);
                operation.put("workerName", workerName != null ? workerName : "unknown-worker");
                
                // Ensure operationId field is present for template
                if (!operation.containsKey("operationId")) {
                    operation.put("operationId", "op-" + (index + 1));
                }
                
                // Extract additional metadata if available
                operation.put("hasMetadata", requestMetadata != null);
                operation.put("hasExecuteResponse", executeResponse != null);
                
                logger.debug("Used fallback parsing for operation data");
                
            } else {
                // Empty or null data
                operation.put("index", index + 1);
                operation.put("operationName", "empty-operation-" + (index + 1));
                operation.put("name", "empty-operation-" + (index + 1));
                operation.put("stage", "EMPTY");
                operation.put("status", "Empty");
                operation.put("queuedAt", "N/A");
                operation.put("queuedTimestamp", "N/A");
                operation.put("rawData", "");
                operation.put("hasMetadata", false);
                operation.put("hasExecuteResponse", false);
                operation.put("actionDigest", generatePlaceholderDigest());
                operation.put("stdoutDigest", null);
                operation.put("stderrDigest", null);
                operation.put("platform", "linux");
                operation.put("workerName", "unknown-worker");
                operation.put("operationId", "empty-op-" + (index + 1));
            }
            
            return operation;
            
        } catch (Exception e) {
            logger.error("Error parsing operation data: {}", operationData, e);
            return null;
        }
    }

    /**
     * Generate a placeholder digest for operations that don't have one
     */
    private String generatePlaceholderDigest() {
        // Generate a proper 64-character hex digest
        StringBuilder digest = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            digest.append('0');
        }
        return digest.toString();
    }

    /**
     * Determine operation status based on stage and data
     */
    private String determineOperationStatus(String stage, String operationData) {
        if (stage == null) {
            return "Unknown";
        }
        
        switch (stage.toUpperCase()) {
            case "QUEUED":
                return "Queued";
            case "EXECUTING":
                return "Executing";
            case "COMPLETED":
                return "Completed";
            case "FAILED":
                return "Failed";
            default:
                return stage;
        }
    }
    
    /**
     * Get all operations from all queues with queue information
     */
    public java.util.List<java.util.Map<String, Object>> getAllQueueOperations() {
        java.util.List<java.util.Map<String, Object>> allOperations = new java.util.ArrayList<>();
        
        try {
            logger.info("Getting all operations from buildfarm queue pattern");
            
            // Step 1: Use the new dynamic queue discovery method
            java.util.List<String> discoveredQueueNames = getQueueNames();
            logger.info("Using {} discovered queue names: {}", discoveredQueueNames.size(), discoveredQueueNames);
            
            // Step 2: For each discovered queue name, load all related keys and operations
            for (String queueName : discoveredQueueNames) {
                logger.info("Processing queue: {}", queueName);
                
                // Look for all keys related to this queue
                java.util.Set<String> queueRelatedKeys = getKeys("*" + queueName + "*");
                logger.info("Found {} keys related to queue '{}': {}", queueRelatedKeys.size(), queueName, queueRelatedKeys);
                
                // Process each key related to this queue
                for (String queueKey : queueRelatedKeys) {
                    try {
                        String keyType = getKeyType(queueKey);
                        logger.info("Processing queue key: {} of type: {} for queue: {}", queueKey, keyType, queueName);
                        
                        if ("list".equals(keyType)) {
                            // Handle list-type queue
                            Object listData = getValueByType(queueKey);
                            if (listData instanceof java.util.List) {
                                @SuppressWarnings("unchecked")
                                java.util.List<String> list = (java.util.List<String>) listData;
                                
                                logger.info("Processing {} operations from list key: {}", list.size(), queueKey);
                                for (int i = 0; i < list.size(); i++) {
                                    String operationData = list.get(i);
                                    java.util.Map<String, Object> operation = parseOperationData(operationData, allOperations.size());
                                    if (operation != null) {
                                        operation.put("queueName", queueName);
                                        operation.put("sourceKey", queueKey);
                                        
                                        // Extra safety check for actionDigest
                                        Object digest = operation.get("actionDigest");
                                        if (digest != null && digest instanceof String && ((String) digest).length() < 8) {
                                            logger.warn("Found operation with short actionDigest '{}' (length={}), replacing with placeholder", 
                                                       digest, ((String) digest).length());
                                            operation.put("actionDigest", generatePlaceholderDigest());
                                        }
                                        
                                        allOperations.add(operation);
                                    }
                                }
                            }
                        } else if ("hash".equals(keyType)) {
                            // Handle hash-type queue
                            Object hashData = getValueByType(queueKey);
                            if (hashData instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, String> hash = (java.util.Map<String, String>) hashData;
                                
                                logger.info("Processing {} operations from hash key: {}", hash.size(), queueKey);
                                for (java.util.Map.Entry<String, String> entry : hash.entrySet()) {
                                    String operationId = entry.getKey();
                                    String operationData = entry.getValue();
                                    java.util.Map<String, Object> operation = parseOperationData(operationData, allOperations.size());
                                    if (operation != null) {
                                        operation.put("operationId", operationId);
                                        operation.put("queueName", queueName);
                                        operation.put("sourceKey", queueKey);
                                        
                                        // Extra safety check for actionDigest
                                        Object digest = operation.get("actionDigest");
                                        if (digest != null && digest instanceof String && ((String) digest).length() < 8) {
                                            logger.warn("Found operation with short actionDigest '{}' (length={}), replacing with placeholder", 
                                                       digest, ((String) digest).length());
                                            operation.put("actionDigest", generatePlaceholderDigest());
                                        }
                                        
                                        allOperations.add(operation);
                                    }
                                }
                            }
                        } else if ("set".equals(keyType)) {
                            // Handle set-type data
                            Object setData = getValueByType(queueKey);
                            if (setData instanceof java.util.Set) {
                                @SuppressWarnings("unchecked")
                                java.util.Set<String> set = (java.util.Set<String>) setData;
                                
                                logger.info("Processing {} operations from set key: {}", set.size(), queueKey);
                                int index = 0;
                                for (String operationData : set) {
                                    java.util.Map<String, Object> operation = parseOperationData(operationData, allOperations.size());
                                    if (operation != null) {
                                        operation.put("queueName", queueName);
                                        operation.put("sourceKey", queueKey);
                                        operation.put("setIndex", index++);
                                        
                                        // Extra safety check for actionDigest
                                        Object digest = operation.get("actionDigest");
                                        if (digest != null && digest instanceof String && ((String) digest).length() < 8) {
                                            logger.warn("Found operation with short actionDigest '{}' (length={}), replacing with placeholder", 
                                                       digest, ((String) digest).length());
                                            operation.put("actionDigest", generatePlaceholderDigest());
                                        }
                                        
                                        allOperations.add(operation);
                                    }
                                }
                            }
                        } else if ("zset".equals(keyType)) {
                            // Handle sorted set (zset) - this is what buildfarm uses for queues
                            try {
                                // Use raw string operations to avoid JSON deserialization issues with timestamp prefixes
                                org.springframework.data.redis.core.StringRedisTemplate stringTemplate = 
                                    new org.springframework.data.redis.core.StringRedisTemplate(valkeyTemplate.getConnectionFactory());
                                
                                java.util.Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> zsetsWithScores = 
                                    stringTemplate.opsForZSet().rangeWithScores(queueKey, 0, -1);
                                
                                if (zsetsWithScores != null) {
                                    logger.info("Processing {} operations from zset key: {}", zsetsWithScores.size(), queueKey);
                                    
                                    int itemCount = 0;
                                    for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : zsetsWithScores) {
                                        String operationDataStr = tuple.getValue();
                                        Double score = tuple.getScore();
                                        
                                        // Debug first few items to check for truncation
                                        if (itemCount < 3) {
                                            logger.debug("Zset item {}: length={}, starts={}, ends={}", 
                                                        itemCount, 
                                                        operationDataStr != null ? operationDataStr.length() : 0,
                                                        operationDataStr != null && operationDataStr.length() > 50 ? operationDataStr.substring(0, 50) : operationDataStr,
                                                        operationDataStr != null && operationDataStr.length() > 50 ? operationDataStr.substring(operationDataStr.length() - 50) : "");
                                        }
                                        itemCount++;
                                        
                                        if (operationDataStr != null) {
                                            // Let parseOperationData handle timestamp extraction
                                            // Don't strip it here to avoid double-processing
                                            logger.debug("Processing zset value of length: {}", operationDataStr.length());
                                            
                                            java.util.Map<String, Object> operation = parseOperationData(operationDataStr, allOperations.size());
                                            if (operation != null) {
                                                operation.put("queueName", queueName);
                                                operation.put("sourceKey", queueKey);
                                                operation.put("priority", score); // Store the zset score as priority
                                                
                                                // Extra safety check for actionDigest
                                                Object digest = operation.get("actionDigest");
                                                if (digest != null && digest instanceof String && ((String) digest).length() < 8) {
                                                    logger.warn("Found operation with short actionDigest '{}' (length={}), replacing with placeholder", 
                                                               digest, ((String) digest).length());
                                                    operation.put("actionDigest", generatePlaceholderDigest());
                                                }
                                                
                                                allOperations.add(operation);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Error processing zset key: {} for queue: {}", queueKey, queueName, e);
                            }
                        } else {
                            logger.info("Skipping queue key {} with unsupported type: {}", queueKey, keyType);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing queue key: {} for queue: {}", queueKey, queueName, e);
                    }
                }
            }
            
            logger.info("Returning {} total operations from {} queues", allOperations.size(), discoveredQueueNames.size());
            
        } catch (Exception e) {
            logger.error("Failed to get all queue operations", e);
        }
        
        return allOperations;
    }
    
    /**
     * Extract queue name from Redis key using *_queue_* pattern
     */
    private String extractQueueNameFromRedisKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        logger.debug("Extracting queue name from key: {}", key);
        
        // Look for patterns like "cpu_queue_priority", "gpu_queue_priority", etc.
        if (key.contains("_queue_")) {
            // First try to find the queue name in common Redis key patterns
            // Pattern 1: {namespace}:cpu_queue_priority or {namespace}:QueuedOperations:cpu_queue_priority
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_]+_queue_[a-zA-Z0-9_]+)");
            java.util.regex.Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                String queueName = matcher.group(1);
                logger.debug("Extracted queue name '{}' from key '{}'", queueName, key);
                return queueName;
            }
            
            // Pattern 2: Split by delimiters and find the part with _queue_
            String[] parts = key.split("[:{\\}\\[\\]]");
            for (String part : parts) {
                if (part.contains("_queue_") && !part.isEmpty()) {
                    logger.debug("Extracted queue name '{}' from key part '{}'", part, key);
                    return part;
                }
            }
            
            // Pattern 3: Direct match for known queue patterns
            if (key.contains("cpu_queue_priority")) {
                return "cpu_queue_priority";
            } else if (key.contains("gpu_queue_priority")) {
                return "gpu_queue_priority";
            }
            
            // Pattern 4: Extract any substring that looks like a queue name
            int queueIndex = key.indexOf("_queue_");
            if (queueIndex > 0) {
                // Find the start of the queue name (look backwards for word boundary)
                int start = queueIndex;
                while (start > 0 && Character.isLetterOrDigit(key.charAt(start - 1))) {
                    start--;
                }
                
                // Find the end of the queue name (look forwards for word boundary)
                int end = queueIndex + "_queue_".length();
                while (end < key.length() && (Character.isLetterOrDigit(key.charAt(end)) || key.charAt(end) == '_')) {
                    end++;
                }
                
                if (end > start) {
                    String extractedName = key.substring(start, end);
                    logger.debug("Extracted queue name '{}' using boundary detection from key '{}'", extractedName, key);
                    return extractedName;
                }
            }
        }
        
        logger.debug("No queue name found in key: {}", key);
        return null;
    }
    /**
     * Get all dispatched operations from the DispatchedOperations hash
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> getDispatchedOperations() {
        java.util.List<java.util.Map<String, Object>> dispatchedOperations = new java.util.ArrayList<>();
        
        try {
            logger.info("Getting dispatched operations from DispatchedOperations hash");
            String dispatchedKey = "DispatchedOperations";
            
            if (hasKey(dispatchedKey)) {
                String keyType = getKeyType(dispatchedKey);
                logger.info("Processing dispatched operations key: {} of type: {}", dispatchedKey, keyType);
                
                if ("hash".equals(keyType)) {
                    // Handle hash-type dispatched operations
                    Object hashData = getValueByType(dispatchedKey);
                    if (hashData instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, String> hash = (java.util.Map<String, String>) hashData;
                        
                        logger.info("Found {} dispatched operations in hash", hash.size());
                        
                        for (java.util.Map.Entry<String, String> entry : hash.entrySet()) {
                            String operationId = entry.getKey();
                            String operationData = entry.getValue();
                            java.util.Map<String, Object> operation = parseOperationData(operationData, dispatchedOperations.size());
                            if (operation != null) {
                                operation.put("operationId", operationId);
                                operation.put("status", "dispatched");
                                dispatchedOperations.add(operation);
                            }
                        }
                    }
                } else {
                    logger.warn("DispatchedOperations key is not a hash, found type: {}", keyType);
                }
            } else {
                logger.warn("DispatchedOperations key does not exist");
            }
            
            logger.info("Returning {} dispatched operations", dispatchedOperations.size());
            
        } catch (Exception e) {
            logger.error("Failed to get dispatched operations", e);
        }
        
        return dispatchedOperations;
    }
    
    /**
     * Extract operation name specifically from buildfarm executeEntry structure
     */
    private String extractBuildfarmOperationName(String jsonData) {
        // Look for operationName specifically in executeEntry context
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"executeEntry\"\\s*:\\s*\\{[^}]*\"operationName\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(jsonData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback to simpler pattern
        pattern = java.util.regex.Pattern.compile("\"operationName\"\\s*:\\s*\"([^\"]+)\"");
        matcher = pattern.matcher(jsonData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "Unknown Operation";
    }
    
    /**
     * Extract action digest hash from buildfarm executeEntry structure
     */
    private String extractBuildfarmActionDigest(String jsonData) {
        // Look for hash specifically in executeEntry > actionDigest context
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"executeEntry\"\\s*:\\s*\\{[^}]*\"actionDigest\"\\s*:\\s*\\{[^}]*\"hash\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(jsonData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback to simpler hash pattern
        pattern = java.util.regex.Pattern.compile("\"hash\"\\s*:\\s*\"([^\"]+)\"");
        matcher = pattern.matcher(jsonData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Get all prequeued operations from the {Arrival}:PreQueuedOperations list
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> getPrequeuedOperations() {
        java.util.List<java.util.Map<String, Object>> prequeuedOperations = new java.util.ArrayList<>();
        
        try {
            logger.info("Getting prequeued operations from {Arrival}:PreQueuedOperations list");
            String prequeuedKey = "{Arrival}:PreQueuedOperations";
            
            if (hasKey(prequeuedKey)) {
                String keyType = getKeyType(prequeuedKey);
                logger.info("Processing prequeued operations key: {} of type: {}", prequeuedKey, keyType);
                
                if ("list".equals(keyType)) {
                    // Handle list-type prequeued operations
                    java.util.List<Object> listData = valkeyTemplate.opsForList().range(prequeuedKey, 0, -1);
                    
                    if (listData != null) {
                        logger.info("Found {} prequeued operations in list", listData.size());
                        
                        for (int i = 0; i < listData.size(); i++) {
                            Object item = listData.get(i);
                            String operationData = item.toString();
                            java.util.Map<String, Object> operation = parseOperationData(operationData, i + 1);
                            if (operation != null) {
                                operation.put("status", "prequeued");
                                operation.put("stage", "PREQUEUED");
                                prequeuedOperations.add(operation);
                            }
                        }
                    }
                } else {
                    logger.warn("PreQueuedOperations key is not a list, found type: {}", keyType);
                }
            } else {
                logger.warn("{Arrival}:PreQueuedOperations key does not exist");
            }
            
            logger.info("Returning {} prequeued operations", prequeuedOperations.size());
            
        } catch (Exception e) {
            logger.error("Failed to get prequeued operations", e);
        }
        
        return prequeuedOperations;
    }
    
    @Override
    public Long removeFromList(String key, String value) {
        try {
            logger.debug("Removing value from list key: {}", key);
            return valkeyTemplate.opsForList().remove(key, 0, value);
        } catch (Exception e) {
            logger.error("Error removing from list {}: {}", key, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public Boolean removeFromHash(String key, String field) {
        try {
            logger.debug("Removing field '{}' from hash key: {}", field, key);
            return valkeyTemplate.opsForHash().delete(key, field) > 0;
        } catch (Exception e) {
            logger.error("Error removing from hash {} field {}: {}", key, field, e.getMessage());
            return false;
        }
    }
    
    @Override
    public java.util.List<String> getListAsString(String key) {
        try {
            java.util.List<Object> items = valkeyTemplate.opsForList().range(key, 0, -1);
            if (items == null) {
                return new java.util.ArrayList<>();
            }
            java.util.List<String> result = new java.util.ArrayList<>();
            for (Object item : items) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        } catch (Exception e) {
            logger.error("Error getting list as string for key {}: {}", key, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    @Override
    public Long removeFromZSet(String key, String value) {
        try {
            logger.debug("Removing value from sorted set key: {}", key);
            return valkeyTemplate.opsForZSet().remove(key, value);
        } catch (Exception e) {
            logger.error("Error removing from sorted set {}: {}", key, e.getMessage());
            return 0L;
        }
    }
    
    @Override
    public java.util.List<String> getZSetValues(String key) {
        try {
            java.util.Set<Object> values = valkeyTemplate.opsForZSet().range(key, 0, -1);
            if (values == null) {
                return new java.util.ArrayList<>();
            }
            java.util.List<String> result = new java.util.ArrayList<>();
            for (Object value : values) {
                result.add(value != null ? value.toString() : "");
            }
            return result;
        } catch (Exception e) {
            logger.error("Error getting sorted set values for key {}: {}", key, e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}
