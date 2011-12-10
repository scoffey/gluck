package ar.edu.itba.dcc.tp.assembler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.DuplicatedSymbolException;
import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;
import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.SymbolTable;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;
import ar.edu.itba.dcc.tp.semantic.symbols.ArrayTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.ArrayTypeSymbolComponent;
import ar.edu.itba.dcc.tp.semantic.symbols.BoolTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.DoubleTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.IntTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.ObjectWrapperTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.StringTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.Symbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UserDefinedTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UserDefinedTypeSymbolField;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;

/**
 * Esta clase se encagarga de generar el archivo que interpreta Jasmin
 */
public class BytecodeGenerator {

	private String path;

	/* Nombre de la clase */
	private String className;

	/* Nombre del paquete (location) */
	private String packageName;

	/* Tabla de símbolos que recibe del Compiler */
	private SymbolTable symbolTable;

	/* Lista de instrucciones que recibe del Compiler */
	private List<Instruction> instructions;

	/* Tabla de símbolos de la funcion que se esta procesando */
	private SymbolTable functionSymbolTable;

	/*
	 * Tablita de símbolos que asociaun identificador con un objeto Variable.
	 * Dicho objeto extiende de VariableSymbol agregando un campo que determina
	 * el numero de variable en la JVM
	 */
	private RegistersSymbolTable vars;

	/* Funcion para la que se quiere generar el .class */
	private FunctionSymbol functionSymbol;

	/* Tipo definido por el usuario para el que se quiere generar el .class */
	private UserDefinedTypeSymbol userDefinedTypeSymbol;

	/* Numero de instruccion que se esta procesando */
	private int currentInstructionNumber;

	private boolean hasStaticInitBlock = false;

	private static final String STACK_SIZE = "1000";
	private static final String LOCALS_SIZE = "1000";

	private List<String> headerInstructions;
	private List<String> fieldsInstructions;
	private List<String> methodsInstructions;
	private List<String> generalInstructions;
	private PrintStream file;

	private String currentFunctionName;

	private enum BytecodeGeneratorState {
		INIT, NORMAL
	};

	private BytecodeGeneratorState state;

