package redis.subscriber.model;

import java.util.*;

public class TreeSetCircularDHT implements CircularDHT {

    private final TreeSet<Long> values;
    private final long universeSize;

    public TreeSetCircularDHT(long universeSize) {
        values = new TreeSet<>();
        this.universeSize = universeSize;
    }

    public TreeSetCircularDHT(long universeSize, List<Long> values) {
        for (Long value : values) {
            if (value < 0 || value >= universeSize) {
                throw new IllegalArgumentException(String.format("Value %d out of bounds of [%d %d]", value, 0, universeSize - 1));
            }
        }
        this.values = new TreeSet<>(values);
        this.universeSize = universeSize;
    }

    @Override
    public boolean addValue(long value) {
        if (value < 0 || value >= universeSize) {
            throw new IllegalArgumentException(String.format("Value %d out of bounds of [%d %d]", value, 0, universeSize - 1));
        }
        if (values.contains(value)) {
            return false;
        }
        values.add(value);
        return true;
    }

    @Override
    public boolean removeValue(long value) {
        return values.remove(value);
    }

    @Override
    public boolean hasValue(long value) {
        return values.contains(value);
    }

    /**
     *
     * @param value
     * @return Returns the nearest value(the left-most in case of ties) in the cyclic monoid of size universeSize;
     */
    @Override
    public long getNearest(long value) {
        if (values.isEmpty()) {
            throw new IllegalStateException("Circular DHT is empty");
        }
        if (value < 0 || value >= universeSize) {
            throw new IllegalArgumentException(String.format("Value %d out of bounds of [%d %d]", value, 0, universeSize - 1));
        }
        if (value <= values.first()) {
            if (universeSize - values.last() + value <= values.first() - value) {
                return values.last();
            } else {
                return values.first();
            }
        } else if (value >= values.last()) {
            if (value - values.last() <= universeSize - value + values.first()) {
                return values.last();
            } else {
                return values.first();
            }
        }
        Long prev = values.floor(value);
        assert(prev != null);
        Long next = values.higher(prev);
        assert(next != null);
        assert(prev <= value && value < next);
        if (value - prev <= next - value) {
            return prev;
        } else {
            return next;
        }
    }

    @Override
    public Set<Long> getKNearest(long value, int k) {
        k = Math.min(k, values.size());
        long prev = values.contains(value) ? value : getPrev(value);
        long next = getNext(value);
        Set<Long> result = new HashSet<>();
        while (k > 0) {
            if (getDistance(prev, value) <= getDistance(next, value)) {
                result.add(prev);
                prev = getPrev(prev);
            } else {
                result.add(next);
                next = getNext(next);
            }
            k--;
        }
        return result;
    }

    @Override
    public List<Long> getValues() {
        return new ArrayList<>(values);
    }

    private long getPrev(long value) {
        Long prev = values.lower(value);
        if (prev != null) {
            return prev;
        }
        return values.last();
    }

    private long getNext(long value) {
        Long next = values.higher(value);
        if (next != null) {
            return next;
        }
        return values.first();
    }

    private long getDistance(long value1, long value2) {
        if (value1 > value2) {
            long tmp = value1;
            value1 = value2;
            value2 = tmp;
        }
        return Math.min(value2 - value1, universeSize - value2 + value1);
    }
}
