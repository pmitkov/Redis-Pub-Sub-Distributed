package redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.subscriber.model.ChannelProperties;
import redis.subscriber.service.SubscriberGroupService;
import redis.subscriber.service.SubscriberWorker;
import redis.telemetry.TelemetryService;

import java.util.Map;

import static java.util.Map.entry;

@Service
public class BootstrapApplication {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapApplication.class);

    private final SubscriberGroupService subscriberGroupService;
    private final ConnectionPoolConfig poolConfig;
    private final HostAndPort hostAndPort;
    private final ChannelProperties channelProperties;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final long subscriberId;

    private final long retentionTime;
    private final String subscriberLabel;
    private final String metricLabel;

    public BootstrapApplication(SubscriberGroupService subscriberGroupService,
                                ConnectionPoolConfig poolConfig,
                                HostAndPort hostAndPort,
                                ChannelProperties channelProperties,
                                TelemetryService telemetryService,
                                ObjectMapper objectMapper,
                                ThreadPoolTaskExecutor taskExecutor,
                                long subscriberId,
                                @Value("${telemetry.retention.time.ms}") long retentionTime,
                                @Value("${telemetry.subscriber.label}") String subscriberLabel,
                                @Value("${telemetry.metric.label}") String metricLabel) {
        this.subscriberGroupService = subscriberGroupService;
        this.poolConfig = poolConfig;
        this.hostAndPort = hostAndPort;
        this.channelProperties = channelProperties;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.subscriberId = subscriberId;
        this.retentionTime = retentionTime;
        this.subscriberLabel = subscriberLabel;
        this.metricLabel = metricLabel;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
        jedis.lpush(channelProperties.listName(), Long.toString(subscriberId));

        subscriberGroupService.sendKeepAliveHeartBeat();
        subscriberGroupService.updateAliveWorkers();

        try {
            telemetryService.createTS(telemetryService.getProcessedTSName(), retentionTime,
                    Map.ofEntries(entry(subscriberLabel, Long.toString(subscriberId)),
                            entry(metricLabel, TelemetryService.PROCESSED_METER)));
            telemetryService.createTS(telemetryService.getFailedLockTSName(), retentionTime,
                    Map.ofEntries(entry(subscriberLabel, Long.toString(subscriberId)),
                            entry(metricLabel, TelemetryService.FAILED_METER)));
            telemetryService.createTS(telemetryService.getNotProcessedTSName(), retentionTime,
                    Map.ofEntries(entry(subscriberLabel, Long.toString(subscriberId)),
                            entry(metricLabel, TelemetryService.NOT_PROCESSED_METER)));
        } catch (JedisDataException e) {
            LOG.info("RTS creation failed. Reason: {}", e.getMessage());
        }


        taskExecutor.execute(new SubscriberWorker(Long.toString(subscriberId),
                poolConfig, hostAndPort, channelProperties, telemetryService, objectMapper, subscriberGroupService));
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
        jedis.lrem(channelProperties.listName(), 1, Long.toString(subscriberId));
    }
}
