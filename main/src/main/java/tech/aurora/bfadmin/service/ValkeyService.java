package tech.aurora.bfadmin.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Interface for Valkey cluster operations
 */
public interface ValkeyService {
    
    /**
     * Test the connection to Valkey cluster
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection();
    
    /**
     * Get cluster information
     * @return cluster information as string
     */
    String getClusterInfo();
    
    /**
     * Store a key-value pair
     * @param key the key
     * @param value the value to store
     */
    void setValue(String key, Object value);
    
    /**
     * Store a key-value pair with expiration
     * @param key the key
     * @param value the value to store
     * @param timeout the timeout value
     * @param unit the time unit for timeout
     */
    void setValue(String key, Object value, long timeout, TimeUnit unit);
    
    /**
     * Get a value by key
     * @param key the key
     * @return the value or null if not found
     */
    Object getValue(String key);
    
    /**
     * Delete a key
     * @param key the key to delete
     * @return true if key was deleted, false otherwise
     */
    boolean deleteKey(String key);
    
    /**
     * Get all keys matching a pattern
     * @param pattern the pattern to match
     * @return set of matching keys
     */
    Set<String> getKeys(String pattern);
    
    /**
     * Check if key exists
     * @param key the key to check
     * @return true if key exists, false otherwise
     */
    boolean hasKey(String key);
    
    /**
     * Get all configured queue names
     * @return list of queue names
     */
    java.util.List<String> getQueueNames();
    
    /**
     * Get operations for a specific queue
     * @param queueName the name of the queue
     * @return list of operations
     */
    java.util.List<java.util.Map<String, Object>> getQueueOperations(String queueName);
    
    /**
     * Get operations from all configured queues
     * @return list of all operations
     */
    java.util.List<java.util.Map<String, Object>> getAllQueueOperations();
    
    /**
     * Get all dispatched operations from the DispatchedOperations hash
     * @return list of dispatched operations
     */
    java.util.List<java.util.Map<String, Object>> getDispatchedOperations();
    
    /**
     * Get all prequeued operations from the {Arrival}:PreQueuedOperations list
     * @return list of prequeued operations
     */
    java.util.List<java.util.Map<String, Object>> getPrequeuedOperations();
}
