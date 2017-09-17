
package log.dto;

public class LogChunk {

	private String content;
	private int sequenceNumber;
	private String agentId;
	
	public LogChunk() {
		
	}
	
	public LogChunk(String content, int sequenceNumber, String agentId) {
		this.content = content;
		this.sequenceNumber = sequenceNumber;
		this.agentId = agentId;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public String getAgentId() {
		return agentId;
	}
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
}
