package subscriber;

import org.junit.jupiter.api.Test;
import redis.subscriber.model.TreeSetCircularDHT;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TreeSetCircularDHTTest {

    @Test
    public void testOperations() {
        TreeSetCircularDHT set = new TreeSetCircularDHT(128);
        assertThat(set.addValue(10L)).isTrue();
        assertThat(set.addValue(5L)).isTrue();
        assertThat(set.addValue(2L)).isTrue();
        assertThat(set.addValue(3L)).isTrue();
        assertThat(set.addValue(10L)).isFalse();
        assertThat(set.getValues()).hasSize(4);
        assertThat(set.getValues()).containsExactly(2L, 3L, 5L, 10L);

        assertThat(set.hasValue(10L)).isTrue();
        assertThat(set.removeValue(10L)).isTrue();
        assertThat(set.removeValue(10L)).isFalse();
        assertThat(set.hasValue(10L)).isFalse();
        assertThat(set.getValues()).containsExactly(2L, 3L, 5L);
    }

    @Test
    public void testGetNearest() {
        TreeSetCircularDHT set = new TreeSetCircularDHT(128, List.of(4L, 10L, 15L, 24L, 25L, 30L));
        assertThat(set.getNearest(4L)).isEqualTo(4L);
        assertThat(set.getNearest(5L)).isEqualTo(4L);
        assertThat(set.getNearest(6L)).isEqualTo(4L);
        assertThat(set.getNearest(7L)).isEqualTo(4L);
        assertThat(set.getNearest(1L)).isEqualTo(4L);
        assertThat(set.getNearest(2L)).isEqualTo(4L);
        assertThat(set.getNearest(3L)).isEqualTo(4L);
        assertThat(set.getNearest(127L)).isEqualTo(4L);
        assertThat(set.getNearest(8L)).isEqualTo(10L);
        assertThat(set.getNearest(12L)).isEqualTo(10L);
        assertThat(set.getNearest(13L)).isEqualTo(15L);

        set = new TreeSetCircularDHT(32, List.of(3L, 29L));
        assertThat(set.getNearest(0L)).isEqualTo(29L);
        assertThat(set.getNearest(1L)).isEqualTo(3L);
        assertThat(set.getNearest(31L)).isEqualTo(29L);
    }

    @Test()
    public void testConstraints() {
        assertThrows(IllegalArgumentException.class, () -> new TreeSetCircularDHT(32, List.of(-1L)));
        assertThrows(IllegalArgumentException.class, () -> new TreeSetCircularDHT(32, List.of(32L)));
        TreeSetCircularDHT set = new TreeSetCircularDHT(128);
        assertThrows(IllegalArgumentException.class, () -> set.addValue(-1L));
        assertThrows(IllegalArgumentException.class, () -> set.addValue(128));
        assertThrows(IllegalArgumentException.class, () -> set.addValue(129));
        assertThrows(IllegalStateException.class, () -> set.getNearest(12L));
    }

    @Test
    public void testKNearest() {
        TreeSetCircularDHT set = new TreeSetCircularDHT(128, List.of(4L, 10L, 15L, 24L, 25L, 30L));
        assertThat(set.getKNearest(11L, 3)).containsOnly(4L, 10L, 15L);
        assertThat(set.getKNearest(15L, 3)).containsOnly(10L, 15L, 24L);
        assertThat(set.getKNearest(14L, 3)).containsOnly(4L, 10L, 15L);
        assertThat(set.getKNearest(127L, 3)).containsOnly(4L, 10L, 15L);
        for (int i = 0; i < 128; i++) {
            assertThat(set.getKNearest(i, 1).stream().findFirst().get()).isEqualTo(set.getNearest(i));
            assertThat(set.getKNearest(11L, 6)).containsOnly(4L, 10L, 15L, 24L, 25L, 30L);
        }
        set = new TreeSetCircularDHT(32, List.of(4L, 10L, 15L, 24L, 25L, 30L));
        assertThat(set.getKNearest(4L, 3)).containsOnly(4L, 10L, 30L);
    }
}
