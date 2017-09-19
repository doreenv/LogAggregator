package log.aggregation.exception;

public class DuplicateLineReceivedException extends Exception {
	
	int latestSequenceReceived;

	public DuplicateLineReceivedException(int latestSequenceReceived) {
		this.latestSequenceReceived = latestSequenceReceived;
	}
	
	public int getLatestSequenceReceived() {
		return latestSequenceReceived;
	}

	public void setLatestSequenceReceived(int latestSequenceReceived) {
		this.latestSequenceReceived = latestSequenceReceived;
	}
	
}
