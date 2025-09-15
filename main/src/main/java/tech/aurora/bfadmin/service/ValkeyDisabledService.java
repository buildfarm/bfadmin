package tech.aurora.bfadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "admin.service.mock", havingValue = "true", matchIfMissing = true)
public class ValkeyDisabledService implements ValkeyServiceInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(ValkeyDisabledService.class);
    
    public ValkeyDisabledService() {
        logger.info("Valkey service is disabled. All operations will return default values.");
    }
    
    @Override
    public boolean testConnection() {
        logger.warn("Valkey is disabled - connection test skipped");
        return false;
    }
    
    @Override
    public String getClusterInfo() {
        return "Valkey cluster is disabled in configuration";
    }
    
    @Override
    public void setValue(String key, Object value) {
        logger.warn("Valkey is disabled - setValue operation skipped for key: {}", key);
    }
    
    @Override
    public void setValue(String key, Object value, long timeout, TimeUnit unit) {
        logger.warn("Valkey is disabled - setValue with expiration operation skipped for key: {}", key);
    }
    
    @Override
    public Object getValue(String key) {
        logger.warn("Valkey is disabled - getValue operation skipped for key: {}", key);
        return null;
    }
    
    @Override
    public boolean deleteKey(String key) {
        logger.warn("Valkey is disabled - deleteKey operation skipped for key: {}", key);
        return false;
    }
    
    @Override
    public Set<String> getKeys(String pattern) {
        logger.warn("Valkey is disabled - getKeys operation skipped for pattern: {}", pattern);
        return Set.of();
    }
    
    @Override
    public boolean hasKey(String key) {
        logger.warn("Valkey is disabled - hasKey operation skipped for key: {}", key);
        return false;
    }
}
