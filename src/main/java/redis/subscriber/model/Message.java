package redis.subscriber.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

    @JsonProperty(value = "message_id", required = true)
    private String messageId;

    @JsonProperty("computed_value")
    private String computedValue;

    @JsonProperty("consumer_id")
    private String consumerId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getComputedValue() {
        return computedValue;
    }

    public void setComputedValue(String computedValue) {
        this.computedValue = computedValue;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}