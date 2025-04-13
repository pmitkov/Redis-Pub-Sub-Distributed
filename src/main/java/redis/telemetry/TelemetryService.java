package redis.telemetry;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.timeseries.TSCreateParams;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TelemetryService {

    public static final String PROCESSED_METER = "PROCESSED_MESSAGE_METER";
    public static final String FAILED_METER = "FAILED_MESSAGE_METER";
    public static final String NOT_PROCESSED_METER = "NOT_PROCESSED_MESSAGE_METER";

    private final MetricRegistry metricRegistry;
    private final ConnectionPoolConfig poolConfig;
    private final HostAndPort hostAndPort;
    private final ConsoleReporter reporter;
    private final String subscriberId;

    public TelemetryService(@Value("${telemetry.poll.interval.ms}") int pollInterval,
                            MetricRegistry metricRegistry,
                            ConnectionPoolConfig poolConfig,
                            HostAndPort hostAndPort,
                            long subscriberId) {
        this.metricRegistry = metricRegistry;
        this.poolConfig = poolConfig;
        this.hostAndPort = hostAndPort;
        this.subscriberId = Long.toString(subscriberId);

        reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(pollInterval, TimeUnit.MILLISECONDS);
    }

    public void messageProcessed() {
        metricRegistry.meter(PROCESSED_METER).mark();
    }

    public void messageFailedLock() {
        metricRegistry.meter(FAILED_METER).mark();
    }

    public void messageNotProcessed() {
        metricRegistry.meter(NOT_PROCESSED_METER).mark();
    }

    @Scheduled(fixedRateString = "${telemetry.poll.interval.ms}")
    public void reportTelemetry() {
        addTS(getProcessedTSName(), System.currentTimeMillis(), metricRegistry.meter(PROCESSED_METER).getCount());
        addTS(getFailedLockTSName(), System.currentTimeMillis(), metricRegistry.meter(FAILED_METER).getCount());
        addTS(getNotProcessedTSName(), System.currentTimeMillis(), metricRegistry.meter(NOT_PROCESSED_METER).getCount());
        metricRegistry.remove(PROCESSED_METER);
        metricRegistry.remove(FAILED_METER);
        metricRegistry.remove(NOT_PROCESSED_METER);
    }

    public String getProcessedTSName() {
        return PROCESSED_METER + ":" + subscriberId;
    }

    public String getFailedLockTSName() {
        return FAILED_METER + ":" + subscriberId;
    }

    public String getNotProcessedTSName() {
        return NOT_PROCESSED_METER + ":" + subscriberId;
    }

    public void createTS(String key, long retentionMS, Map<String, String> labels) {
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
        jedis.tsCreate(key, TSCreateParams
                .createParams()
                .retention(retentionMS)
                .labels(labels));
    }

    public void addTS(String key, long timestamp, double value) {
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort.getHost(), hostAndPort.getPort());
        jedis.tsAdd(key, timestamp, value);
    }
}
