package log.forwarding.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import log.dto.LogChunk;

public class Agent {

	private final static Logger LOGGER = Logger.getLogger(Agent.class.getName());

	private String logFilePath = "src/main/resources/logfile.log";
	private long defaultWaitInterval = 2000;
	private int defaultChunkSize = 4;
	private String defaultAgentId = "agent1";
	private String agentId;
	private String aggregationServiceUri = "http://localhost:8080/logAggregation/";
	private int maxSleepingTimes = 10;
	int chunkSequenceNumber;

	public Agent() {
		this.agentId = defaultAgentId;
	}

	public Agent(String agentId) {
		this.agentId = agentId;
	}

	public void run() {
		LOGGER.info("Log Forwarding Agent now running!");
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(logFilePath));
		} catch (FileNotFoundException e) {
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
					invokeAggregationService(chunk.toString());
					chunk = new StringBuffer();
					chunkSequenceNumber++;
					numberOfLinesRead = 0;
				}
			} catch (InterruptedException | IOException e) {
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

	private void invokeAggregationService(String content) {
		LOGGER.info("Invoking log aggregation service, sending the following chunk:\n " + content);
		RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
		LogChunk chunk = new LogChunk(content, chunkSequenceNumber, agentId);
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		restTemplate.postForObject(aggregationServiceUri, chunk, LogChunk.class);
	}
}
