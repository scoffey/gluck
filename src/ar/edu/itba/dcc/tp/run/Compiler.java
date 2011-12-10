package ar.edu.itba.dcc.tp.run;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ar.edu.itba.dcc.tp.assembler.BytecodeGenerator;
import ar.edu.itba.dcc.tp.assembler.ClassInfoCollector;
import ar.edu.itba.dcc.tp.lexer.GluckLexer;
import ar.edu.itba.dcc.tp.lexer.Lexer;
import ar.edu.itba.dcc.tp.lexer.Token;
import ar.edu.itba.dcc.tp.lexer.TokenTypes;
import ar.edu.itba.dcc.tp.optimizer.Optimizer;
import ar.edu.itba.dcc.tp.parser.GluckParser;
import ar.edu.itba.dcc.tp.parser.ParserAdapter;
import ar.edu.itba.dcc.tp.semantic.CodeGenerator;
import ar.edu.itba.dcc.tp.semantic.DuplicatedSymbolException;
import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.SemanticAnalyzer;
import ar.edu.itba.dcc.tp.semantic.SymbolTable;
import ar.edu.itba.dcc.tp.semantic.SymbolTableBuilder;
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.util.ConsoleProcessLogger;
import ar.edu.itba.dcc.tp.util.ProcessLogger;

/**
 * El compilador. Se lo instancia con un lexer, un parser y un nombre de archivo
 * a compilar. El compilador se maneja como un singleton, se lo puede crear a
 * través del método estático <code>createCompiler</code>,y luego utilizarlo
 * a través de <code>getInstance</code>.
 */
public class Compiler {

	private Set<String> inputFiles;
	private CompilerModes mode;
	private ProcessLogger logger;
	public CompilerInputFile currentFile;
	private Set<String> optimizationOptions;

	private static Compiler me = new Compiler(); /* singleton */

	/**
	 * Modos en los que puede ejecutarse el compilador. Lexer realiza solamente
	 * el análisis léxico, imprimiendo los tokens por salida estándar. Parser
	 * hace el análisis léxico y sintáctico. Dependencies hace todo lo anterior,
	 * y además imprime por salida estándar las dependencias entre los archivos.
	 */
	public enum CompilerModes {
		MODE_LEXER, MODE_PARSER, MODE_DEPENDENCIES, MODE_DEPENDENCIES_REC, MODE_SEMANTIC, MODE_SYMBOL_TABLE, MODE_IC, MODE_BYTECODES, MODE_JASMIN
	};

	/**
	 * Punto de entrada de la aplicación.
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			return;
		}
		Compiler compiler = Compiler.getInstance();
		compiler.logger = new ConsoleProcessLogger();

		try {
			String fileName = setCompilerMode(args);
			if (compiler.mode == CompilerModes.MODE_LEXER) {
				compiler.dumpTokens(fileName);
			} else {
				compiler.compileFile(fileName, 0, 0);
			}
		} catch (IllegalArgumentException e) {
			usage();
			return;
		}
	}

	private static void usage() {
		System.err.println("Usage: ");
		System.err.println("Lexical analysis: ");
		System.err.println("\tjava -jar dcc.jar -L <filename>");
		System.err.println("Lexical-syntactical analysis: ");
		System.err.println("\tjava -jar dcc.jar -S <filename>");
		System.err.println("Lexical-syntactical analysis in dependency debugging mode: ");
		System.err.println("\tjava -jar dcc.jar -D [-r] <filename>");
		System.err.println("Symbol table: ");
		System.err.println("\tjava -jar dcc.jar -T <filename>");
		System.err.println("Semantic analysis: ");
		System.err.println("\tjava -jar dcc.jar -E <filename>");
		System.err.println("Intermediate code generation and optimization: ");
		System.err.println("\tjava -jar dcc.jar -I <filename> [-f<optimization> ... | -o]");
		System.err.println("Assembler: ");
		System.err.println("\tjava -jar dcc.jar -A <filename>");
		System.err.println("Compile: ");
		System.err.println("\tjava -jar dcc.jar -C <filename>");
	}

	/**
	 * Establece el modo del Compiler (singleton) y devuelve el nombre del
	 * archivo según los argumentos parseados.
	 * 
	 * @param args Argumentos de la línea de comandos.
	 * @return Nombre del archivo a parsear.
	 * @throws IllegalArgumentException Si los argumentos son incorrectos.
	 */
	public static String setCompilerMode(String[] args) throws IllegalArgumentException {
		String arg = args[args.length - 1];
		CompilerModes mode;

		if (args[0].equals("-L")) {
			mode = CompilerModes.MODE_LEXER;
			if (args.length > 2) {
				throw new IllegalArgumentException();
			}
		} else if (args[0].equals("-S")) {
			mode = CompilerModes.MODE_PARSER;
		} else if (args[0].equals("-D")) {
			if (args.length == 2) {
				mode = CompilerModes.MODE_DEPENDENCIES;
			} else if (args.length == 3 && args[1].equals("-r")) {
				mode = CompilerModes.MODE_DEPENDENCIES_REC;
			} else {
				throw new IllegalArgumentException();
			}
		} else if (args[0].equals("-T")) {
			mode = CompilerModes.MODE_SYMBOL_TABLE;
			if (args.length > 2) {
				throw new IllegalArgumentException();
			}
		} else if (args[0].equals("-E")) {
			mode = CompilerModes.MODE_SEMANTIC;
			if (args.length > 2) {
				throw new IllegalArgumentException();
			}
		} else if (args[0].equals("-A")) {
			mode = CompilerModes.MODE_JASMIN;
			arg = args[1];
			setCompilerModeForOptimizations(args);
		} else if (args[0].equals("-C")) {
			mode = CompilerModes.MODE_BYTECODES;
			arg = args[1];
			setCompilerModeForOptimizations(args);
		} else if (args[0].equals("-I")) {
			mode = CompilerModes.MODE_IC;
			arg = args[1];
			setCompilerModeForOptimizations(args);
		} else {
			throw new IllegalArgumentException();
		}

		Compiler.getInstance().setMode(mode);
		return arg;
	}
	
