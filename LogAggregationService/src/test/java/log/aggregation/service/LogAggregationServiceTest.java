package log.aggregation.service;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import junit.framework.TestCase;
import log.aggregation.exception.DuplicateLineReceivedException;
import log.aggregation.exception.TooManyPendingChunksException;
import log.dto.LogChunk;

@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class LogAggregationServiceTest extends TestCase {
	
	@Spy
	LogAggregationService logAggregationService;
	
	@Test
	public void testAddLogChunk() throws IOException, TooManyPendingChunksException, DuplicateLineReceivedException {
		Mockito.doNothing().when(logAggregationService).initializeTargetFile();
		Mockito.doNothing().when(logAggregationService).setAgentId(Mockito.anyString());	
		Mockito.doCallRealMethod().when(logAggregationService).addLogChunk(Mockito.any(LogChunk.class));
	    ReflectionTestUtils.setField(logAggregationService, "defaultRequestResendLimit", 100);
	    ReflectionTestUtils.setField(logAggregationService, "defaultMaxPendingChunks", 5);	    
		LogChunk chunk = new LogChunk("Sample content", 0, "testAgent");
		StringWriter stringWriter = new StringWriter();
		logAggregationService.setWriter(stringWriter);
		logAggregationService.addLogChunk(chunk);
		Assert.assertEquals(stringWriter.toString(), "Sample content");
	}

}
