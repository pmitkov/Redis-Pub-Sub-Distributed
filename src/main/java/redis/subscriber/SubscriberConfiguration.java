package redis.subscriber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.subscriber.model.ChannelProperties;

@Configuration
@PropertySource("classpath:redis.properties")
public class SubscriberConfiguration {

    @Bean
    public ChannelProperties channelProperties(@Value("${redis.message.channel}") String channel,
                                               @Value("${redis.message.stream}") String stream,
                                               @Value("${redis.list}") String list) {
        return new ChannelProperties(channel, stream, list);
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(16);
        taskExecutor.setMaxPoolSize(16);
        taskExecutor.setQueueCapacity(0);
        return taskExecutor;
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
        return new JedisPool(jedisPoolConfig, host, port);
    }
}
