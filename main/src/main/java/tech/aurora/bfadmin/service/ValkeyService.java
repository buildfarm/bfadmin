package tech.aurora.bfadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ValkeyService implements ValkeyServiceInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(ValkeyService.class);
    
    @Autowired
    private RedisTemplate<String, Object> valkeyTemplate;
    
    @Value("${valkey.cluster.nodes:localhost:7000}")
    private String clusterNodes;
    
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
            Set<String> serverKeys = getKeys(pattern);
            logger.info("🔍 Fetching Servers keys from Redis with pattern: {}", pattern);
            logger.info("🔑 Found {} server keys matching pattern '{}': {}", serverKeys.size(), pattern, serverKeys);
            
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
                                String serverKey = entry.getKey();
                                String serverJson = entry.getValue();
                                
                                try {
                                    logger.info("📄 Server JSON for key '{}': {}", serverKey, serverJson);
                                    // Parse the JSON for each individual server
                                    java.util.Map<String, Object> serverData = parseServerJson(serverKey, serverJson);
                                    if (serverData != null) {
                                        servers.add(serverData);
                                        logger.info("✅ Successfully parsed server: {}", serverData);
                                    }
                                } catch (Exception e) {
                                    logger.error("Error parsing server JSON for key {}: {}", serverKey, serverJson, e);
                                    // Add error entry for this specific server
                                    java.util.Map<String, Object> errorData = new java.util.HashMap<>();
                                    errorData.put("serverId", serverKey);
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
     * Get all available queue names including prequeue using backplane configuration
     */
    public java.util.List<String> getQueueNames() {
        java.util.List<String> queueNames = new java.util.ArrayList<>();
        
        try {
            // Add prequeue first (from backplane config: {Arrival}:PreQueuedOperations)
            queueNames.add("prequeue");
            
            // Look for queued operations using backplane naming: {Execution}:QueuedOperations
            Set<String> queueKeys = getKeys("{Execution}:QueuedOperations*");
            logger.info("Found {} execution queue keys", queueKeys.size());
            
            for (String key : queueKeys) {
                logger.info("Processing queue key: {}", key);
                // Extract queue name from backplane format
                if (key.equals("{Execution}:QueuedOperations")) {
                    // Default queue
                    queueNames.add("cpu");
                } else if (key.startsWith("{Execution}:QueuedOperations:")) {
                    // Named queue - extract name after the colon
                    String queueName = key.substring("{Execution}:QueuedOperations:".length());
                    if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                        queueNames.add(queueName);
                    }
                }
            }
            
            // Look for more backplane queue patterns beyond the basic ones
            // Search for additional execution queue patterns that might exist
            Set<String> allExecutionKeys = getKeys("{Execution}*");
            logger.info("Found {} execution-related keys", allExecutionKeys.size());
            
            for (String key : allExecutionKeys) {
                logger.debug("Examining execution key: {}", key);
                if (key.startsWith("{Execution}:QueuedOperations:") && !key.equals("{Execution}:QueuedOperations")) {
                    // Extract queue name from pattern like {Execution}:QueuedOperations:queuename
                    String queueName = key.substring("{Execution}:QueuedOperations:".length());
                    if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                        queueNames.add(queueName);
                        logger.info("Found execution queue: {}", queueName);
                    }
                }
            }
            
            // Look for arrival/dispatch patterns that might indicate other queues
            Set<String> arrivalKeys = getKeys("{Arrival}*");
            logger.info("Found {} arrival-related keys", arrivalKeys.size());
            
            for (String key : arrivalKeys) {
                logger.debug("Examining arrival key: {}", key);
                // Look for patterns like {Arrival}:QueuedOperations:queuename
                if (key.startsWith("{Arrival}:QueuedOperations:") && !key.equals("{Arrival}:QueuedOperations")) {
                    String queueName = key.substring("{Arrival}:QueuedOperations:".length());
                    if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                        queueNames.add(queueName);
                        logger.info("Found arrival queue: {}", queueName);
                    }
                }
            }
            
            // Look for dispatch patterns
            Set<String> dispatchKeys = getKeys("{Dispatch}*");
            logger.info("Found {} dispatch-related keys", dispatchKeys.size());
            
            for (String key : dispatchKeys) {
                logger.debug("Examining dispatch key: {}", key);
                if (key.contains("QueuedOperations:") || key.contains("Queue:")) {
                    // Extract potential queue names from dispatch keys
                    String[] parts = key.split(":");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("QueuedOperations".equals(parts[i]) || "Queue".equals(parts[i])) {
                            String potentialQueue = parts[i + 1];
                            if (!potentialQueue.isEmpty() && !queueNames.contains(potentialQueue)) {
                                queueNames.add(potentialQueue);
                                logger.info("Found dispatch queue: {}", potentialQueue);
                            }
                        }
                    }
                }
            }
            
            // Also look for other queue patterns that might exist in the backplane
            Set<String> allQueueKeys = getKeys("*Queue*");
            logger.info("Found {} keys containing 'Queue'", allQueueKeys.size());
            
            for (String key : allQueueKeys) {
                logger.debug("Examining queue-related key: {}", key);
                
                // Look for backplane queue patterns
                if (key.contains("QueuedOperations:")) {
                    String[] parts = key.split(":");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("QueuedOperations".equals(parts[i]) && i + 1 < parts.length) {
                            String queueName = parts[i + 1];
                            if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                                queueNames.add(queueName);
                                logger.info("Found backplane queue: {}", queueName);
                            }
                        }
                    }
                }
                
                // Look for traditional queue patterns like cpu_queue, gpu_queue etc.
                if (key.contains("_queue") || key.endsWith("Queue")) {
                    String[] parts = key.split(":");
                    String lastPart = parts[parts.length - 1];
                    if (lastPart.endsWith("_queue")) {
                        String queueName = lastPart.replace("_queue", "");
                        if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                            queueNames.add(queueName);
                            logger.info("Found traditional queue: {}", queueName);
                        }
                    } else if (lastPart.endsWith("Queue") && !lastPart.equals("Queue")) {
                        String queueName = lastPart.replace("Queue", "").toLowerCase();
                        if (!queueName.isEmpty() && !queueNames.contains(queueName)) {
                            queueNames.add(queueName);
                            logger.info("Found named queue: {}", queueName);
                        }
                    }
                }
            }
            
            // Add known queues from backplane configuration if not found dynamically
            String[] knownQueues = {"cpu", "default"};
            for (String knownQueue : knownQueues) {
                if (!queueNames.contains(knownQueue)) {
                    // Check if this queue actually exists in Redis
                    String queueKey = "{Execution}:QueuedOperations:" + knownQueue;
                    if (hasKey(queueKey)) {
                        queueNames.add(knownQueue);
                    }
                }
            }
            
            logger.info("Returning {} queue names: {}", queueNames.size(), queueNames);
            
        } catch (Exception e) {
            logger.error("Failed to get queue names", e);
        }
        
        return queueNames;
    }

    /**
     * Get operations in a specific queue using backplane configuration
     */
    public java.util.List<java.util.Map<String, Object>> getQueueOperations(String queueName) {
        java.util.List<java.util.Map<String, Object>> operations = new java.util.ArrayList<>();
        
        try {
            logger.info("Getting operations for queue: {} using backplane config", queueName);
            
            String queueKey;
            if ("prequeue".equals(queueName)) {
                // Use backplane prequeue key: {Arrival}:PreQueuedOperations
                queueKey = "{Arrival}:PreQueuedOperations";
            } else {
                // Use backplane queued operations pattern: {Execution}:QueuedOperations[:queuename]
                if ("cpu".equals(queueName) || "default".equals(queueName)) {
                    // Default queue
                    queueKey = "{Execution}:QueuedOperations";
                } else {
                    // Named queue
                    queueKey = "{Execution}:QueuedOperations:" + queueName;
                }
            }
            
            // Check if the specific key exists, otherwise look for related keys
            Set<String> queueKeys = new java.util.HashSet<>();
            if (hasKey(queueKey)) {
                queueKeys.add(queueKey);
            } else {
                // Fallback: look for keys with similar patterns
                String pattern = queueKey + "*";
                queueKeys = getKeys(pattern);
            }
            
            logger.info("Found {} keys for queue '{}' with key: {}", queueKeys.size(), queueName, queueKey);
            
            for (String key : queueKeys) {
                try {
                    String keyType = getKeyType(key);
                    logger.info("Processing queue key: {} of type: {}", key, keyType);
                    
                    if ("list".equals(keyType)) {
                        // Handle list-type queue
                        Object listData = getValueByType(key);
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
                        Object hashData = getValueByType(key);
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
                        Object stringData = getValueByType(key);
                        if (stringData != null) {
                            java.util.Map<String, Object> operation = parseOperationData(stringData.toString(), 0);
                            if (operation != null) {
                                operations.add(operation);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing queue key: {}", key, e);
                }
            }
            
            logger.info("Returning {} operations for queue: {}", operations.size(), queueName);
            
        } catch (Exception e) {
            logger.error("Failed to get operations for queue: {}", queueName, e);
        }
        
        return operations;
    }

    /**
     * Parse operation data from JSON or string format
     */
    private java.util.Map<String, Object> parseOperationData(String operationData, int index) {
        try {
            java.util.Map<String, Object> operation = new java.util.HashMap<>();
            
            if (operationData != null && !operationData.trim().isEmpty()) {
                // Try to extract common fields from JSON-like data
                String operationName = extractJsonField(operationData, "name");
                String stage = extractJsonField(operationData, "stage");
                String requestMetadata = extractJsonField(operationData, "requestMetadata");
                String executeResponse = extractJsonField(operationData, "executeResponse");
                String queuedTimestamp = extractJsonField(operationData, "queuedTimestamp");
                
                operation.put("index", index + 1);
                operation.put("operationName", operationName != null ? operationName : "operation-" + (index + 1));
                operation.put("stage", stage != null ? stage : "UNKNOWN");
                operation.put("status", determineOperationStatus(stage, operationData));
                operation.put("queuedAt", queuedTimestamp != null ? formatTimestamp(queuedTimestamp) : "N/A");
                operation.put("rawData", operationData.length() > 200 ? operationData.substring(0, 200) + "..." : operationData);
                
                // Extract additional metadata if available
                if (requestMetadata != null) {
                    operation.put("hasMetadata", true);
                } else {
                    operation.put("hasMetadata", false);
                }
                
                if (executeResponse != null) {
                    operation.put("hasExecuteResponse", true);
                } else {
                    operation.put("hasExecuteResponse", false);
                }
                
            } else {
                // Empty or null data
                operation.put("index", index + 1);
                operation.put("operationName", "empty-operation-" + (index + 1));
                operation.put("stage", "EMPTY");
                operation.put("status", "Empty");
                operation.put("queuedAt", "N/A");
                operation.put("rawData", "");
                operation.put("hasMetadata", false);
                operation.put("hasExecuteResponse", false);
            }
            
            return operation;
            
        } catch (Exception e) {
            logger.error("Error parsing operation data: {}", operationData, e);
            return null;
        }
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
}
