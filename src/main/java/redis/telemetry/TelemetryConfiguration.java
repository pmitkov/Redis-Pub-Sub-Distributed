package redis.telemetry;

import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:telemetry.properties")
public class TelemetryConfiguration {

    @Bean
    MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }
}