	public static void setCompilerModeForOptimizations(String[] args) {
		Compiler compiler = Compiler.getInstance();
		if (args.length > 2) {
			if (args[2].equals("-o")) {
				compiler.setAllOptimizationOptions();
				if (args.length > 3) {
					throw new IllegalArgumentException();
				}
			} else {
				for (int i = 2; i < args.length; i++) {
					if (args[i].startsWith("-f")) {
						compiler.addOptimizationOption(args[i].substring(2));
					} else {
						throw new IllegalArgumentException();
					}
				}
			}
		}
	}

	public String getFilename(String arg) {
		String[] aux = arg.split("/");
		String fileName = aux[aux.length - 1];
		return fileName;
	}

	public void dumpTokens(String fileName) {
		Token<TokenTypes> token;
		Reader in;
		Lexer lexer = new GluckLexer();
		lexer.setFeedback(logger);

		try {
			in = new FileReader(new File(fileName/*.replace('/', File.separatorChar)*/));
		} catch (Exception e) {
			logger.showError(fileName, 0, 0, "File not found: " + fileName);
			return;
		}

		lexer.process(in, fileName);

		while ((token = lexer.nextToken()) != null) {
			System.out.println(token);
		}
	}

	public void setMode(CompilerModes mode) {
		Compiler.getInstance().mode = mode;
	}

	public CompilerModes getMode() {
		return mode;
	}

	/**
	 * @return La única instancia del compilador.
	 */
	public static Compiler getInstance() {
		return me;
	}

	/**
	 * Crea una instancia del compilador.
	 */
	private Compiler() {
		super();
		this.inputFiles = new HashSet<String>();
		this.mode = null;
		this.optimizationOptions = new TreeSet<String>();
	}

	public void addOptimizationOption(String s) {
		if (optimizationOptions == null) // significa "all" options
			return;
		optimizationOptions.add(s);
	}

	public void setAllOptimizationOptions() {
		optimizationOptions.clear();
		optimizationOptions = null;
	}

	public Set<String> getOptimizationOptions() {
		return optimizationOptions;
	}

