package log.aggregation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author doreen.vanunu
 *
 *         A Log Aggregation Application hosting a RESTful web service to
 *         receive and aggregate log files from multiple agents
 */
@SpringBootApplication
public class LogAggregationApplication {
	public static void main(String[] args) {
		SpringApplication.run(LogAggregationApplication.class, args);
	}
}
