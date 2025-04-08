package redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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

    @Bean(destroyMethod = "destroy")
    public JedisPool jPool(@Value("${redis.host}") String host,
                           @Value("${redis.port}") int port,
                           @Value("${jedis.pool.max.total}") int maxTotal,
                           @Value("${jedis.pool.max.idle}") int maxIdle,
                           @Value("${jedis.pool.min.idle}") int minIdle) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setJmxEnabled(false);
        return new JedisPool(jedisPoolConfig, host, port);
    }
}