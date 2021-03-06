package log.aggregation.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import log.aggregation.exception.DuplicateLineReceivedException;
import log.aggregation.exception.TooManyPendingChunksException;
import log.aggregation.service.LogAggregationService;
import log.dto.LogChunk;

@RestController
@RequestMapping("/logAggregation")
@Configuration
@PropertySource("classpath:service.properties")
public class LogAggregationController {
	
	private final static Logger LOGGER = Logger.getLogger(LogAggregationController.class.getName());

	private Map<String, LogAggregationService> services;
	@Value("${elapsedTimeLimit}")
	int elapsedTimeLimit;
	
	@Autowired
	private ApplicationContext appContext;
	
	public LogAggregationController() {
		services = new HashMap<String, LogAggregationService>();
	}
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String sayHello() {
		return "Log Aggregation Service is up and running!";
	}
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<?> receiveLogChunk(@RequestBody LogChunk chunk, UriComponentsBuilder ucBuilder) {
		LOGGER.info("Received a chunk! " + chunk.toString());
		
        long startTime = System.nanoTime();
		LogAggregationService service = services.get(chunk.getAgentId());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/logAggregation/").build().toUri());
		ResponseEntity<String> response = null;
		
		try {
			if (service == null) {
				service = appContext.getAutowireCapableBeanFactory().createBean(LogAggregationService.class);
				service.setAgentId(chunk.getAgentId());
				services.put(chunk.getAgentId(), service);
			}
			service.addLogChunk(chunk);
	        long elapsedTime = System.nanoTime() - startTime;
	        if (elapsedTime > elapsedTimeLimit) {
        			response = new ResponseEntity<String>(headers, HttpStatus.TOO_MANY_REQUESTS);    			
	        } else {
	        		response = new ResponseEntity<String>(headers, HttpStatus.CREATED);
	        }

		} catch (TooManyPendingChunksException e) {
			response = new ResponseEntity<String>(headers, HttpStatus.PRECONDITION_FAILED);
		} catch (IOException e) {
			response = new ResponseEntity<String>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (DuplicateLineReceivedException e) {
			response = new ResponseEntity<String>(String.valueOf(e.getLatestSequenceReceived()), headers, HttpStatus.CONFLICT);
		}

        return response;
	}
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
}
