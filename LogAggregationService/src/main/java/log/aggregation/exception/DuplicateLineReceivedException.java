package log.aggregation.exception;

public class DuplicateLineReceivedException extends Exception {
	
	private static final long serialVersionUID = -983277756430988512L;
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
