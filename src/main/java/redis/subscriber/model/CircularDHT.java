package redis.subscriber.model;

import java.util.List;

public interface CircularDHT {
    boolean addValue(long value);
    boolean removeValue(long value);
    boolean hasValue(long value);
    long getNearest(long value);
    List<Long> getValues();
}