	public BytecodeGenerator(String path, String className, String packageName, SymbolTable symbolTable,
			List<Instruction> instructions) {

		super();
		this.path = magic(path);
		if (this.path.equals("")) {
			this.path = ".";
		}
		this.className = className;
		this.packageName = magic(packageName);

		this.symbolTable = symbolTable;
		this.instructions = instructions;
		this.functionSymbolTable = null;
		this.vars = new RegistersSymbolTable();

		try {
			file = new PrintStream(new File(path + "/" + className + ".j"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.headerInstructions = new ArrayList<String>();
		this.fieldsInstructions = new ArrayList<String>();
		this.methodsInstructions = new ArrayList<String>();
		this.generalInstructions = new ArrayList<String>();
	}

	public static String magic(String packageName) {
		String[] barras = packageName.split("/");
		String[] output = new String[barras.length];
		int index = 0;

		for (String b : barras) {
			if (b.equals("."))
				continue;
			if (b.equals("")) {
				continue;
			}
			if (b.equals("..")) {
				index--;
				if (index < 0)
					throw new RuntimeException("Wrong location or compiler execution path.");
			} else {
				output[index++] = b;
			}
		}

		String output2 = "";
		for (int i = 0; i < index; i++) {
			output2 += output[i];
			output2 += "/";
		}

		if (output2.equals("/")) {
			return "";
		}
		return output2;
	}

	/**
	 * Este constructor se ustiliza cuando se quiere definir una clase inner
	 * para el valor de retornod e una funcion.
	 * 
	 * @param className
	 * @param packageName
	 * @param functionSymbol
	 */
	public BytecodeGenerator(String path, String className, String packageName, SymbolTable symbolTable,
			FunctionSymbol functionSymbol) {
		this(path, className, packageName, symbolTable, new ArrayList<Instruction>());
		this.functionSymbol = functionSymbol;
	}

	/**
	 * Este constructor se utiliza cuando se quiere definir una clase inner para
	 * un tipo definido por el usuario.
	 * 
	 * @param className
	 * @param packageName
	 * @param userDefinedTypeSymbol
	 */
	public BytecodeGenerator(String path, String className, String packageName, SymbolTable symbolTable,
			UserDefinedTypeSymbol userDefinedTypeSymbol) {
		this(path, className, packageName, symbolTable, new ArrayList<Instruction>());
		this.userDefinedTypeSymbol = userDefinedTypeSymbol;
	}

	/**
	 * Genera el archivo con directivas para jasmin.
	 */
	public void generateJasmin(boolean dumpJasminDirectives) {
		header();
		/* Compilar la clase que contiene los valores de retorno de una funcion */
		if (functionSymbol != null) {
			for (VariableSymbol outputParam : functionSymbol.getOutputParams()) {
				defineNonStaticField(packageName + className.substring(0, className.lastIndexOf("$")), outputParam,
						false);
			}
			/* Constructor default */
			defineDefaultConstructor();
		} else if (userDefinedTypeSymbol != null) {
			for (VariableSymbol field : userDefinedTypeSymbol.getComponents()) {
				defineNonStaticField(packageName + className, field, true);
			}

			/* Constructor default */
			defineDefaultConstructor();
		} else {
			/* Compilar un modulo */
			Instruction i;
			currentInstructionNumber = 0;
			while (currentInstructionNumber < instructions.size()) {
				i = instructions.get(currentInstructionNumber++);

				/* Si la instruccion tiene una etiqueta puede ser una funcion */
				if (i.getLabel() != null) {
					if (symbolTable.getFunctionSymbol(i.getLabel(), true) != null
							|| symbolTable.getFunctionSymbol(i.getLabel(), false) != null) {
						defineSubroutine(i);
					} else if (i.getLabel().equals("_init")) {
						state = BytecodeGeneratorState.INIT;
						hasStaticInitBlock = true;
						addGeneralInstruction(".method static <clinit>()V");
						generateLocal(generalInstructions);
					} else if (i.getLabel().equals("_init_end")) {
						state = BytecodeGeneratorState.NORMAL;
						if (hasStaticInitBlock) {
							addGeneralInstruction("return");
							addGeneralInstruction(".end method");
						}
						continue;
					} else if (i.getLabel().equals("_end_init_params")) {
						if (currentFunctionName != null && !currentFunctionName.equals("start")) {
							/*
							 * Obtener la funcion (o constraint) que define la
							 * etiqueta
							 */
							FunctionSymbol currentFunctionSymbol = symbolTable.getFunctionSymbol(currentFunctionName,
									true);
							if (currentFunctionSymbol == null) {
								currentFunctionSymbol = symbolTable.getFunctionSymbol(currentFunctionName, false);
							}

							/* Inicializar las variables de salida */
							for (VariableSymbol outputParam : currentFunctionSymbol.getOutputParams()) {
								Variable variableOutputParam = vars.getVariable("#" + outputParam.getId());
								initialize(variableOutputParam);
							}
						}

					} else {
						addGeneralInstruction(i.getLabel() + ":");
					}
				}

				/*
				 * Procesar las instrucciones de codigo intermedio segun
				 * corresponda
				 */
				switch (i.getInstructionType()) {
				case ADD:
					handleAddInstruction(i);
					break;
				case ASSIGN:
					handleAssignInstruction(i);
					break;
				case CALL:
					handleCallInstruction(i);
					break;
				case DIV:
					handleDivInstruction(i);
					break;
				case GOTO:
					handleGotoInstruction(i);
					break;
				case IF_EQ:
					handleIfeqInstruction(i);
					break;
				case IF_GT:
					handleIfgtInstruction(i);
					break;
				case IF_GTE:
					handleIfgteInstruction(i);
					break;
				case IF_LT:
					handleIfltInstruction(i);
					break;
				case IF_LTE:
					handleIflteInstruction(i);
					break;
				case IF_NEQ:
					handleIfneqInstruction(i);
					break;
				case MOD:
					handleModInstruction(i);
					break;
				case MULT:
					handleMultInstruction(i);
					break;
				case PARAM:
					handleParamInstruction(i);
					break;
				case RET:
					handleRetInstruction(i);
					break;
				case SUB:
					handleSubInstruction(i);
					break;
				}
			}
		}

		/*
		 * Pedirle al InstructionCollector que baje las instrucciones al archivo
		 * ".j"
		 */
		if (dumpJasminDirectives) {
			file = System.out;
			dump();
			/* Borrar el .j */
			File jasminFile = new File(packageName + className + ".j");
			jasminFile.delete();

		} else {
			dump();
		}
	}

	/**
	 * Dado un archivo con las directivas jasmin, invoca a dicho assembler para
	 * generar el ".class".
	 */
	public void generateBytecodes() {
		/* Generar el .class */
		String outpath = packageName.length() >= path.length() ? null : path.substring(0, path.length()
				- packageName.length());

		if (outpath != null) {
			jasmin.Main.main(new String[] { path + "/" + className + ".j", "-d", outpath, "-g" });
		} else {
			jasmin.Main.main(new String[] { path + "/" + className + ".j", "-g" });
		}

		/* Borrar el .j */
		File jasminFile = new File(path + "/" + className + ".j");
		jasminFile.delete();
	}

	/**
	 * Genera el codigo con la definicion de la clase, superclase, sources.
	 * Tambien se encarga de definir mienbros para todas las variables globales
	 * y clases inner para los valores de retorno de funciones y tipos definidos
	 * por el usuario.
	 */
	private void header() {
		String sourceFile = null;
		if (functionSymbol != null || userDefinedTypeSymbol != null) {
			sourceFile = className.substring(0, className.lastIndexOf('$'));
		} else {
			sourceFile = className;
		}

		/* Instrucciones que van al header del archivo */
		addHeaderInstruction(".class public " + packageName + className);
		addHeaderInstruction(".super java/lang/Object");

		/* Si se esta procesando un modulo */
		if (userDefinedTypeSymbol == null && functionSymbol == null) {
			/* Definir miembros para las variables globales */
			for (VariableSymbol variableSymbol : symbolTable.getVariableSymbols()) {
				defineStaticField(packageName + className, variableSymbol);
			}

			/* Definir classes inner para retorno de valores de los metodos */
			for (FunctionSymbol functionSymbol : symbolTable.getFunctionSymbols()) {
				if (!functionSymbol.isNative() && !functionSymbol.isExternal()
						&& functionSymbol.getOutputParams().size() > 0) {
					createInnerClass(functionSymbol);
				}
			}

			/* Definir clases inner para tipos definidos por el usuario */
			for (UserDefinedTypeSymbol userDefinedTypeSymbol : symbolTable.getUserDefinedTypeSymbols()) {
				if (!userDefinedTypeSymbol.isExternal()) {
					createInnerClass(userDefinedTypeSymbol);
					addGetterAndSetters(userDefinedTypeSymbol);
				}

			}
		}

	}

	/**
	 * Agrega al RegisterSymbolTable los metodos exportados por una clase inner
	 * de un tipo definido por el usuario (getters y setters).
	 * 
	 * @param userDefinedTypeSymbol UserDefinedTypeSymbol del cual se quiere
	 *            importar los getters y setters.
	 */
	private void addGetterAndSetters(UserDefinedTypeSymbol userDefinedTypeSymbol) {
		ClassInfoCollector collector = null;
		String workingDir = packageName.length() >= path.length() ? "" : path.substring(0, path.length()
				- packageName.length() - 1);
		try {
			collector = new ClassInfoCollector(workingDir + "/" + userDefinedTypeSymbol.getSourceFile() + "$"
					+ userDefinedTypeSymbol.getId());
		} catch (IOException e) {
			e.printStackTrace();
		}
		/* Los getters y setters son los unicos metodos publicos no estaticos */
		for (String method : collector.getNonStaticMethods()) {
			try {
				symbolTable.addFunction(new FunctionSymbol(method, userDefinedTypeSymbol.getSourceFile() + "$"
						+ userDefinedTypeSymbol.getId(), true));
			} catch (DuplicatedSymbolException e) {
				// Nunca debe pasar
				e.printStackTrace();
			}
		}
	}

	/**
	 * Crea una clase inner para los valores de retorno de un funcion o para un
	 * tipo definido por el usuario.
	 * 
	 * @param symbol Funcion para que se quiere crear la clase inner.
	 */
	private void createInnerClass(Symbol symbol) {

		BytecodeGenerator byteCodeGenerator = null;
		if ((symbol instanceof FunctionSymbol)) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			// anteponerle algun caracter
			byteCodeGenerator = new BytecodeGenerator(path, className + "$" + functionSymbol.getId(), packageName,
					symbolTable, functionSymbol);

		} else if (symbol instanceof UserDefinedTypeSymbol) {
			UserDefinedTypeSymbol userDefinedTypeSymbol = (UserDefinedTypeSymbol) symbol;
			// anteponerle algun caract
			byteCodeGenerator = new BytecodeGenerator(path, className + "$" + userDefinedTypeSymbol.getId(),
					packageName, symbolTable, userDefinedTypeSymbol);
		}

		/* Generar el ".j" y el ".class" */
		byteCodeGenerator.generateJasmin(false);
		byteCodeGenerator.generateBytecodes();
	}

	/**
	 * Dada una instruccion que tiene una etiqueta que define el comienzo de una
	 * funcion, genera el codigo necesario para indicar que esta comenzando una
	 * funcion y resetea el RegisterSymbolTable.
	 * 
	 * @param i Instrucccion que representa el comienzo de la funcion.
	 */
	private void defineSubroutine(Instruction i) {
		/* Limpiar el RegisterSymbolTable */
		vars = new RegistersSymbolTable();

		/* Obtener la funcion (o constraint) que define la etiqueta */
		functionSymbol = symbolTable.getFunctionSymbol(i.getLabel(), true);
		if (functionSymbol == null) {
			functionSymbol = symbolTable.getFunctionSymbol(i.getLabel(), false);
		}

		functionSymbolTable = functionSymbol.getSymbolTable();

		currentFunctionName = i.getLabel();
		/* Generar el prototipo de la funcion */
		if (i.getLabel().equals("start")) {
			addGeneralInstruction(".method public static main([Ljava/lang/String;)V");
			vars
					.addVariableSymbol(new VariableSymbol("_args", new ArrayTypeSymbol(new StringTypeSymbol()), null,
							true));
		} else if (functionSymbol.isFunction()) {
			addGeneralInstruction(".method public static " + i.getLabel()
					+ functionSymbol.jasminType(packageName, className));
		} else {
			addGeneralInstruction(".method public static constraint__" + i.getLabel()
					+ functionSymbol.jasminType(packageName, className));
		}

		generateLocal(generalInstructions);
		/*
		 * Poner en el RegisterSymbolTable los parametros de entrada, salida y
		 * las variables locales de la funcion
		 */
		for (VariableSymbol variableSymbol : functionSymbol.getInputParams()) {
			VariableSymbol newVariableSymbol = new VariableSymbol("#" + variableSymbol.getId(), variableSymbol
					.getType(), variableSymbol.getConstraints(), false);
			vars.addVariableSymbol(newVariableSymbol);
		}

		for (VariableSymbol variableSymbol : functionSymbolTable.getVariableSymbols()) {
			if (variableSymbol.isLocalVariable()) {
				VariableSymbol newVariableSymbol = new VariableSymbol((variableSymbol.getId().charAt(0) != '$' ? "#"
						: "")
						+ variableSymbol.getId(), variableSymbol.getType(), variableSymbol.getConstraints(), false);
				vars.addVariableSymbol(newVariableSymbol);
			}
		}

		for (VariableSymbol variableSymbol : functionSymbol.getOutputParams()) {
			VariableSymbol newVariableSymbol = new VariableSymbol("#" + variableSymbol.getId(), variableSymbol
					.getType(), variableSymbol.getConstraints(), false);
			vars.addVariableSymbol(newVariableSymbol);
		}
	}

	/**
	 * Genera el codigo para inicializar una variable segun su tip
	 */
	private void initialize(Variable variableOutputParam) {
		/* Cargar el rvalue */
		if (variableOutputParam.getType() instanceof IntTypeSymbol
				|| variableOutputParam.getType() instanceof BoolTypeSymbol) {
			addGeneralInstruction("ldc 0");
		} else if (variableOutputParam.getType() instanceof DoubleTypeSymbol) {
			addGeneralInstruction("ldc2_w 0.0");
		} else if (variableOutputParam.getType() instanceof ArrayTypeSymbol) {
			newArray((ArrayTypeSymbol) variableOutputParam.getType());
		} else if (variableOutputParam.getType() instanceof UserDefinedTypeSymbol) {
			newObject(variableOutputParam.getType().jasminType(packageName, className));
		} else {
			// Tiene que ser un String
			newObject("java/lang/String");
		}

		/* Guardarlo en el registro de la variable */
		storeVariable(variableOutputParam.getId(), variableOutputParam.getType());
	}

	/**
	 * Genera el codigo pertinente ante una instruccion ADD de codigo
	 * intermedio.
	 * 
	 * @param i Instruction actual.
	 */
	private void handleAddInstruction(Instruction i) {

		/* Obtener VariableSymbols con los operandos de la suma */
		VariableSymbol op1 = getVariableSymbol(i.getOp2().charAt(0) == '*' ? i.getOp2().substring(1) : i.getOp2());
		VariableSymbol op2 = getVariableSymbol(i.getOp3().charAt(0) == '*' ? i.getOp3().substring(1) : i.getOp3());

		if (op1.getType() instanceof UserDefinedTypeSymbol && op2.getType() instanceof IntTypeSymbol) {
			/* Acceso a un campo de un tipo definido por el usuario */
			defineUserDefinedTypeField(i, (UserDefinedTypeSymbol) op1.getType());
		} else {
			/* Cargo los operandos */
			TypeSymbol operationType = (loadRvalue(i, i.getOp2())).getType();
			loadRvalue(i, i.getOp3());
			handleMathOperation("add", i, op1);
		}
	}

	/**
	 * 
	 * @param typeSymbol
	 */
	private void defineUserDefinedTypeField(Instruction i, UserDefinedTypeSymbol userTypeSymbol) {
		/* Obtener el numero de campo del tipo definido por el usuario */
		int fieldPosition = -1;
		try {
			fieldPosition = Integer.parseInt(instructions.get(currentInstructionNumber - 2).getOp3());
		} catch (IllegalArgumentException e) {
			fieldPosition = Integer.parseInt(instructions.get(currentInstructionNumber - 2).getOp2());
		}

		/* Agregar al RegisterSymbolTable el nuevo UserDefinedTypeSymbolField */
		VariableSymbol fieldVariableSymbol = userTypeSymbol.getComponents().get(fieldPosition);
		UserDefinedTypeSymbolField fieldTypeSymbol = new UserDefinedTypeSymbolField(
				((UserDefinedTypeSymbol) userTypeSymbol).getSourceFile() + "$" + userTypeSymbol.getId(), i.getOp1(),
				fieldVariableSymbol);

		vars.addVariableSymbol(new VariableSymbol(i.getOp1(), fieldTypeSymbol, fieldTypeSymbol.getField()
				.getConstraints(), false));

		/* Copiar la instancia */
		handleAssignInstruction(new Instruction("", InstructionType.ASSIGN, i.getOp1(), i.getOp2(), null));
	}

	/**
	 * Genera el codigo jasmin ante una instruccion ASSIGN de codigo intermedio.
	 * 
	 * @param i Instruction actual.
	 */
	private void handleAssignInstruction(Instruction i) {
		VariableSymbol rvalue = loadRvalue(i, i.getOp2());
		storeInLvalue(i.getOp1(), rvalue);
	}

	/**
	 * Gaurda lo que hay en el tope del stack en el registro asociado al valor
	 * izquierdo.
	 * 
	 * @param lvalue String del valor izquierdo, donde se quiere guardar lo que
	 *            esta en el tope del stack.
	 * @param rvalue VariableSymbol asociado al valor derecho (lo que esta en el
	 *            tope del stack).
	 */
	private void storeInLvalue(String lvalue, VariableSymbol rvalue) {
		/*
		 * Si es una desreferencia de codigo intermedio es una asignacion a una
		 * componente de un arreglo o un tipo definido por el usuario
		 */
		if (lvalue.charAt(0) == '*') {
			VariableSymbol lvalueVariableSymbol = getVariableSymbol(lvalue.substring(1));
			if (lvalueVariableSymbol.getType() instanceof UserDefinedTypeSymbolField) {
				/* Acceso a una componente de una estructura */
				UserDefinedTypeSymbolField userField = (UserDefinedTypeSymbolField) lvalueVariableSymbol.getType();
				setUserDefinedTypeField(userField, lvalue);
			} else {
				/* Acceso a una componente de un arreglo */
				ArrayTypeSymbolComponent component = (ArrayTypeSymbolComponent) lvalueVariableSymbol.getType();
				setArrayComponent(component, lvalue);
			}
		} else {
			/* Asignacion a un escalar u objeto (a objeto) */
			if (isLocalVariable(lvalue) && state != BytecodeGeneratorState.INIT) {
				storeVariable(lvalue, rvalue.getType());
			} else {
				/*
				 * Generar field para la variable temporaria (argumento de un
				 * constraint)
				 */
				if (isLocalVariable(lvalue) && state == BytecodeGeneratorState.INIT) {
					VariableSymbol newVariableSymbol = new VariableSymbol(lvalue, rvalue.getType(),
							new ArrayList<FunctionSymbol>(), false);

					defineStaticField(BytecodeGenerator.magic(packageName + className), newVariableSymbol);
					vars.addVariableSymbol(newVariableSymbol);
					invokeStatic(symbolTable.getFunctionSymbol("_set" + lvalue, true), generalInstructions);
				} else {
					/* El setter invoca los constraints */
					VariableSymbol lvalueVariableSymbol = getVariableSymbol(lvalue);
					invokeStatic(symbolTable.getFunctionSymbol("_set" + lvalue.substring(1), true), generalInstructions);
					skipConstraintCallInstructions(lvalueVariableSymbol);
				}
			}
		}
	}

	/**
	 * co * Carga en el stack el el registro asociado al operando.
	 * 
	 * @param i Instruction actual.
	 * @param operand String con el valor del operando que se quiere levantar
	 *            (op2 u op3).
	 * @return VariableSymbol del dato que quedo en el tope del stack.
	 */
	// me parece que no tiene sentido que devuelva un VariableSymbol con
	// un TypeSymbol deberia bastar
	private VariableSymbol loadRvalue(Instruction i, String operand) {
		/*
		 * Si comienza con "*" es la desreferencia de una componente de un
		 * arreglo o el acceso a un tipo definido por el usuario
		 */
		if (operand.charAt(0) == '*') {
			VariableSymbol lvalue = getVariableSymbol(operand.substring(1));
			if (lvalue.getType() instanceof UserDefinedTypeSymbolField) {
				/* Acceso a una componente de una estructura */
				UserDefinedTypeSymbolField userField = (UserDefinedTypeSymbolField) lvalue.getType();

				/* Apilar el receptor */
				loadOperand(userField.getReceptor());
				getUserDefinedTypeField(userField);
				return userField.getField();
			} else if (lvalue.getType() instanceof ArrayTypeSymbolComponent) {
				TypeSymbol type = getArrayComponent((ArrayTypeSymbolComponent) lvalue.getType(), operand.substring(1));
				ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) getVariableSymbol(
						((ArrayTypeSymbolComponent) lvalue.getType()).getAlias()).getType();
				return new VariableSymbol("TODO", type, null, false);
			} else {
				throw new RuntimeException("Wrong types.");
			}
		} else {
			/* Se quiere apilar un escalar */
			return loadOperand(operand);
		}
	}

	/**
	 * Saltea las instrucciones que corresponden a la invocacion de los
	 * constraints del VariableSymbol que recibe.
	 * 
	 * @param variableSymbol VariableSymbol del que se quiere saltear sus
	 *            constraints.
	 */
	private void skipConstraintCallInstructions(VariableSymbol variableSymbol) {
		for (int i = 0; i < variableSymbol.getConstraints().size(); i++) {
			while (instructions.get(currentInstructionNumber).getInstructionType() != InstructionType.CALL) {
				currentInstructionNumber++;
			}
			currentInstructionNumber++;
		}
	}

	/**
	 * Genera el codigo necesario para manejar
	 * 
	 * @param i
	 */
	private void handleCallInstruction(Instruction i) {

		/* Si la funcion a invocar es un casteo de gluck */
		if (i.getOp1().equals("_array_alloc")) {
			arrayAlloc(i);
		} else if (i.getOp1().equals("_array_get")) {
			arrayGet(i);
		} else if (i.getOp1().equals("_array_size")) {
			arraySize(i);
		} else if (i.isCastCall()) {
			castCall(i);
		} else if (i.isDoubleMathOperationCall()) {
			doubleMathOperationCall(i);
		} else {
			FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol(i.getOp1(), true);
			if (functionSymbol != null) {
				if (functionSymbol.isNative()) {
					nativeFunctionCall(functionSymbol);
				} else {
					invokeStatic(functionSymbol, generalInstructions);
					/* Copiar valores de retorno de la funcion */
					if (functionSymbol != null && functionSymbol.getOutputParams().size() > 0) {
						storeVariable("_ret", null);
						String objRet = functionSymbol.getSourceFile() + "$" + functionSymbol.getId();
						int len = functionSymbol.getOutputParams().size();
						for (int j = 0; j < functionSymbol.getOutputParams().size(); j++) {
							Instruction currentInstruction = instructions.get(currentInstructionNumber - 2
									- (len - 1 - j));
							loadOperand("_ret");
							getField(objRet, functionSymbol.getOutputParams().get(j), generalInstructions);
							storeVariable(currentInstruction.getOp1().substring(1), functionSymbol.getOutputParams()
									.get(j).getType() instanceof BoolTypeSymbol ? IntTypeSymbol.getInstance()
									: functionSymbol.getOutputParams().get(j).getType());
						}
					}
				}
			} else {
				/* Invocacion a un constraint */
				functionSymbol = symbolTable.getFunctionSymbol(i.getOp1(), false);
				invokeStatic(functionSymbol, generalInstructions);
			}
		}
	}

	private void nativeFunctionCall(FunctionSymbol functionSymbol) {
		invokeStatic(functionSymbol, generalInstructions);

		if (functionSymbol.hasOutputParams()) {
			/* Buscar el parametro de salida */
			String outputParam = instructions.get(currentInstructionNumber - 2).getOp1().substring(1);
			storeVariable(outputParam, functionSymbol.getOutputParams().get(0).getType());
		}
	}

	/**
	 * Genera el codigo para invocar a _array_size
	 * 
	 * @param i Instruction actual
	 */
	private void arraySize(Instruction i) {
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol(i.getOp1(), true);
		invokeStatic(functionSymbol, generalInstructions);

		/* Copiar valor de retorno */
		String varname = instructions.get(currentInstructionNumber - 2).getOp1().substring(1);
		storeInLvalue(varname, new VariableSymbol(varname, IntTypeSymbol.getInstance(), null, false));
	}

	private void arrayGet(Instruction i) {
		/* Acceso a una componente de un arreglo */
		String arrayReferenceName = instructions.get(currentInstructionNumber - 4).getOp1();
		VariableSymbol arrayRefecence = getVariableSymbol(arrayReferenceName.charAt(0) == '*' ? arrayReferenceName
				.substring(1) : arrayReferenceName);
		ArrayTypeSymbolComponent component = null;
		String indexAddr = instructions.get(currentInstructionNumber - 3).getOp1();
		if (arrayRefecence.getType() instanceof ArrayTypeSymbol) {
			component = new ArrayTypeSymbolComponent(arrayReferenceName);
		} else if (arrayRefecence.getType() instanceof UserDefinedTypeSymbolField) {
			component = new ArrayTypeSymbolComponent(((UserDefinedTypeSymbolField) arrayRefecence.getType()).getField()
					.getId(), arrayReferenceName);
		} else {
			component = new ArrayTypeSymbolComponent(((ArrayTypeSymbolComponent) arrayRefecence.getType()));
		}
		component.addIndexAddr(indexAddr);

		/* Donde se debe dejar el resultado */
		String resultVarname = instructions.get(currentInstructionNumber - 2).getOp1().substring(1);

		/* Desapilar parametros */
		addGeneralInstruction("pop");
		addGeneralInstruction("pop");
		addGeneralInstruction("pop");

		/* Almacenar la nueva variable */
		vars.addVariableSymbol(new VariableSymbol(resultVarname, component, new ArrayList<FunctionSymbol>(), false));
	}

	private void arrayAlloc(Instruction i) {
		/* Desapilar el tamaño del arreglo de stack */
		addGeneralInstruction("pop");

		String varname = instructions.get(currentInstructionNumber - 3).getOp1().substring(1);

		/*
		 * Buscar la linea en que se asigna el arreglo que se va a crear a una
		 * variable para saber de que tipo es el arreglo y ademas despues poder
		 * saltear las lineas intermedias (que en caso de ser una matriz generan
		 * los subarreglos)
		 */
		int counter = currentInstructionNumber;
		while (instructions.get(counter).getInstructionType() != InstructionType.ASSIGN
				|| (!instructions.get(counter).getOp2().equals("&" + varname) && !instructions.get(counter).getOp2()
						.equals(varname))) {
			counter++;
		}

		VariableSymbol variableSymbol = getVariableSymbol(instructions.get(counter).getOp1().charAt(0) == '*' ? instructions
				.get(counter).getOp1().substring(1)
				: instructions.get(counter).getOp1());

		if (variableSymbol.getType() instanceof ArrayTypeSymbol) {

			/*
			 * Es una variable temporaria aun no definida en la que se quiere
			 * guardar un arreglo
			 */
			ArrayTypeSymbol typeSymbol = (ArrayTypeSymbol) variableSymbol.getType();
			newArray(typeSymbol);

			/* Asignar el objeto al parametro del array_alloc */
			varname = instructions.get(currentInstructionNumber - 3).getOp1().substring(1);
			storeVariable(varname, typeSymbol);

			/* Saltear las lienas intermedias (por si era una matriz) */
			currentInstructionNumber = counter;

			/* Asignar el arreglo a la variable donde fue definida */
			VariableSymbol rvalue = loadOperand(instructions.get(currentInstructionNumber).getOp2().charAt(0) == '&' ? instructions
					.get(currentInstructionNumber).getOp2().substring(1)
					: instructions.get(currentInstructionNumber).getOp2());
			if (isLocalVariable(instructions.get(currentInstructionNumber).getOp1())) {
				storeInLvalue(instructions.get(currentInstructionNumber).getOp1(), rvalue);
			} else {
				putStatic(variableSymbol.getSourceFile(), variableSymbol, generalInstructions);
			}
			currentInstructionNumber++;
		} else if (variableSymbol.getType() instanceof UserDefinedTypeSymbol) {
			UserDefinedTypeSymbol typeSymbol = (UserDefinedTypeSymbol) variableSymbol.getType();
			newObject(typeSymbol.getSourceFile() + "$" + typeSymbol.getId());
			vars.addVariableSymbol(new VariableSymbol(varname, typeSymbol, null, false));

			/* Asignar el objeto al parametro del array_alloc */
			varname = instructions.get(currentInstructionNumber - 3).getOp1().substring(1);
			storeVariable(varname, typeSymbol);
		} else if (variableSymbol.getType() instanceof UserDefinedTypeSymbolField) {
			storeInLvalue(variableSymbol.getId(), variableSymbol);
			currentInstructionNumber = counter + 1;
		}

	}

	private void handleDivInstruction(Instruction i) {
		VariableSymbol op1 = loadRvalue(i, i.getOp2());
		VariableSymbol op2 = loadRvalue(i, i.getOp3());

		handleMathOperation("div", i, op1);
	}

	private void handleGotoInstruction(Instruction i) {
		addGeneralInstruction("goto " + i.getOp1());
	}

	private void handleIfeqInstruction(Instruction i) {
		handleIfOperation("eq", i);
	}

	private void handleIfgtInstruction(Instruction i) {
		handleIfOperation("gt", i);
	}

	private void handleIfgteInstruction(Instruction i) {
		handleIfOperation("ge", i);
	}

	private void handleIfltInstruction(Instruction i) {
		handleIfOperation("lt", i);
	}

	private void handleIflteInstruction(Instruction i) {
		handleIfOperation("le", i);
	}

	private void handleIfneqInstruction(Instruction i) {
		handleIfOperation("ne", i);
	}

	private void handleModInstruction(Instruction i) {
		VariableSymbol op1 = loadRvalue(i, i.getOp2());
		VariableSymbol op2 = loadRvalue(i, i.getOp3());

		handleMathOperation("rem", i, op1);
	}

	private void handleMultInstruction(Instruction i) {
		VariableSymbol op1 = loadRvalue(i, i.getOp2());
		VariableSymbol op2 = loadRvalue(i, i.getOp3());

		handleMathOperation("mul", i, op1);
	}

	private void handleParamInstruction(Instruction i) {
		/* Si es una constante la cargo y ya */
		if (!isVariable(i.getOp1())) {
			loadOperand(i.getOp1());
		} else {
			/* No hacer nada para los parametros de salida */
			if (i.getOp1().charAt(0) == '&' && !i.getOp1().matches("&#_const.*")) {
				return;
			}
			VariableSymbol operand = getVariableSymbol(i.getOp1().charAt(0) == '*' ? i.getOp1().substring(1) : i
					.getOp1());
			if (operand.getType() instanceof UserDefinedTypeSymbolField) {
				getUserDefinedTypeField((UserDefinedTypeSymbolField) operand.getType(), operand.getId());
			} else if (operand.getType() instanceof ArrayTypeSymbolComponent) {
				getArrayComponent((ArrayTypeSymbolComponent) operand.getType(), i.getOp1());
			} else {
				loadOperand(i.getOp1());
			}
		}
	}

	private void handleRetInstruction(Instruction i) {
		if (functionSymbol.getOutputParams().size() > 0) {
			/* Crear el objeto que se debe retornar */
			String newObject = packageName + className + "$" + functionSymbol.getId();
			newObject(newObject);
			storeVariable("_ret", null);
			for (VariableSymbol v : functionSymbol.getOutputParams()) {
				loadOperand("_ret");
				loadOperand("#" + v.getId());
				putField(newObject, v, generalInstructions);
			}
			loadOperand("_ret");
			addGeneralInstruction("areturn");
		} else {
			addGeneralInstruction("return");
		}
		functionSymbolTable = null;
		addGeneralInstruction(".end method");
	}

	private void handleSubInstruction(Instruction i) {
		VariableSymbol op1 = loadRvalue(i, i.getOp2());
		VariableSymbol op2 = loadRvalue(i, i.getOp3());

		handleMathOperation("sub", i, op1);
	}

	/**
	 * Retorna la instancia Variable guardada en la tabla de simbolos.
	 * 
	 * @param id Identificador que se quiere buscar
	 * @return Variable asociado al identificador o null en caso de no
	 *         encontrarlo.
	 */
	private Variable lookupVariable(String id) {
		Variable v = null;
		if ((v = vars.getVariable(id)) == null) {
			/* Tiene que ser una variable global */
			for (VariableSymbol vs : symbolTable.getVariableSymbols()) {
				if (vs.getId().equals(id)) {
					v = new Variable(vs);
				}
			}
		}

		return v;
	}

	/**
	 * Genera el codigo para cargar una variable en el stack.
	 * 
	 * @param id Identificador de la variable que se quiere cargar.
	 * @return
	 */
	private VariableSymbol loadOperand(String id) {
		Variable v = null;

		if (isVariable(id)) {
			if (isLocalVariable(id) && (state != BytecodeGeneratorState.INIT) || id.matches(".*_swap_var.*")) {

				v = lookupVariable(id);

				if (id.matches("#_string.*")) {
					addGeneralInstruction("ldc " + ((StringTypeSymbol) v.getType()).getValue());
				} else {
					loadVariable(v);
				}
				return v.getVariable();
			} else {
				/* El setter invoca los constraints */
				invokeStatic(
						symbolTable.getFunctionSymbol("_get" + (id.charAt(0) == '#' ? id.substring(1) : id), true),
						generalInstructions);

				return getVariableSymbol(id);
			}
		} else {
			TypeSymbol type = getConstantType(id);
			if (type instanceof StringTypeSymbol) {
				addGeneralInstruction("ldc " + id);
			} else if (type instanceof DoubleTypeSymbol) {
				addGeneralInstruction("ldc2_w " + id.substring(8).replaceAll("_", "."));
			} else {
				addGeneralInstruction("ldc " + id);
			}
			return new VariableSymbol(null, type, new ArrayList<FunctionSymbol>(), false, null);
		}
	}

	/**
	 * Genera el codigo para carga la variable variable en el stack.
	 * 
	 * @param variable Variable que se quiere cargar en el stack.
	 */
	private void loadVariable(Variable variable) {
		String preffix = getOperationPreffix(variable.getType());
		if (variable.getPosition() <= 3) {
			addGeneralInstruction(preffix + "load_" + variable.getPosition());
		} else {
			addGeneralInstruction(preffix + "load " + variable.getPosition());
		}
	}

	/**
	 * Retorna el prefijo asociado al tipo de la constante
	 * 
	 * @param constant
	 * @return
	 */
	private TypeSymbol getConstantType(String constant) {
		if (constant.charAt(0) == '"') {
			return new StringTypeSymbol();
		} else if (constant.charAt(0) == '&') {
			return DoubleTypeSymbol.getInstance();
		} else {
			return IntTypeSymbol.getInstance();
		}
	}

	/**
	 * Genera el codigo para guardar lo que esta en el tope del stack.
	 * 
	 * @param id Identificador donde se quiere guardar la variable.
	 * @param type Tipo de la variable donde se quiere guardar el identificador.
	 */
	private void storeVariable(String id, TypeSymbol type) {
		Variable v = lookupVariable(id);
		if (v == null) {
			vars.addVariableSymbol(new VariableSymbol(id, type, null, false));
			v = vars.getVariable(id);
		}

		/* Gaurdar en la tabla de simbolos los getters y setters */
		if (!id.matches(".*_swap_var.*") && isLocalVariable(id) && state == BytecodeGeneratorState.INIT) {
			String objectPath;
			if (path.equals(".")) {
				objectPath = className;
			} else {
				objectPath = path + "/" + className;
			}
			defineStaticField(objectPath, new VariableSymbol(id, type, new ArrayList<FunctionSymbol>(), true));

			invokeStatic(symbolTable.getFunctionSymbol("_set" + id, true), generalInstructions);
		} else {

			/* Asignacion de null */
			String preffix = null;
			if (v.getType() instanceof UserDefinedTypeSymbol && type instanceof IntTypeSymbol) {
				addGeneralInstruction("pop");
				addGeneralInstruction("aconst_null");
				preffix = "a";
			} else {
				preffix = getOperationPreffix(type);
			}

			if (v.getPosition() <= 3) {
				addGeneralInstruction(preffix + "store_" + v.getPosition());
			} else {
				addGeneralInstruction(preffix + "store " + v.getPosition());
			}
		}
	}

	/**
	 * Indica si un identificador es o no una variable.
	 * 
	 * @param id
	 * @return
	 */
	private boolean isVariable(String id) {
		try {
			Integer.parseInt(id);
			return false;
		} catch (NumberFormatException e) {
			if (id.charAt(0) == '"') {
				return false;
			} else if (id.matches("#_const[0-9].*") || id.matches("&#_const[0-9].*")) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Genera el codigo para una operacion matematica.
	 * 
	 * @param mathOperation String con el codigo de jasmin asociado a la
	 *            operacion que se desea realizar.
	 * @param i Instruccion con la operacion matematica
	 * @param operationType TypeSymbol con el tipo de los datos de la operacion
	 *            matematica
	 */
	private void handleMathOperation(String mathOperation, Instruction i, VariableSymbol rvalue) {

		/* Obtener el tipo de la operacion matematica */
		TypeSymbol opType = null;
		if (rvalue.getType() instanceof ArrayTypeSymbolComponent) {
			ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) getVariableSymbol(
					((ArrayTypeSymbolComponent) rvalue.getType()).getAlias()).getType();
			opType = arrayTypeSymbol.getIthLevelType(((ArrayTypeSymbolComponent) rvalue.getType()).getIndexesAddrs()
					.size());
			rvalue.setType(opType);
		} else if (rvalue.getType() instanceof UserDefinedTypeSymbolField) {
			opType = ((UserDefinedTypeSymbolField) rvalue.getType()).getField().getType();
			rvalue.setType(opType);
		} else {
			opType = rvalue.getType();
		}
		addGeneralInstruction(getOperationPreffix(opType) + mathOperation);
		storeInLvalue(i.getOp1(), rvalue);
	}

	private void handleIfOperation(String conditionalType, Instruction i) {
		VariableSymbol op1 = loadRvalue(i, i.getOp1());
		VariableSymbol op2 = loadRvalue(i, i.getOp2());

		/* Ambos operandos deberian ser del msimo tipo ya */
		TypeSymbol operationTypeSymbol = null;
		if (op1.getType() instanceof ArrayTypeSymbolComponent) {
			ArrayTypeSymbol varType = (ArrayTypeSymbol) getVariableSymbol(
					((ArrayTypeSymbolComponent) op1.getType()).getAlias()).getType();
			operationTypeSymbol = varType.getIthLevelType(((ArrayTypeSymbolComponent) op1.getType()).getIndexesAddrs()
					.size());
		} else if (op1.getType() instanceof UserDefinedTypeSymbolField) {
			operationTypeSymbol = ((UserDefinedTypeSymbolField) op1.getType()).getField().getType();
		} else {
			operationTypeSymbol = op1.getType();
		}

		if (operationTypeSymbol instanceof IntTypeSymbol || operationTypeSymbol instanceof BoolTypeSymbol) {
			addGeneralInstruction("if_icmp" + conditionalType + " " + i.getOp3());
		} else if (operationTypeSymbol instanceof DoubleTypeSymbol) {
			addGeneralInstruction("dcmpl");
			addGeneralInstruction("if" + conditionalType + " " + i.getOp3());
		} else if (operationTypeSymbol instanceof StringTypeSymbol) {
			addGeneralInstruction("invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z");
			addGeneralInstruction("if" + (conditionalType.equals("eq") ? "ne" : "eq") + " " + i.getOp3());
		}

	}

	/**
	 * Obtiene un VariableSymbol de la tabla de simbolos de la funcion que se
	 * esta procesando o de la tabla de simbolos global.
	 * 
	 * @param id Identificador que se quiere obtener.
	 * @return VariableSymbol asociado al identificador.
	 */
	private VariableSymbol getVariableSymbol(String id) {
		VariableSymbol ret = vars.getVariable(id);
		if (ret != null) {
			return ret;
		}
		if (functionSymbolTable != null) {
			ret = functionSymbolTable.getVariableSymbol(id.substring(1));
			if (ret != null) {
				return ret;
			}
			/* Sacarle el "#" */
			ret = symbolTable.getVariableSymbol(id.substring(1));
			return ret;
		} else {
			/* Sacarle el "#" */
			return symbolTable.getVariableSymbol(id.substring(1));
		}
	}

	private void doubleMathOperationCall(Instruction i) {
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol(i.getOp1(), true);
		invokeStatic(functionSymbol, generalInstructions);
		/* Le saco el "&" */
		String varname = instructions.get(currentInstructionNumber - 2).getOp1().substring(1);
		Variable var = vars.getVariable(varname);
		if (var == null) {
			vars.addVariableSymbol(new VariableSymbol(varname, DoubleTypeSymbol.getInstance(), null, false));
			var = vars.getVariable(varname);
		}
		storeVariable(var.getId(), var.getType());
	}

	/**
	 * Devuelve el prefijo de tipo correspondiente al tipo que recibe.
	 * 
	 * @param type TypeSymbol para el que se quiere averiguar el prefijo de
	 *            jasmin.
	 * @return String asociado al TypeSymbol.
	 */
	public static String getOperationPreffix(TypeSymbol type) {
		// hace falta, o son siempre enteras?
		String preffix = null;
		if (type instanceof IntTypeSymbol || type instanceof BoolTypeSymbol) {
			preffix = "i";
		} else if (type instanceof DoubleTypeSymbol) {
			preffix = "d";
		} else {
			preffix = "a";
		}
		return preffix;
	}

	private void castCall(Instruction i) {
		FunctionSymbol func = symbolTable.getFunctionSymbol(i.getOp1(), true);
		invokeStatic(symbolTable.getFunctionSymbol(i.getOp1(), true), generalInstructions);
		/* Obtener el nombre de la variabla donde se deja el resultado */
		String varname = instructions.get(currentInstructionNumber - 2).getOp1().substring(1);

		/* Obtener el tipo de la variable donde se deja el resultado */
		TypeSymbol type = null;
		if (i.getOp1().matches(".*2int")) {
			type = IntTypeSymbol.getInstance();
		} else if (i.getOp1().matches(".*2string")) {
			type = new StringTypeSymbol();
		} else {
			type = DoubleTypeSymbol.getInstance();
		}

		if (state == BytecodeGeneratorState.NORMAL) {
			Variable var = lookupVariable(varname);
			if (var == null) {
				/* Propagar los constraints */
				vars.addVariableSymbol(new VariableSymbol(varname, type, null, false));
				var = vars.getVariable(varname);
			}
			storeVariable(varname, var.getType());
		} else {
			VariableSymbol newVariableSymbol = new VariableSymbol(varname, type, new ArrayList<FunctionSymbol>(), false);
			defineStaticField(packageName + className, newVariableSymbol);
			vars.addVariableSymbol(newVariableSymbol);
			putStatic(packageName + className, newVariableSymbol, generalInstructions);
		}

	}

	private boolean isLocalVariable(String id) {
		return id.charAt(0) == '$' || vars.getVariable(id) != null;
	}

	/**
	 * Genera el codigo para invocar al setter para establecer el campo indicado
	 * por el parametro de un tipo definido por el usuario. El valor que se
	 * quiere setear ya se encuentra apilado3 en el stack.
	 * 
	 * 
	 * @param userField UserDefinedTypeField que tiene la informacion acerca del
	 *            campo y el tipo definido por el usuario que se quiere settear.
	 * @param lvalue String con el nombre de la variable donde esta la instancia
	 *            del tipo definido por el usuario.
	 * 
	 */
	private void setUserDefinedTypeField(UserDefinedTypeSymbolField userField, String lvalue) {
		/* El receptor es el primer argumento! */
		storeVariable("_" + userField.getField().getType().toString() + "_swap_var", userField.getField().getType());
		loadOperand(lvalue.substring(1));
		VariableSymbol v = getVariableSymbol(lvalue.substring(1));

		addGeneralInstruction("checkcast " + userField.getObject());
		loadOperand("_" + userField.getField().getType().toString() + "_swap_var");

		/* Invocar el setter */
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol("_set" + userField.getField().getId(), true);
		invokeVirtual(functionSymbol);

		/* Saltear invocaciones a constraints */
		VariableSymbol variableSymbol = getVariableSymbol(lvalue.substring(1));
		skipConstraintCallInstructions(variableSymbol);

	}

	/**
	 * Genera el codigo para invocar al setter para setear una componente de un
	 * arreglo. El valor que se quiere setear ya se encuentra apilado en el
	 * stack.
	 * 
	 * 
	 * @param component ArrayTypeSymbolComponent que tiene la informacion acerca
	 *            del indice de la componente y el tipo del arreglo que se
	 *            quiere settear.
	 * @param lvalue String con el nombre de la variable donde esta la instancia
	 *            del tipo definido por el usuario.
	 * 
	 */
	private void setArrayComponent(ArrayTypeSymbolComponent component, String lvalue) {
		/* De ser necesario hacer auto-boxing */
		ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) getVariableSymbol(component.getAlias()).getType();
		TypeSymbol type = arrayTypeSymbol.getIthLevelType(component.getIndexesAddrs().size());
		if (type instanceof IntTypeSymbol) {
			invokeStatic(symbolTable.getFunctionSymbol("_int2Integer", true), generalInstructions);
		} else if (type instanceof DoubleTypeSymbol) {
			invokeStatic(symbolTable.getFunctionSymbol("_double2Double", true), generalInstructions);
		}

		VariableSymbol lvalueVariableSymbol = getVariableSymbol(component.getAlias());

		/* Invocar al setter */
		/* Saltear la siguiente instruccion ($temp = *array) */
		if (instructions.get(currentInstructionNumber).getInstructionType() == InstructionType.ASSIGN
				&& instructions.get(currentInstructionNumber).getOp2().equals(lvalue)) {
			currentInstructionNumber++;
		}
		if (isLocalVariable(component.getAlias())) {
			storeVariable("_swap_var", ObjectWrapperTypeSymbol.getInstance());
			loadOperand(component.getAlias());
			loadIndexVector(component.getIndexesAddrs());
			loadOperand("_swap_var");
			invokeStatic(symbolTable.getFunctionSymbol("_arraySet", true), generalInstructions);
		} else {
			/* El setter invoca los constraints */
			storeVariable("_swap_var", ObjectWrapperTypeSymbol.getInstance());
			loadIndexVector(component.getIndexesAddrs());
			loadOperand("_swap_var");
			invokeStatic(symbolTable.getFunctionSymbol("_set" + component.getAlias().substring(1), true),
					generalInstructions);
			skipConstraintCallInstructions(lvalueVariableSymbol);
		}
	}

	/**
	 * Dado un vector de strings que representan constantes enteras o addrs de
	 * variables, crea un vector de enteros con los valores de dichas variables.
	 * 
	 * @param indexesAddrs Vector de strigns con las constantes enteras o los
	 *            nombres de las variables enteras que se quieren poner en el
	 *            arreglo.
	 */
	private void loadIndexVector(List<String> indexesAddrs) {
		loadOperand(new Integer(indexesAddrs.size()).toString());
		addGeneralInstruction("newarray int");
		int i = 0;
		for (String index : indexesAddrs) {
			addGeneralInstruction("dup");
			loadOperand(new Integer(i).toString());
			dividePosition(index);
			addGeneralInstruction("iastore");
			i++;
		}
	}

	/**
	 * Dado un vector de string que tienen constantes enteras genera el codigo
	 * para construir un vector de enteros con dichas constantes.
	 * 
	 * @param dimentions Vector con las constantes enteras que se quieren poner
	 *            en el vector.
	 */
	private void loadDimentionsVector(List<String> dimentions) {
		loadOperand(new Integer(dimentions.size()).toString());
		addGeneralInstruction("newarray int");
		int i = 0;
		for (String index : dimentions) {
			addGeneralInstruction("dup");
			loadOperand(new Integer(i).toString());
			loadOperand(index);
			addGeneralInstruction("iastore");
			i++;
		}
	}

	/**
	 * Genera el codigo para obtener la componente de un arreglo. En caso de que
	 * el arreglo sea de Integer o Double devuelve el int o double build-in
	 * correspondiente.
	 * 
	 * @param component ArrayTypeSymbolComponent que tiene la informacion acerca
	 *            del indice y el arreglo de que se quiere obtener el element.
	 * @param lvalue String con el nombre de la variable donde esta la instancia
	 *            del arreglo.
	 */
	private TypeSymbol getArrayComponent(ArrayTypeSymbolComponent component, String lvalue) {
		/*
		 * Generar el codigo para obtener el campo simpre en cuanto, la
		 * componente que se esta obteniendo no sea argumento de otro array_get
		 */
		if (currentInstructionNumber + 2 >= instructions.size()
				|| instructions.get(currentInstructionNumber + 2).getInstructionType() != InstructionType.CALL
				|| !instructions.get(currentInstructionNumber + 2).getOp1().equals("_array_get")) {
			/* Apilar el receptor y el indice de la componente del arreglo */
			loadOperand(component.getAlias());
			loadIndexVector(component.getIndexesAddrs());
			ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) getVariableSymbol(component.getAlias()).getType();
			TypeSymbol componentsType = arrayTypeSymbol.getIthLevelType(component.getIndexesAddrs().size());

			/* Invocar al getter */
			invokeStatic(symbolTable.getFunctionSymbol("_arrayGet", true), generalInstructions);

			/* De ser necesario castear */
			if (componentsType instanceof IntTypeSymbol) {
				invokeStatic(symbolTable.getFunctionSymbol("_Integer2int", true), generalInstructions);
			} else if (componentsType instanceof DoubleTypeSymbol) {
				invokeStatic(symbolTable.getFunctionSymbol("_Double2double", true), generalInstructions);
			}
			return componentsType;
		} else {
			/*
			 * apilar cualquier cosa en la pila total el pop del arrayGet lo va
			 * a aeliminar
			 */
			addGeneralInstruction("ldc -1");
			return null;
		}
	}

