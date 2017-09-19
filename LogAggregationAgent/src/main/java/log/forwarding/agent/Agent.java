package log.forwarding.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import log.dto.LogChunk;

/**
 * @author doreenvanunu
 *
 *         Log forwarding agent - tails a log file and sends the content in
 *         chunks to a log aggregation service
 */
@Configuration
@PropertySource("classpath:agent.properties")
public class Agent {

	private final static Logger LOGGER = Logger.getLogger(Agent.class.getName());

	@Value("${logFilePath}")
	private String logFilePath;
	@Value("${defaultWaitInterval}")
	private Long defaultWaitInterval;
	@Value("${defaultChunkSize}")
	private Integer defaultChunkSize;
	@Value("${aggregationServiceUri}")
	private String aggregationServiceUri;
	@Value("${maxSleepingTimes}")
	private int maxSleepingTimes;
	@Value("${defaultAgentId}")
	private String agentId;
	@Value("${slowDownDueToOverloadInterval}")
	private Integer slowDownDueToOverloadInterval;

	int chunkSequenceNumber;
	Reader reader;
	
	public Agent() {

	}

	public void run() {
		LOGGER.info("Log Forwarding Agent now running! agentId: " + agentId);
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(getReader());
		} catch (FileNotFoundException e) {
			LOGGER.severe("Agent could not find log file");
			e.printStackTrace();
		}

		String line;
		StringBuffer chunk = new StringBuffer();
		boolean keepReading = true;
		int numberOfLinesRead = 0;
		int timesSlept = 0;
		while (keepReading) {
			try {
				line = bufferedReader.readLine();
				LOGGER.info("Number of lines read so far: " + numberOfLinesRead + " content: " + line
						+ " current sequence number: " + chunkSequenceNumber);
				numberOfLinesRead++;
				if (line == null) {
					timesSlept++;
					Thread.sleep(defaultWaitInterval);
				} else {
					chunk.append(line).append("\n");
				}
				if ((line != null && numberOfLinesRead > defaultChunkSize)
						|| (timesSlept > maxSleepingTimes && chunk.length() > 0)) {
					try {
						invokeAggregationService(chunk.toString());
						chunk = new StringBuffer();
						chunkSequenceNumber++;
						numberOfLinesRead = 0;
					} catch (HttpStatusCodeException e) {
						if (e.getStatusCode() == HttpStatus.CONFLICT) {
							int latestChunkSequenceFromService = Integer.parseInt(e.getResponseBodyAsString());
							LOGGER.info(
									"Notified of duplicate chunk sent to service side; starting the next chunk from the latest service side sequence - " + latestChunkSequenceFromService);
							for (int i = 0; i < (latestChunkSequenceFromService + 1) * defaultChunkSize; i++) {
								bufferedReader.readLine();
							}
							chunk = new StringBuffer();
							chunkSequenceNumber = latestChunkSequenceFromService + 1;
						}
						if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
							Thread.sleep(slowDownDueToOverloadInterval);
						}
						if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
							keepReading = false;	
						}
					}
				}
			} catch (InterruptedException e) {
				LOGGER.severe("Exception while thread sleeping waiting for additional log lines");
				e.printStackTrace();
				keepReading = false;
			} catch (IOException e) {
				LOGGER.severe("IO Exception while attempting to read next log file line");
				e.printStackTrace();
				keepReading = false;
			}
		}

		try {
			bufferedReader.close();
		} catch (IOException e) {
			LOGGER.severe("Closing file buffered reader failed :( ");
			e.printStackTrace();
		}
	}

	public void invokeAggregationService(String content) {
		LOGGER.info("Invoking log aggregation service, sending the following chunk:\n" + content);
		RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
		LogChunk chunk = new LogChunk(content, chunkSequenceNumber, agentId);
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		restTemplate.postForEntity(aggregationServiceUri, chunk, LogChunk.class);
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
	public Reader getReader() throws FileNotFoundException {
		if (reader == null) {
			reader = new FileReader(logFilePath);
		}
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}
}
