package log.forwarding.agent;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AgentTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AgentTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AgentTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testRun()
    {
        assertTrue( true );
    }
}
