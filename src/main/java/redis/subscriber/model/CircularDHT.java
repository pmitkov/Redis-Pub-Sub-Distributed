package redis.subscriber.model;

import java.util.List;
import java.util.Set;

public interface CircularDHT {
    boolean addValue(long value);
    boolean removeValue(long value);
    boolean hasValue(long value);
    long getNearest(long value);
    Set<Long> getKNearest(long value, int k);
    List<Long> getValues();
}
