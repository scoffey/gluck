package system;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que implementa metodos de libreria para gluck
 */
public class io {
	public static List<FileHandler> fileHandlers = new ArrayList<FileHandler>();

	public static void print(Object s) {
		System.out.println(s);
	}

	public static int str2i(String s) {
		return Integer.parseInt(s);
	}

	public static double str2r(String s) {
		return Double.parseDouble(s);
	}

	public static int strlen(String s) {
		return s.length();
	}

	public static String substring(String s, int start, int length) {
		try {
			return s.substring(start, start + length);
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	public static int open(String filename) {
		int ret = fileHandlers.size();
		fileHandlers.add(new FileHandler(filename));
		return ret;
	}

	public static void close(int f) {
		fileHandlers.get(f).close();
	}

	public static String readLine(int f) {
		return fileHandlers.get(f).readLine();
	}

	public static void writeLine(int f, String s) {
		fileHandlers.get(f).writeLine(s);
	}

	private static class FileHandler {
		private BufferedReader reader;
		private BufferedWriter writer;
		private File actualFile;

		private enum FileMode {
			READ, WRITE, NONE
		};

		private FileMode mode;
		private boolean initialized;

		public FileHandler(String filename) {
			super();
			actualFile = new File(filename);
			initialized = false;
			mode = FileMode.NONE;
		}

		public void writeLine(String s) {
			if (mode == FileMode.READ) {
				throw new RuntimeException("Can't write over a file that has already benn read");
			} else {
				if (!initialized) {
					try {
						this.writer = new BufferedWriter(new FileWriter(actualFile));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					initialized = true;
				}
				mode = FileMode.WRITE;
			}
			try {
				writer.write(s + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String readLine() {
			if (mode == FileMode.WRITE) {
				throw new RuntimeException("Can't read over a file that has already benn written in");
			} else {
				if (!initialized) {
					try {
						this.reader = new BufferedReader(new FileReader(actualFile));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					initialized = true;
				}
				mode = FileMode.READ;
			}
			try {
				return reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		public void close() {
			try {
				if (mode == FileMode.READ) {
					reader.close();
				} else {
					writer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private static void copy(File source, File dest) throws IOException {
			FileChannel in = null, out = null;
			try {
				in = new FileInputStream(source).getChannel();
				out = new FileOutputStream(dest).getChannel();

				long size = in.size();
				MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);

				out.write(buf);

			} finally {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			}
		}

	}
}