	/**
	 * Genera el codigo para invocar al getter del campo indicado por el
	 * parametro de un tipo definido por el usuario.
	 * 
	 * @param userField UserDefinedTypeField que tiene la informacion acerca del
	 *            campo y el tipo definido por el usuario que se quiere settear.
	 * @param lvalue String con el nombre de la variable donde esta la instancia
	 *            del tipo definido por el usuario.
	 */
	private void getUserDefinedTypeField(UserDefinedTypeSymbolField userField, String lvalue) {
		loadOperand(lvalue);
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol("_get" + userField.getField().getId(), true);
		invokeVirtual(functionSymbol);
	}

	public void newArray(ArrayTypeSymbol arrayTypeSymbol) {
		/* Crear array de enteros con las dimensiones */
		loadDimentionsVector(arrayTypeSymbol.getDimentions());

		/* Apilar el Class del arreglo */
		TypeSymbol type = arrayTypeSymbol.getMostDeepComponentType();
		if (type instanceof IntTypeSymbol) {
			addGeneralInstruction("ldc \"java.lang.Integer\"");
		} else if (type instanceof DoubleTypeSymbol) {
			addGeneralInstruction("ldc \"java.lang.Double\"");
		} else if (type instanceof StringTypeSymbol) {
			addGeneralInstruction("ldc \"java.lang.String\"");
		} else {
			addGeneralInstruction("ldc \"" + arrayTypeSymbol.getComponentsType().jasminType(packageName, className)
					+ "\"");
		}

		/* Instanciar el arreglo */
		invokeStatic(symbolTable.getFunctionSymbol("_newArray", true), generalInstructions);
	}

