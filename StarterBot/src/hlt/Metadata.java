package hlt;

import java.util.StringTokenizer;

public class Metadata {
	private StringTokenizer metadataTokenizer;

	public Metadata(String metadata) {
		this.metadataTokenizer = new StringTokenizer(metadata);
	}
	public String pop() {
		return metadataTokenizer.nextToken();
	}
	public boolean isEmpty() {
		return !metadataTokenizer.hasMoreTokens();
	}
}
