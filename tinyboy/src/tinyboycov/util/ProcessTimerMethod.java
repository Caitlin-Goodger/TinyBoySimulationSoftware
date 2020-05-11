// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a SWEN221 assignment.
// You may not distribute it in any other way without permission.
package tinyboycov.util;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Used to enforce strict timeouts for long running programs. This is achieved
 * by isolating the program in question into a separate process which can then
 * be easily terminated when the timeout expires.
 *
 * @author David J. Pearce
 *
 */
public class ProcessTimerMethod {
	public static String JAVA_CMD = System.getProperty("java.home") + "/bin/java";
	public static String CLASSPATH = System.getProperty("java.class.path");

	public static Outcome exec(long timeout, String receiver, String method, Object... args) throws Throwable {

		// ===================================================
		// Construct command
		// ===================================================
		ArrayList<String> command = new ArrayList<>();
		command.add(JAVA_CMD);
		command.add("-ea"); // enable assertions by default
		command.add("-cp");
		command.add(CLASSPATH);
		command.add("tinyboycov.util.ProcessTimerMethod");

		// ===================================================
		// Construct the process
		// ===================================================
		ProcessBuilder builder = new ProcessBuilder(command);
		Process child = builder.start();
		try {
			// first, send over the method in question + args
			OutputStream output = child.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(output);
			oos.writeObject(receiver);
			oos.writeObject(method);
			oos.writeObject(args);
			oos.flush();
			output.flush();
			// second, read the result whilst checking for a timeout
			InputStream input = child.getInputStream();
			InputStream error = child.getErrorStream();
			boolean success = child.waitFor(timeout,TimeUnit.MILLISECONDS);
			byte[] stdout = readInputStream(input);
			byte[] stderr = readInputStream(error);
			return new Outcome(success ? child.exitValue() : null,stdout,stderr);
		} finally {
			// make sure child process is destroyed.
			child.destroy();
		}
	}

	private static byte[] readInputStream(InputStream input) throws IOException {
		byte[] buffer = new byte[1024];
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		while (input.available() > 0) {
			int count = input.read(buffer);
			output.write(buffer, 0, count);
		}
		return output.toByteArray();
	}

	public static void main(String[] args) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(System.in);
		int exitCode=0;
		try {
			String receiver = (String) ois.readObject();
			String name = (String) ois.readObject();
			Object[] arguments = (Object[]) ois.readObject();
			// now, find the object
			Class[] paramtypes = new Class[arguments.length];
			int i = 0;
			for (Object arg : arguments) {
				paramtypes[i++] = arg.getClass();
			}
			Class clazz = Class.forName(receiver);
			Object instance = clazz.newInstance();
			Method method = clazz.getMethod(name, paramtypes);
			method.invoke(instance, arguments);
		} catch(InvocationTargetException e) {
			e.getCause().printStackTrace();
			exitCode=-1;
		} catch (Exception e) {
			e.printStackTrace();
			exitCode=-2;
		}
		// Finally write the exit code
		System.exit(exitCode);

	}


	public static class Outcome {

		private final Integer exitCode;

		private final byte[] stdout;

		private final byte[] stderr;

		public Outcome(Integer exitCode, byte[] stdout, byte[] stderr) {
			this.exitCode = exitCode;
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public Integer exitCode() {
			return exitCode;
		}

		public byte[] getStdout() {
			return stdout;
		}

		public byte[] getStderr() {
			return stderr;
		}
	}
}