	/**
	 * En caso de que el String recibido represente un build-in, retorna su
	 * wrapper correspondiente.
	 * 
	 * @param jasminType String con el tipo del dato
	 * @return Wrapper del tipo recibido
	 */
	private String getArrayType(String jasminType) {
		if (jasminType.equals("I")) {
			return "java/lang/Integer";
		} else if (jasminType.equals("D")) {
			return "java/lang/Double";
		} else {
			return jasminType.substring(1, jasminType.length() - 1);
		}
	}

	/**
	 * Genera codigo para dividir un numero multiplo de 4 por 4.
	 * 
	 * @param indexAddr Variable que se queire dividir por 4. No esta
	 *            previamente cargada en el stack.
	 */
	public void dividePosition(String indexAddr) {
		loadOperand(indexAddr);
		addGeneralInstruction("ldc 4");
		addGeneralInstruction("idiv");
	}

	public void newString(String s) {
		addGeneralInstruction("new java/lang/String");
		addGeneralInstruction("dup");
		addGeneralInstruction("ldc " + s);
		addGeneralInstruction("invokespecial java/lang/String/<init>(Ljava/lang/String;)V");
	}

	public void dump() {
		for (String s : headerInstructions) {
			file.println(s);
		}

		for (String s : fieldsInstructions) {
			file.println(s);
		}

		for (String s : methodsInstructions) {
			file.println(s);
		}

		for (String s : generalInstructions) {
			file.println(s);
		}
		file.close();
	}

