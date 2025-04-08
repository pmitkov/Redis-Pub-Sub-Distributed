package redis.subscriber.model;

public record ChannelProperties(String messageChannelName, String streamName, String listName) {
}