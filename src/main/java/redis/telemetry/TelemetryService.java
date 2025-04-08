package redis.telemetry;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TelemetryService {

    private static final String METER = "MESSAGE_METER";

    private final MetricRegistry metricRegistry;
    private final ConsoleReporter reporter;

    public TelemetryService(@Value("${telemetry.poll.interval.ms}") int pollInterval,
            MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(pollInterval, TimeUnit.MILLISECONDS);
    }

    public void messageProcessed() {
        metricRegistry.meter(METER).mark();
    }
}