	public void addHeaderInstruction(String s) {
		headerInstructions.add(s);
	}

	public void addFieldsInstruction(String s) {
		fieldsInstructions.add(s);
	}

	public void addMethodsInstruction(String s) {
		methodsInstructions.add(s);
	}

	public void addGeneralInstruction(String s) {
		generalInstructions.add(s);
	}

	/**
	 * Genera el codigo para invocar una funcion estatica.
	 * 
	 * @param functionSymbol Funcion o constraint que se desea invocar
	 */
	public void invokeStatic(FunctionSymbol functionSymbol, List<String> instructionsList) {
		instructionsList.add("invokestatic " + magic(functionSymbol.getSourceFile())
				+ (functionSymbol.isFunction() ? "" : "constraint__") + functionSymbol.getId()
				+ functionSymbol.jasminType(packageName, className));
	}

	/**
	 * Genera el codigo para invocar un constructor.
	 * 
	 * @param functionSymbol Funcion o constraint que se desea invocar.
	 */
	public void invokeSpecial(FunctionSymbol functionSymbol) {
		addGeneralInstruction("invokespecial " + functionSymbol.getSourceFile() + "/"
				+ (functionSymbol.isFunction() ? "" : "constraint__") + functionSymbol.getId()
				+ functionSymbol.jasminType(packageName, className));
	}

