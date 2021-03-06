package log.aggregation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import log.aggregation.exception.DuplicateLineReceivedException;
import log.aggregation.exception.TooManyPendingChunksException;
import log.dto.LogChunk;

/**
 * @author doreenvanunu
 * 
 *         A service that recreates log files based on log chunks received from
 *         a given agent
 */
@Configuration
@PropertySource("classpath:service.properties")
public class LogAggregationService {

	private final static Logger LOGGER = Logger.getLogger(LogAggregationService.class.getName());

	/**
	 *  This collection holds chunks that were received out of order, so that a
	 *  previous chunk is missing; those will be written to the log in order once the
	 *  missing chunk is received
	 */
	private TreeSet<LogChunk> chunksReceivedAndPending;
	@Value("${defaultMaxPendingChunks}")
	private Integer defaultMaxPendingChunks;
	@Value("${defaultRequestResendLimit}")
	private Integer defaultRequestResendLimit;
	@Value("${defaultDir}")
	private String defaultDir;
	private int latestSequenceReceived = -1;
	private Writer writer;

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	public LogAggregationService() {
		chunksReceivedAndPending = new TreeSet<LogChunk>(new Comparator<LogChunk>() {
			public int compare(LogChunk chunk1, LogChunk chunk2) {
				return chunk1.getSequenceNumber() - chunk2.getSequenceNumber();
			}
		});
	}
	
	/**
	 *  Adds a new log chunk to the log file. If chunk is not received in order - if
	 *  previous chunks are missing - chunk writing will be pended until the previous
	 *  missing chunks are received. They will then be written to the file in order
	 * 
	 * @param chunk
	 * @throws TooManyPendingChunksException
	 * @throws IOException
	 * @throws DuplicateLineReceivedException
	 */
	public void addLogChunk(LogChunk chunk)
			throws TooManyPendingChunksException, IOException, DuplicateLineReceivedException {
		LOGGER.info("Latest sequence received: " + latestSequenceReceived);
		if (latestSequenceReceived + 1 < chunk.getSequenceNumber()) {
			chunksReceivedAndPending.add(chunk);
		} else if (latestSequenceReceived + 1 == chunk.getSequenceNumber()) {
			latestSequenceReceived = chunk.getSequenceNumber();
			getWriter().write(chunk.getContent());
			LogChunk nextPendingChunk = null;
			if (chunksReceivedAndPending.size() > 0) {
				nextPendingChunk = chunksReceivedAndPending.first();
			}
			while (nextPendingChunk != null && latestSequenceReceived + 1 == nextPendingChunk.getSequenceNumber()) {
				getWriter().write(chunk.getContent());
				latestSequenceReceived = nextPendingChunk.getSequenceNumber();
				chunksReceivedAndPending.pollFirst();
				if (chunksReceivedAndPending.size() > 0) {
					nextPendingChunk = chunksReceivedAndPending.first();
				}
			}
		} else if (latestSequenceReceived > chunk.getSequenceNumber()) {
			LOGGER.severe("Received duplicate chunk. Latest chunk received: " + latestSequenceReceived
					+ ", current chunk sequence: " + chunk.getSequenceNumber());
			throw new DuplicateLineReceivedException(latestSequenceReceived);
		}
		if (chunksReceivedAndPending.size() > defaultRequestResendLimit) {
			// TODO request resend
		}
		if (chunksReceivedAndPending.size() > defaultMaxPendingChunks) {
			throw new TooManyPendingChunksException();
		}

		LOGGER.finest("Printing all pending chunks");
		Iterator<LogChunk> iterator = chunksReceivedAndPending.iterator();
		while (iterator.hasNext()) {
			LogChunk pendingChunk = iterator.next();
			LOGGER.finest(Integer.toString(pendingChunk.getSequenceNumber()));
		}

		getWriter().flush();
	}

	@PostConstruct
	public void initializeTargetFile() {
		File folder = new File(defaultDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}
	}

	public void setAgentId(String agentId) throws IOException {
		File logFile = createLogFile(agentId);
		setWriter(new BufferedWriter(new FileWriter(logFile, true)));
	}
	
	private File createLogFile(String agentId) throws IOException {
		File logFile = new File(defaultDir + "/" + agentId + ".log");
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
		return logFile;
	}

	@PreDestroy
	public void releaseResources() throws IOException {
		getWriter().close();
	}
	
	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}
}


