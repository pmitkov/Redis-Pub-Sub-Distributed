package redis.subscriber.model;

import java.util.*;

/**
 * A simple circular DHT data structure with similar structure as in
 * 'Computer Networking A Top-Down Approach 6th Edition p.177'
 * Both adding value and getting the nearest value(left nearest value in case of tiebreaker)
 * are performed in O(lgn) time, where n is the size of the set.
 */
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
    public List<Long> getValues() {
        return new ArrayList<>(values);
    }
}
