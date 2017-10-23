package hlt;

public class Metadata {
	private String[] metadata;
	private int index = 0;

	public Metadata(String[] metadata) {
		this.metadata = metadata;
	}
	public String pop() {
		return metadata[index++];
	}
	public boolean isEmpty() {
		return index == metadata.length;
	}
}
