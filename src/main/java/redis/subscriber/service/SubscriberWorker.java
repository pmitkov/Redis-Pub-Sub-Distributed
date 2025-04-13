package redis.subscriber.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.XAddParams;
import redis.subscriber.CPUTaskStub;
import redis.subscriber.HashUtils;
import redis.subscriber.model.ChannelProperties;
import redis.subscriber.model.Message;
import redis.telemetry.TelemetryService;

import java.util.Map;

public class SubscriberWorker extends JedisPubSub implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberWorker.class);

    private final String id;
    private final ConnectionPoolConfig poolConfig;
    private final HostAndPort hostAndPort;
    private final ChannelProperties channelProperties;
    private final TelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final SubscriberGroupService subscriberGroupService;

    public SubscriberWorker(String id,
                            ConnectionPoolConfig poolConfig,
                            HostAndPort hostAndPort,
                            ChannelProperties channelProperties,
                            TelemetryService telemetryService,
                            ObjectMapper objectMapper,
                            SubscriberGroupService subscriberGroupService) {
        this.id = id;
        this.poolConfig = poolConfig;
        this.hostAndPort = hostAndPort;
        this.channelProperties = channelProperties;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.subscriberGroupService = subscriberGroupService;
    }

    @Override
    public void run() {
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
        jedis.subscribe(this, channelProperties.messageChannelName());
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        LOG.info("Subscribed to channel {}: {}", channel, id);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        LOG.info("Unsubscribed to channel {}: {}", channel, id);
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            Message msg = objectMapper.readValue(message, Message.class);
            if (!subscriberGroupService.isProcessedBySubscriber(Math.abs(HashUtils.getHash(msg.getMessageId())))) {
                telemetryService.messageNotProcessed();
                return;
            }
            msg.setComputedValue(CPUTaskStub.calculatePi((int)(Math.random() * 11) + 39));
            msg.setConsumerId(id);
            JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
            Map<String,String> msgToWrite = objectMapper.convertValue(msg, new TypeReference<>() {});
            jedis.xadd(channelProperties.streamName(), msgToWrite, new XAddParams());
            telemetryService.messageProcessed();
        } catch (JsonProcessingException e) {
            LOG.error("Unable to parse message: {}", e.getMessage());
        }
    }
}
