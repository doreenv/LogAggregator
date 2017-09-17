package log.forwarding.agent;

public class AgentMain {
	public static void main(String[] args) {
		Agent agent;
		if (args.length > 0) {
			agent = new Agent(args[0]);
		} else {
			agent = new Agent();
		}
		agent.run();
	}
}
