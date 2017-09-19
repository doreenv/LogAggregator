package log.forwarding.agent;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class})
public class AgentTest extends TestCase {

	@Spy
	Agent agent;
	
	@Test
	public void testRunInternalServerError() throws UnsupportedEncodingException {
		HttpStatusCodeException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		Mockito.doThrow(exception).when(agent).invokeAggregationService(Mockito.anyString());
	    ReflectionTestUtils.setField(agent, "defaultChunkSize", 5);
	    ReflectionTestUtils.setField(agent, "defaultWaitInterval", 2000L);	    
		String content = "sample content";
		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content.getBytes("UTF-8")));
		agent.setReader(reader);
		agent.run();
	}
	
}
