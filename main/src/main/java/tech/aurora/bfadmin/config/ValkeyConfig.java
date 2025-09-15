package tech.aurora.bfadmin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ValkeyConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ValkeyConfig.class);
    
    @Value("${valkey.uri:redis://localhost:6379}")
    private String redisUri;
    
    @Value("${valkey.cluster.nodes:localhost:7000}")
    private String clusterNodes;
    
    @Value("${valkey.cluster.enabled:false}")
    private boolean clusterEnabled;
    
    @Value("${valkey.connection.timeout:2000}")
    private long connectionTimeout;
    
    @Value("${valkey.connection.pool.max-active:8}")
    private int maxActive;
    
    @Value("${valkey.connection.pool.max-idle:8}")
    private int maxIdle;
    
    @Value("${valkey.connection.pool.min-idle:0}")
    private int minIdle;
    
    @Value("${valkey.cluster.username:}")
    private String username;
    
    @Value("${valkey.cluster.password:}")
    private String password;
    
    @Value("${valkey.cluster.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${valkey.cluster.ssl.verify-peer:true}")
    private boolean sslVerifyPeer;
    
    @Bean
    public RedisConnectionFactory valkeyConnectionFactory() {
        try {
            // Configure connection pool
            GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(maxActive);
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMinIdle(minIdle);
            
            LettucePoolingClientConfiguration clientConfiguration;
            
            // Configure SSL/TLS if enabled
            if (sslEnabled) {
                logger.info("Configuring Redis/Valkey with SSL/TLS encryption");
                clientConfiguration = LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .commandTimeout(Duration.ofMillis(connectionTimeout))
                        .useSsl()
                        .build();
                logger.info("SSL configuration applied successfully");
            } else {
                clientConfiguration = LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .commandTimeout(Duration.ofMillis(connectionTimeout))
                        .build();
            }
            
            if (clusterEnabled) {
                // Cluster mode configuration
                List<String> nodes = Arrays.asList(clusterNodes.split(","));
                logger.info("Configuring Valkey cluster with nodes: {}", nodes);
                
                RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodes);
                
                // Set authentication if provided
                if (username != null && !username.trim().isEmpty()) {
                    logger.info("Configuring Valkey cluster with username authentication");
                    clusterConfiguration.setUsername(username);
                }
                if (password != null && !password.trim().isEmpty()) {
                    logger.info("Configuring Valkey cluster with password authentication");
                    clusterConfiguration.setPassword(password);
                }
                
                LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
                factory.setValidateConnection(true);
                logger.info("Valkey cluster connection factory configured successfully");
                return factory;
            } else {
                // Standalone mode configuration using URI
                logger.info("Configuring standalone Redis/Valkey with URI: {}", redisUri);
                
                // Parse URI to get host and port
                String host = "localhost";
                int port = 6379;
                if (redisUri.startsWith("redis://")) {
                    String[] parts = redisUri.substring(8).split(":");
                    host = parts[0];
                    if (parts.length > 1) {
                        port = Integer.parseInt(parts[1]);
                    }
                }
                
                RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
                
                // Set authentication if provided
                if (username != null && !username.trim().isEmpty()) {
                    logger.info("Configuring standalone Redis with username authentication");
                    standaloneConfig.setUsername(username);
                }
                if (password != null && !password.trim().isEmpty()) {
                    logger.info("Configuring standalone Redis with password authentication");
                    standaloneConfig.setPassword(password);
                }
                
                LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
                factory.setValidateConnection(true);
                logger.info("Standalone Redis connection factory configured successfully for {}:{}", host, port);
                return factory;
            }
            
        } catch (Exception e) {
            logger.error("Failed to configure Redis/Valkey connection factory", e);
            throw new RuntimeException("Unable to configure Redis/Valkey connection", e);
        }
    }
    
    @Bean
    public RedisTemplate<String, Object> valkeyTemplate(RedisConnectionFactory valkeyConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(valkeyConnectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        
        logger.info("Valkey RedisTemplate configured successfully");
        return template;
    }
    

}
