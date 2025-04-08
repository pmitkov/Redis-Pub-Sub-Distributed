package redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.subscriber.model.ChannelProperties;
import redis.subscriber.service.SubscriberGroupService;
import redis.subscriber.service.SubscriberWorker;
import redis.telemetry.TelemetryService;

@Service
public class BootstrapApplication {

    private final SubscriberGroupService subscriberGroupService;
    private final JedisPool jedisPool;
    private final ChannelProperties channelProperties;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final long subscriberId;

    public BootstrapApplication(SubscriberGroupService subscriberGroupService,
                                JedisPool jedisPool,
                                ChannelProperties channelProperties,
                                TelemetryService telemetryService,
                                ObjectMapper objectMapper,
                                ThreadPoolTaskExecutor taskExecutor,
                                long subscriberId) {
        this.subscriberGroupService = subscriberGroupService;
        this.jedisPool = jedisPool;
        this.channelProperties = channelProperties;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.subscriberId = subscriberId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(channelProperties.listName(), Long.toString(subscriberId));
        }
        subscriberGroupService.sendKeepAliveHeartBeat();
        subscriberGroupService.updateAliveWorkers();
        taskExecutor.execute(new SubscriberWorker(Long.toString(subscriberId),
                jedisPool, channelProperties, telemetryService, objectMapper, subscriberGroupService));
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lrem(channelProperties.listName(), 1, Long.toString(subscriberId));
        }
    }
}
