package hlt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DebugLog {
	private PrintWriter writer;
	private static DebugLog instance;

	private DebugLog(File file) {
		file.getAbsoluteFile().getParentFile().mkdirs();
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			this.writer = new PrintWriter(new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public PrintWriter getWriter() {
		return writer;
	}
	static void initialize(String filename) {
		if (instance != null) {
			throw new IllegalStateException("Already Initialized DebugLog"); // Fail Fast
		}
		instance = new DebugLog(new File(filename));
	}
	public static void log(String message) {
		instance.getWriter().println(message);
		instance.getWriter().flush();
	}
}