	/**
	 * Genera el codigo para invocar una funcion no estatica.
	 * 
	 * @param functionSymbol Funcion o constraint que se desea invocar.
	 */
	public void invokeVirtual(FunctionSymbol functionSymbol) {
		addGeneralInstruction("invokevirtual " + functionSymbol.getSourceFile() + "/"
				+ (functionSymbol.isFunction() ? "" : "constraint__") + functionSymbol.getId()
				+ functionSymbol.jasminType(packageName, className));
	}

	/**
	 * Obtiene un campo de un objeto.
	 * 
	 * @param object String con el path del receptor.
	 * @param variableSymbol VariableSymbol que representa el field que se
	 *            quiere recuperar.
	 */
	public void getField(String object, VariableSymbol variableSymbol, List<String> instructionsList) {
		if (object.contains("$")) {
			instructionsList.add("checkcast " + object);
		}
		instructionsList.add("getfield " + object + "/" + "_" + variableSymbol.getId() + " "
				+ variableSymbol.getType().jasminType(packageName, className));
	}

	/**
	 * Genera el codigo para obtener el atributo estatico representado por
	 * variableSymbol de la clase object
	 * 
	 * @param object String con el path de la clase de la que se quiere obtener
	 *            el miembro estatico.
	 * @param variableSymbol VariableSymbol que representa el campo que se
	 *            quiere obtener.
	 */
	public void getStaticField(String object, VariableSymbol variableSymbol, List<String> instructionsList) {
		instructionsList.add("getstatic " + object + "/" + "_" + variableSymbol.getId() + " "
				+ variableSymbol.getType().jasminType(packageName, className));
	}

