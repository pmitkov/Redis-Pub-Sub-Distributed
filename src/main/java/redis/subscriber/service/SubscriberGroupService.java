package redis.subscriber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.subscriber.model.ChannelProperties;
import redis.subscriber.model.CircularDHT;
import redis.subscriber.model.TreeSetCircularDHT;
import redis.telemetry.TelemetryService;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Service
public class SubscriberGroupService {
    private static final long UNIVERSE_SIZE = Integer.MAX_VALUE;
    private static final String KEY_PREFIX = "ABRACADABRA_";

    private final CircularDHT circularDHT = new TreeSetCircularDHT(UNIVERSE_SIZE);
    private final JedisPool jedisPool;
    private final long subscriberId;
    private final long keepAliveTimeout;
    private final ChannelProperties channelProperties;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;


    public SubscriberGroupService(@Value("${redis.keep-alive-timeout.ms}") long keepAliveTimeout,
                                  JedisPool jedisPool,
                                  ChannelProperties channelProperties,
                                  ThreadPoolTaskExecutor taskExecutor,
                                  TelemetryService telemetryService,
                                  ObjectMapper objectMapper) {
        this.keepAliveTimeout = keepAliveTimeout;
        this.jedisPool = jedisPool;
        this.subscriberId = Math.abs((new Random()).nextLong()) % UNIVERSE_SIZE;
        this.channelProperties = channelProperties;
        this.taskExecutor = taskExecutor;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRateString = "${redis.keep-alive.heartbeat.ms}")
    public void sendKeepAliveHeartBeat() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(KEY_PREFIX + Long.toString(subscriberId),
                    keepAliveTimeout / 1000L,
                    Instant.now().toString());
        }
    }

    @Scheduled(fixedRateString = "${redis.keep-alive-timeout.ms}")
    public void updateAliveWorkers() {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> registered = jedis.lrange(channelProperties.listName(), 0, -1);
            for (String worker : registered) {
                if (jedis.get(KEY_PREFIX + worker) != null) {
                    circularDHT.addValue(Long.parseLong(worker));
                } else {
                    circularDHT.removeValue(Long.parseLong(worker));
                }
            }
        }
    }

    public boolean isProcessedBySubscriber(long message) {
        return circularDHT.getNearest(message % UNIVERSE_SIZE) == subscriberId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(channelProperties.listName(), Long.toString(subscriberId));
        }
        sendKeepAliveHeartBeat();
        updateAliveWorkers();
        taskExecutor.execute(new SubscriberWorker(Long.toString(subscriberId),
                jedisPool, channelProperties, telemetryService, objectMapper, this));
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lrem(channelProperties.listName(), 1, Long.toString(subscriberId));
        }
    }
}
