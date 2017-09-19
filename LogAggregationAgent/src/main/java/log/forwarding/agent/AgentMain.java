package log.forwarding.agent;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import log.forwarding.agent.config.AppConfig;

/**
 * @author doreenvanunu
 *
 *         Main entry point for the Log Forwarding Agent application - initiates
 *         spring context and spawns a forwarding agent
 */
public class AgentMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		context.scan("log.forwarding.agent");
		Agent agent = (Agent) context.getBean(Agent.class);
		if (args.length > 0) {
			agent.setAgentId(args[0]);
		}
		agent.run();
		context.close();
	}
}
