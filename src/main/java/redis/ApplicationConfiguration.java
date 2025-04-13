package redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;

import java.util.Random;

@Configuration
public class ApplicationConfiguration {

    public static final long UNIVERSE_SIZE = Integer.MAX_VALUE;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }

    @Bean
    public long subscriberId() {
        return Math.abs((new Random()).nextLong()) % UNIVERSE_SIZE;
    }

    @Bean
    public ConnectionPoolConfig poolConfig(@Value("${jedis.pool.max.total}") int maxTotal,
                                           @Value("${jedis.pool.max.idle}") int maxIdle,
                                           @Value("${jedis.pool.min.idle}") int minIdle) {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setJmxEnabled(false);
        return poolConfig;
    }

    @Bean
    public HostAndPort hostAndPort(@Value("${redis.host}") String host,
                                   @Value("${redis.port}") int port) {
        return new HostAndPort(host, port);
    }
}