	/**
	 * Setea un campo de un objeto.
	 * 
	 * @param object String con el path del receptor.
	 * @param variableSymbol VariableSymbol que representa el field que se
	 *            quiere setear.
	 */
	public void putField(String object, VariableSymbol variableSymbol, List<String> instructionsList) {
		instructionsList.add("putfield " + object + "/" + "_" + variableSymbol.getId() + " "
				+ variableSymbol.getType().jasminType(packageName, className));
	}

	/**
	 * Setea un campo estatico de un objeto.
	 * 
	 * @param object String con el path de la clase.
	 * @param variableSymbol VariableSymbol que representa el field que se
	 *            quiere setear.
	 */
	public void putStatic(String object, VariableSymbol variableSymbol, List<String> instructionsList) {
		instructionsList.add("putstatic " + object + "/" + "_" + variableSymbol.getId() + " "
				+ variableSymbol.getType().jasminType(packageName, className));
	}

	/**
	 * Define un campo no estatico con sus respectivos getters y setters.
	 * 
	 * @param String que representa el path del receptor.
	 * @param variableSymbol VariableSymbol que representa el field.
	 */
	public void defineNonStaticField(String object, VariableSymbol variableSymbol, boolean defineMethods) {
		// podrian ser campos privados..
		TypeSymbol fieldType = variableSymbol.getType();

		addFieldsInstruction(".field public " + "_" + variableSymbol.getId() + " "
				+ fieldType.jasminType(packageName, className.substring(0, className.lastIndexOf("$"))));

		if (defineMethods) {
			addMethodsInstruction(".method public _get" + variableSymbol.getId() + "()"
					+ fieldType.jasminType(packageName, className.substring(0, className.lastIndexOf("$"))));
			generateLocal(methodsInstructions);
			addMethodsInstruction("aload_0");
			if (variableSymbol.getType() instanceof UserDefinedTypeSymbolField) {
				String temp = variableSymbol.getType().jasminType(packageName, className);
				addGeneralInstruction("checkcast" + temp.substring(1, temp.length() - 1));
			}
			getField(packageName + className, variableSymbol, methodsInstructions);
			addMethodsInstruction(BytecodeGenerator.getOperationPreffix(fieldType) + "return");
			addMethodsInstruction(".end method");

			addMethodsInstruction(".method public _set" + variableSymbol.getId() + "("
					+ fieldType.jasminType(packageName, className.substring(0, className.lastIndexOf("$"))) + ")V");
			generateLocal(methodsInstructions);
			/* Setear el campo */
			addMethodsInstruction("aload_0");
			if (variableSymbol.getType() instanceof UserDefinedTypeSymbolField) {
				String temp = variableSymbol.getType().jasminType(packageName, className);
				addGeneralInstruction("checkcast" + temp.substring(1, temp.length() - 1));
			}

			addMethodsInstruction(BytecodeGenerator.getOperationPreffix(variableSymbol.getType()) + "load_1");
			putField(packageName + className, variableSymbol, methodsInstructions);

			/* Validar todos los constraints */
			for (FunctionSymbol constraint : variableSymbol.getConstraints()) {
				FunctionSymbol formalConstraint = symbolTable.getFunctionSymbol(constraint.getId(), false);
				addMethodsInstruction("aload_0");
				getField(packageName + className, variableSymbol, methodsInstructions);
				cast(fieldType, formalConstraint.getInputParams().get(0).getType(), symbolTable);
				// apilar los demas parametros
				invokeStatic(formalConstraint, methodsInstructions);
			}

			addMethodsInstruction("return");
			addMethodsInstruction(".end method");

			/* Agregar el getter y setter a la tabla de simbolos */
			try {
				FunctionSymbol functionSymbol = new FunctionSymbol("_get" + variableSymbol.getId() + "()"
						+ fieldType.jasminType(packageName, className.substring(0, className.lastIndexOf("$"))),
						packageName + className, true);
				functionSymbol.setIsGetterOrSetter(true);
				symbolTable.addFunction(functionSymbol);
				functionSymbol = new FunctionSymbol("_set" + variableSymbol.getId() + "("
						+ fieldType.jasminType(packageName, className.substring(0, className.lastIndexOf("$"))) + ")V",
						packageName + className, true);
				functionSymbol.setIsGetterOrSetter(true);
				symbolTable.addFunction(functionSymbol);
			} catch (DuplicatedSymbolException e) {
				// Nunca debe pasar
				e.printStackTrace();
			}
		}
	}

