package redis.subscriber.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;
import redis.subscriber.model.ChannelProperties;
import redis.subscriber.model.CircularDHT;
import redis.subscriber.model.TreeSetCircularDHT;
import redis.telemetry.TelemetryService;

import java.time.Instant;
import java.util.List;

import static redis.ApplicationConfiguration.UNIVERSE_SIZE;

@Service
public class SubscriberGroupService {

    private static final String SUBSCRIBER_KEY_PREFIX = "ABRACADABRA_";
    private static final String MESSAGE_KEY_PREFIX = "LockMessage";

    private final CircularDHT circularDHT = new TreeSetCircularDHT(UNIVERSE_SIZE);
    private final JedisPool jedisPool;
    private final long subscriberId;
    private final long keepAliveTimeout;
    private final int workerReplicas;
    private final ChannelProperties channelProperties;
    private final TelemetryService telemetryService;
    private boolean registered = false;


    public SubscriberGroupService(@Value("${redis.keep-alive-timeout.ms}") long keepAliveTimeout,
                                  @Value("${redis.worker.replicas}") int workerReplicas,
                                  JedisPool jedisPool,
                                  long subscriberId,
                                  ChannelProperties channelProperties,
                                  TelemetryService telemetryService) {
        this.keepAliveTimeout = keepAliveTimeout;
        this.workerReplicas = workerReplicas;
        this.jedisPool = jedisPool;
        this.subscriberId = subscriberId;
        this.channelProperties = channelProperties;
        this.telemetryService = telemetryService;
    }

    @Scheduled(fixedRateString = "${redis.keep-alive.heartbeat.ms}")
    public void sendKeepAliveHeartBeat() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(SUBSCRIBER_KEY_PREFIX + Long.toString(subscriberId),
                    keepAliveTimeout / 1000L,
                    Instant.now().toString());
        }
    }

    @Scheduled(fixedRateString = "${redis.keep-alive-timeout.ms}", initialDelayString = "${redis.keep-alive-timeout.ms}")
    public void updateAliveWorkers() {
        registered = true;
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> registered = jedis.lrange(channelProperties.listName(), 0, -1);
            for (String worker : registered) {
                if (jedis.get(SUBSCRIBER_KEY_PREFIX + worker) != null) {
                    circularDHT.addValue(Long.parseLong(worker));
                } else {
                    circularDHT.removeValue(Long.parseLong(worker));
                }
            }
        }
    }

    public boolean isProcessedBySubscriber(long message) {
        return registered &&
                circularDHT.getKNearest(message % UNIVERSE_SIZE, workerReplicas).contains(subscriberId) &&
                tryToAcquireLock(message);
    }

    private boolean tryToAcquireLock(long message) {
        String messageId = MESSAGE_KEY_PREFIX + Long.toString(message);
        try (Jedis jedis = jedisPool.getResource()) {
            String res = jedis.set(messageId,
                    Long.toString(subscriberId),
                    new SetParams().nx().ex(keepAliveTimeout / 1000));
            if (res == null) {
                telemetryService.messageFailedLock();
                return false;
            } else {
                return true;
            }
        }
    }
}