	/**
	 * Agrega un archivo al stack de archivos a compilar.
	 * 
	 * @param fileName Nombre (con path incluido) del archivo a compilar.
	 * @return La tabla de símbolos que generó el archivo parseado.
	 */
	public SymbolTable compileFile(String fileName, int line, int column) {
		String file = null;

		fileName = fileName.replace(File.separatorChar, '/');
		
		/* system.io */
		if (fileName.contains("system/io.gluck")) {
			SymbolTable ioSymbolTable = new SymbolTable();
			ClassInfoCollector collector = null;
			
			try {
			collector = new ClassInfoCollector(this.currentFile.getPath()
					+ this.currentFile.getLocationPath() + "system/io");
			} catch (IOException e) {
				logger.showError(currentFile.getFileName(), line, column, "Cannot find Gluck io library in current project.");
				return null;
			}
			for (String method : collector.getStaticMethods()) {
				try {
					ioSymbolTable.addFunction(new FunctionSymbol(method, "system/io", true));
				} catch (DuplicatedSymbolException e) {
					// Nunca debe pasar
					e.printStackTrace();
				}
			}
			return ioSymbolTable;
		}

		/* Verificar inclusion recursiva. */
		if (inputFiles.contains(fileName)) {
			logger.showWarning(currentFile.getFileName(), line, column, ConsoleProcessLogger.RECURSIVE_INCLUSION
					+ fileName);
			return null;
		}

		Reader input = null;



		/* Abro archivo de entrada */
		try {
			file = fileName;
			input = new FileReader(new File(file/*.replace('/', File.separatorChar)*/));
		} catch (Exception e) {
			if (mode != CompilerModes.MODE_LEXER && mode != CompilerModes.MODE_JASMIN
					&& mode != CompilerModes.MODE_BYTECODES) {
				logger.showError(fileName, line, column, "File not found: " + file);
				return null;
			} else if (mode == CompilerModes.MODE_LEXER) {
				return null;
			} else {
				input = null;
			}
		}
		
		/* Validar que al menos existe el .gluck o el .class */
		File classFile = new File(file.replace(".gluck", ".class"));
		File gluckFile = new File(file);
		if (!gluckFile.exists() && ! classFile.exists()){
			logger.showError(fileName, line, column, "File not found");
			return null;
		}


		/* Imprimir la dependencia ? */
		if (currentFile != null
				&& (this.mode == CompilerModes.MODE_DEPENDENCIES || this.mode == CompilerModes.MODE_DEPENDENCIES_REC)) {

			logger.showInfo(currentFile.getFileName(), line, column, currentFile.getFileName() + " depends on "
					+ fileName);
		}

		if (currentFile == null || this.mode == CompilerModes.MODE_DEPENDENCIES_REC
				|| this.mode == CompilerModes.MODE_IC || this.mode == CompilerModes.MODE_SEMANTIC
				|| this.mode == CompilerModes.MODE_SYMBOL_TABLE || this.mode == CompilerModes.MODE_BYTECODES
				|| this.mode == CompilerModes.MODE_JASMIN) {


			CodeGenerator codeGenerator = null;
			SemanticAnalyzer symbolTableBuilder = null;
			CompilerInputFile aux = currentFile;
			List<Instruction> instructionList = null;
			if ((this.mode != CompilerModes.MODE_BYTECODES && mode != CompilerModes.MODE_JASMIN)
					|| (gluckFile.exists() || getFileStackSize() == 0 || !classFile.exists())) {
				Lexer lexer = new GluckLexer();
				ParserAdapter parser = new GluckParser();
				symbolTableBuilder = new SymbolTableBuilder(parser, logger);
				Optimizer optimizer = new Optimizer(optimizationOptions);

				parser.setSemanticAnalyzer(symbolTableBuilder);
				parser.setFeedback(logger);
				lexer.setFeedback(logger);

				/* Primera pasada: construimos tabla de simbolos. */
				currentFile = new CompilerInputFile(fileName, input);
				this.inputFiles.add(fileName);
				lexer.process(input, fileName);


				parser.parse(lexer);


				/* Segunda pasada, todo el análisis y generación de código. */
				codeGenerator = new CodeGenerator(parser, logger);
				codeGenerator.setSymbolTable(symbolTableBuilder.getSymbolTable());

				parser.setSemanticAnalyzer(codeGenerator);
				try {
					input.close();
					input = new FileReader(new File(file/*.replace('/', File.separatorChar)*/));
				} catch (Exception e) {
					/* Nunca debe ocurrir, porque ya lo abrí antes. */
					System.out.println();
					e.printStackTrace();
				}
				lexer.process(input, fileName);
				parser.parse(lexer);

				if (mode == CompilerModes.MODE_SYMBOL_TABLE && getFileStackSize() == 1
						&& !codeGenerator.hasReportedErrors() && !symbolTableBuilder.hasReportedErrors()) {
					codeGenerator.getSymbolTable().print(0);
				}

				/* Obtenemos la lista de instrucciones de codigo intermedio. */
				instructionList = codeGenerator.getInstructions();

				if (mode == CompilerModes.MODE_IC && getFileStackSize() == 1 && !codeGenerator.hasReportedErrors()
						&& !symbolTableBuilder.hasReportedErrors()) {

					if (optimizer.size() == 0) {
						// por eficiencia (para no generar el FlowDigraph)
						for (Instruction i : instructionList) {
							System.out.println(i.toString());
						}
					} else {
						// Esto optimiza con las opciones seleccionadas (o
						// ninguno)
						// y hace el dump del código intermedio
						optimizer.optimize(instructionList);
					}
				}
			} else {
				// System.out.println("compileFile: getFileStackSize() = " +
				// getFileStackSize());
				/*
				 * Dado un .class, generar la tabla de simbolos que este exporta
				 */
				String directoryName = null;
				if (file.lastIndexOf('/') == -1) {
					directoryName = ".";
				} else {
					directoryName = file.substring(0, file.lastIndexOf('/'));
				}
				File directory = new File(directoryName/*.replace('/', File.separatorChar)*/);

				// System.out.println(filename.substring(0, filename.length() -
				// 6) + "XXXX");

				SymbolTable symbolTable = new SymbolTable();
				for (String s : directory.list()) {
					if (s.matches(".*" + fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length() - 6)
							+ ".*" + ".class")) {
						symbolTable.addAllSymbols(getDotClassSymbolTable(directoryName + "/"
								+ s.substring(0, s.length() - 6)));
					}
				}
				return symbolTable;
			}
			if (!symbolTableBuilder.hasReportedErrors() && !codeGenerator.hasReportedErrors()
					&& (mode == CompilerModes.MODE_JASMIN || mode == CompilerModes.MODE_BYTECODES)) {
				String moduleName = currentFile.fileName.substring(currentFile.fileName.lastIndexOf('/') + 1,
						currentFile.fileName.lastIndexOf("."));
				String newFilename = null;
				if (this.currentFile.getFileName().lastIndexOf('/') == -1) {
					newFilename = ".";
				} else {
					newFilename = this.currentFile.getFileName().substring(0,
							this.currentFile.getFileName().lastIndexOf('/'));
				}

				BytecodeGenerator byteCodeGenerator = new BytecodeGenerator(newFilename, moduleName, this.currentFile
						.getLocation(), codeGenerator.getSymbolTable(), instructionList);
				byteCodeGenerator.generateJasmin(mode != CompilerModes.MODE_BYTECODES);
				if (mode == CompilerModes.MODE_BYTECODES) {
					byteCodeGenerator.generateBytecodes();
				}
			}

			currentFile = aux;
			this.inputFiles.remove(fileName);

			return codeGenerator.getSymbolTable();
		}
		return null;
	}

	public String generateFilename(String use) {
		return this.currentFile.getPath() + this.currentFile.getLocationPath() + use;
	}

	public void setCurrentLocation(String location) {
		this.currentFile.setLocation(location);
	}

	public String getCurrentLocation() {
		if (this.inputFiles.size() == 0) {
			return "";
		} else {
			return this.currentFile.getLocation();
		}
	}

	public int getFileStackSize() {
		return inputFiles.size();
	}

	/**
	 * Retorna una tabla de simbolos con los simbolos definidos en un .class
	 * (ipos definidos por el usuario y funciones).
	 * 
	 * @param fileName Nombre del ".class" de donde se obtiene la tabla de
	 *            simbolos.
	 * @return SymbolTable del ".class" en cuestion.
	 */
	private SymbolTable getDotClassSymbolTable(String fileName) {
		SymbolTable symbolTable = new SymbolTable();
		ClassInfoCollector collector = null;
		try {
			collector = new ClassInfoCollector(fileName);
		} catch (IOException e) {
			logger.showError(currentFile.getFileName(), 0, 0, "Missing file: " + fileName);
		}
		try {
			for (String method : collector.getStaticMethods()) {
				symbolTable.addFunction(new FunctionSymbol(method, fileName.contains("./") ? fileName.substring(2)
						: fileName, !method.contains("constraint__")));
			}

		} catch (DuplicatedSymbolException e) {
			// Nunca debe pasar
			e.printStackTrace();
		}

		return symbolTable;

	}

	public class CompilerInputFile {
		private String fileName;
		private Reader reader;
		private String location;

		public CompilerInputFile(String fileName, Reader reader, String location) {
			super();
			this.fileName = fileName;
			this.reader = reader;
			this.location = location;
		}

		public CompilerInputFile(String fileName, Reader reader) {
			this(fileName, reader, "");
		}

		public String getFileName() {
			return fileName;
		}

		public String getLocation() {
			return location.equals("") ? "" : location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public Reader getReader() {
			return reader;
		}

		public String getPath() {
			/* Si ya estaba procesando un archivo, calculo el path del nuevo. */
			String path = "";
			if (currentFile != null) {
				String[] parts = currentFile.getFileName().split("/");
				for (int i = 0; i < parts.length - 1; i++) {
					path += parts[i] + "/";
				}
			}
			return path;

		}

		public String getLocationPath() {
			if (location.equals("")) {
				return "./";
			} else {
			return "./" + location.replaceAll("[^/]+", "").replaceAll("/", "../") + "../";
			}
			}
	}

}