	/**
	 * Castea lo que esta en el tope de la pila (cuyo tipo es el primer
	 * parametro) al tipo del segundo parametro.
	 * 
	 * @param type
	 * @param type2
	 */
	private void cast(TypeSymbol type, TypeSymbol type2, SymbolTable symbolTable) {
		String castFunction = null;
		try {
			castFunction = type.castTo(type2);
		} catch (IncompatibleTypesException e) {
			// Nunca debe pasar
			e.printStackTrace();
		}
		if (castFunction != null) {
			invokeStatic(symbolTable.getFunctionSymbol(castFunction, true), generalInstructions);
		}
	}

	/**
	 * Define un campo estatico con sus respectivos getters y setters.
	 * 
	 * @param String que representa el path del receptor.
	 * @param variableSymbol VariableSymbol que representa el field.
	 */
	public void defineStaticField(String object, VariableSymbol variableSymbol) {
		object = BytecodeGenerator.magic(object);
		object = object.substring(0, object.length() - 1);

		TypeSymbol fieldType = variableSymbol.getType();

		// Aca se mapea a boolean si es necesario!
		addFieldsInstruction(".field static public " + "_" + variableSymbol.getId() + " "
				+ variableSymbol.getType().jasminType(packageName, className));

		addMethodsInstruction(".method public static _get" + variableSymbol.getId() + "()"
				+ fieldType.jasminType(packageName, className));
		generateLocal(methodsInstructions);
		getStaticField(object, variableSymbol, methodsInstructions);
		addMethodsInstruction(BytecodeGenerator.getOperationPreffix(variableSymbol.getType()) + "return");
		addMethodsInstruction(".end method");

		if (fieldType instanceof ArrayTypeSymbol && !isLocalVariable(variableSymbol.getId())) {
			addMethodsInstruction(".method public static _set" + variableSymbol.getId() + "([ILjava/lang/Object;)V");
			generateLocal(methodsInstructions);
			/* Invocar a Util._arraySet */
			getStaticField(object, variableSymbol, methodsInstructions);
			addMethodsInstruction("aload_0");
			addMethodsInstruction("aload_1");
			invokeStatic(symbolTable.getFunctionSymbol("_arraySet", true), methodsInstructions);

			/* Agregar el setter */
			try {
				FunctionSymbol functionSymbol = new FunctionSymbol("_set" + variableSymbol.getId()
						+ "([ILjava/lang/Object;)V", packageName + className, true);
				functionSymbol.setIsGetterOrSetter(true);
				symbolTable.addFunction(functionSymbol);
			} catch (DuplicatedSymbolException e) {
				// Nunca debe pasar
				e.printStackTrace();
			}
		} else {
			addMethodsInstruction(".method public static _set" + variableSymbol.getId() + "("
					+ fieldType.jasminType(packageName, className) + ")V");
			generateLocal(methodsInstructions);
			addMethodsInstruction(BytecodeGenerator.getOperationPreffix(fieldType) + "load_0");
			putStatic(object, variableSymbol, methodsInstructions);

			/* Agregar el setter */
			try {
				FunctionSymbol functionSymbol = new FunctionSymbol("_set" + variableSymbol.getId() + "("
						+ fieldType.jasminType(packageName, className) + ")V", packageName + className, true);
				functionSymbol.setIsGetterOrSetter(true);
				symbolTable.addFunction(functionSymbol);
			} catch (DuplicatedSymbolException e) {
				// Nunca debe pasar
				e.printStackTrace();
			}
		}

		/* Validar todos los constraints */
		for (FunctionSymbol constraint : variableSymbol.getConstraints()) {
			/* Obtener el constraint formal y el actual */
			FunctionSymbol formalConstraint = symbolTable.getFunctionSymbol(constraint.getId(), false);
			FunctionSymbol actualConstraint = variableSymbol.getConstraint(constraint.getId());

			/* Apilar el receptor */
			getStaticField(object, variableSymbol, methodsInstructions);
			cast(fieldType, formalConstraint.getInputParams().get(0).getType(), symbolTable);

			/* Apilar los parametros actuales del constraint */
			for (int j = 1; j < actualConstraint.getInputParams().size(); j++) {
				/* Los parametros ya fueron casteados */
				actualConstraint.getInputParams().get(j).setType(formalConstraint.getInputParams().get(j).getType());
				getStaticField(object, actualConstraint.getInputParams().get(j), methodsInstructions);
			}

			/* Invocar al constraint */
			invokeStatic(formalConstraint, methodsInstructions);
		}
		addMethodsInstruction("return");
		addMethodsInstruction(".end method");

		/* Agregar el getter a la tabla de simbolos */
		try {
			FunctionSymbol functionSymbol = new FunctionSymbol("_get" + variableSymbol.getId() + "()"
					+ fieldType.jasminType(packageName, className), packageName + className, true);
			functionSymbol.setIsGetterOrSetter(true);
			symbolTable.addFunction(functionSymbol);
		} catch (DuplicatedSymbolException e) {
			// Nunca debe pasar
			e.printStackTrace();
		}
	}

	private void generateLocal(List<String> instructionsList) {
		instructionsList.add(".limit stack " + STACK_SIZE);
		instructionsList.add(".limit locals " + LOCALS_SIZE);
	}

	/**
	 * Genera el codigo jasmin para el constructor default.
	 */
	public void defineDefaultConstructor() {
		addGeneralInstruction(".method public <init>()V");
		addGeneralInstruction("aload_0");
		addGeneralInstruction("invokenonvirtual java/lang/Object/<init>()V");
		addGeneralInstruction("return");
		addGeneralInstruction(".end method");
	}

	/**
	 * Genera el codigo para instanciar un nuevo objeto invocando al constructor
	 * default.
	 * 
	 * @param object Path del objeto que se desea instanciar.
	 */
	public void newObject(String object) {
		addGeneralInstruction("new " + object);
		addGeneralInstruction("dup");
		addGeneralInstruction("invokespecial " + object + "/<init>()V");
	}

	/**
	 * Genera el codigo para poner en el stack el valor de un campo de un tipo
	 * definido por el usuario. El receptor ya debe estar cargado en el stack.
	 * 
	 * @param userField UserDefinedTypeField del que se obtienen los datos.
	 */
	public void getUserDefinedTypeField(UserDefinedTypeSymbolField userField) {
		getField(userField.getObject(), userField.getField(), generalInstructions);
	}

	private class RegistersSymbolTable {
		private Hashtable<String, Variable> symbolTable;
		private int nextPosition;

		public RegistersSymbolTable() {
			symbolTable = new Hashtable<String, Variable>();
			nextPosition = 0;
		}

		public void addVariableSymbol(VariableSymbol variableSymbol) {
			Variable variable = new Variable(nextPosition, variableSymbol);
			symbolTable.put(variableSymbol.getId().charAt(0) == '&' ? variableSymbol.getId().substring(1)
					: variableSymbol.getId(), variable);

			nextPosition += variableSymbol.getType() instanceof DoubleTypeSymbol ? 2 : 1;
		}

		public Variable getVariable(String id) {
			return symbolTable.get(id);
		}
	}

	private class Variable extends VariableSymbol {
		private int position;

		public Variable(int position, VariableSymbol variableSymbol) {
			super(variableSymbol.getId(), variableSymbol.getType(), variableSymbol.getConstraints(), false, null);
			this.position = position;
		}

		public Variable(VariableSymbol variableSymbol) {
			this(-1, variableSymbol);
		}

		public int getPosition() {
			return position;
		}

		public Variable getVariable() {
			return this;
		}

		@Override
		public String toString() {
			return super.toString();
		}
	}
}