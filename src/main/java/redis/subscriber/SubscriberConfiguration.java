package redis.subscriber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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
}
