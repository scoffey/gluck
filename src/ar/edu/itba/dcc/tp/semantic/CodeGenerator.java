package ar.edu.itba.dcc.tp.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ar.edu.itba.dcc.tp.parser.ParserAdapter;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;
import ar.edu.itba.dcc.tp.semantic.nodes.Constraint;
import ar.edu.itba.dcc.tp.semantic.nodes.Expression;
import ar.edu.itba.dcc.tp.semantic.nodes.ForSentence;
import ar.edu.itba.dcc.tp.semantic.nodes.FunctionCall;
import ar.edu.itba.dcc.tp.semantic.nodes.LValue;
import ar.edu.itba.dcc.tp.semantic.nodes.Sentence;
import ar.edu.itba.dcc.tp.semantic.nodes.Type;
import ar.edu.itba.dcc.tp.semantic.symbols.ArrayTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.BoolTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.DoubleTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.IntTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.ListTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.NullTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.StringTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.Symbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UserDefinedTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol.VariableType;
import ar.edu.itba.dcc.tp.util.ProcessLogger;

public class CodeGenerator extends SemanticAnalyzer {

	public enum MathOperator {
		PLUS(InstructionType.ADD, "_add_real"), MINUS(InstructionType.SUB, "_sub_real"), TIMES(InstructionType.MULT,
				"_mult_real"), DIVIDE(InstructionType.DIV, "_div_real"), MODULE(InstructionType.MOD, null), UNARY_MINUS(
				InstructionType.SUB, "_uminus_real");

		private InstructionType type;
		private String castFunction;

		MathOperator(InstructionType type, String castFunction) {
			this.type = type;
			this.castFunction = castFunction;
		}

		public InstructionType getInstructionType() {
			return type;
		}

		public String getCastFunction() {
			return castFunction;
		}
	};

	public enum LoopType {
		WHILE, UNTIL
	};

	public enum Mode {
		STATIC_BLOCK, COMMON
	};

	/* Modo de operación del generador de código. */
	private Mode currentMode;

	/* Lista de instrucciones y número de la próxima. */
	private List<Instruction> instructions;
	private int nextInstruction = 0;

	/* Lo mismo pero para el bloque estático de inicialización. */
	private List<Instruction> blockInstructions;
	private int blockNextInstruction = 0;

	/* Etiqueta a colocar en la próxima instrucción. */
	private String nextLabel = null;

	/* Contadores para nombres de variables */
	private int stringCount = 0;
	private int labelCount = 0;
	private int tempVarCount = 1;

	public CodeGenerator(ParserAdapter parser, ProcessLogger logger) {
		super(parser, logger);
		this.parser = parser;
		this.currentMode = Mode.STATIC_BLOCK;
		instructions = new ArrayList<Instruction>();
		blockInstructions = new ArrayList<Instruction>();
	}

	/**
	 * @return La lista de instrucciones generadas.
	 */
	public List<Instruction> getInstructions() {
		if (blockInstructions.size() > 0) {
			blockInstructions.get(0).setLabel("_init");
		}
		blockInstructions.add(new Instruction("_init_end", InstructionType.ASSIGN, createTempVar(), "0", null));
		blockInstructions.addAll(instructions);
		return blockInstructions;
	}

	public Integer getNextInstruction() {
		return (currentMode == Mode.COMMON) ? nextInstruction : blockNextInstruction;
	}

	public Integer generateGoto() {
		int ret = getNextInstruction();
		appendInstruction(InstructionType.GOTO, null, null, null);
		return ret;
	}

	public void enterContext() {
		super.enterContext();
		currentMode = Mode.COMMON;
	}

	public void leaveContext() {
		super.leaveContext();
		if (getSymbolTable().isTopContext()) {
			currentMode = Mode.STATIC_BLOCK;
		}
	}

	public Sentence declareVariable(String id, Type type, Expression exp) {

		/* La primera pasada ya puso esta variable en la tabla. */
		VariableSymbol variable = symbolTable.getVariableSymbol(id);

		/* Solo pasa si hay errores sintacticos */
		if (variable == null) {
			return new Sentence();
		}

		TypeSymbol variableType = type.getType();

		/* Si es un arreglo, alocar recursivamente todas las dimensiones. */
		if (type.getArrayAddr().size() > 0) {
			appendAssign("#" + variable.getAddr(), recursiveArrayAlloc(new ArrayList<Expression>(type.getArrayAddr())));
		}

		/* En cade de que sea un arreglo, copiar el vector de dimensiones */
		if (type.getArrayAddr().size() > 0) {
			((ArrayTypeSymbol) variable.getType()).setDimentions(type.getArrayAddr());
		}

		/* Agregarle los constraints */
		variable.setConstraints(type.getConstraints());

		/* Ponerme como receptor en todos mis constraints */
		for (FunctionSymbol c : type.getConstraints()) {
			if (c.getInputParams().size() > 0) {
				c.getInputParams().set(0, variable);
			}
		}

		/* Inicializarla de ser necesario. */
		if (exp != null) {
			LValue lvalue = new LValue("#" + variable.getAddr(), variable.getType(), variable.getConstraints(), false);
			lvalue.setVariable(variable);
			return assignValue(lvalue, exp, true);
		}

		/*
		 * Si es del tipo del usuario, reservar el espacio y alocar
		 * recursivamente todas las matrices.
		 */

		if (type.getArrayAddr().size() == 0 && variableType instanceof UserDefinedTypeSymbol) {
			UserDefinedTypeSymbol userType = ((UserDefinedTypeSymbol) variableType);
			appendAssign("#" + variable.getAddr(), arrayAlloc(createTempVar(), userType.getComponents().size()));
			for (int i = 0; i < userType.getComponents().size(); i++) {
				if (userType.getComponents().get(i).getType() instanceof ArrayTypeSymbol) {
					String offset = appendInstruction(InstructionType.ADD, (variable.getAddr().charAt(0) != '$' ? "#"
							: "")
							+ variable.getAddr(), appendAssignNewVar(4 * i + ""));
					String fieldAddr = recursiveArrayAlloc(new ArrayList<Expression>(userType.getComponents()
							.get(i).getArrayAddr()));
					appendAssign("*" + offset, fieldAddr.charAt(0) == '&' ? fieldAddr.substring(1) :  fieldAddr);
				}
			}
		}
		return new Sentence();
	}

	public Expression declareConstant(TypeSymbol type, int value) {
		String var = appendAssignNewVar(value + "");
		return new Expression(type, var);
	}

	public Expression declareConstant(TypeSymbol type, String value) {
		String var = appendAssignNewVar(value);
		return new Expression(type, var);
	}

	public Expression declareLiteral(String value) {
		StringTypeSymbol typeSymbol = new StringTypeSymbol(value);
		VariableSymbol literal = new VariableSymbol(createStringVar(), typeSymbol, new ArrayList<FunctionSymbol>(),
				true);
		try {
			symbolTable.addVariableSymbol(literal);
		} catch (DuplicatedSymbolException e) {
			// Nunca debe ocurrir.
			e.printStackTrace();
		}
		return new Expression(typeSymbol, "#" + literal.getId());
	}

	public void functionStartPreamble(String id, boolean isFunction) {
		setNextLabel(id);
		currentMode = Mode.COMMON;
		FunctionSymbol s = symbolTable.getFunctionSymbol(id, isFunction);
		symbolTable.enterFunctionContext(s);
	}

	public FunctionSymbol functionStart(String id, List<VariableSymbol> input, List<VariableSymbol> output,
			boolean isFunction) {
		/*
		 * NOTA: nNo borrar las instrucciones de abajo que hacen de NOP porque
		 * se rompe todo. Martín
		 */
		appendAssignNewVar("0");
		nextLabel = "_end_init_params";
		appendAssignNewVar("0");
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol(id, isFunction);
		for (VariableSymbol param : functionSymbol.getInputParams()) {
			checkConstraints(new LValue(param.getAddr(), param.getType(), param.getConstraints(), false), param
					.getAddr());
		}
		return functionSymbol;
	}

	public void functionEnd(Sentence block) {

		/* Verificar que todas las variables hayan sido inicializadas. */
		Collection<Symbol> symbols = symbolTable.getLocalFunctionSymbols();
		for (Symbol s : symbols) {
			if (s instanceof VariableSymbol) {
				VariableSymbol vs = (VariableSymbol) s;
				if (!vs.isInitialized() && !vs.isInputParam()) {
					reportError("Variable " + vs.getId() + " was never set.");
				}
			}
		}
		appendInstruction(InstructionType.RET, null, null, null);

		/* Backpatchear y salir del contexto. */
		backpatch(block.getNextList(), getNextInstruction() - 1);
		symbolTable.leaveFunctionContext();
	}

	public Sentence constraintEnd(String constraintName, Expression exp) {

		appendInstruction(InstructionType.IF_NEQ, exp.getAddr(), "0", null);
		Expression name = declareLiteral("\"" + constraintName + "\"");
		appendParam(name.getAddr());
		appendCall("_exit", 0);
		nextLabel = createLabel();
		List<Integer> list = new ArrayList<Integer>();
		list.add(nextInstruction - 3);
		backpatch(list, nextLabel);
		appendInstruction(InstructionType.RET, null, null, null);
		symbolTable.leaveFunctionContext();
		return new Sentence();
	}

	public void declareType(String id, List<VariableSymbol> components) {

		/* El tipo ya existe en la tabla, lo puso la primera pasada. */
		UserDefinedTypeSymbol userType = (UserDefinedTypeSymbol) symbolTable.getTypeSymbol(id);
		for (int i = 0; i < components.size(); i++) {
			if (components.get(i).getConstraints() != null) {
				userType.getComponents().get(i).setConstraints(components.get(i).getConstraints());
			} else {
				userType.getComponents().get(i).setConstraints(new ArrayList<FunctionSymbol>());
			}
		}
	}

	public Sentence ifSentenceStart(Expression condition) {
		Sentence ret = new Sentence();
		ret.getNextList().add(getNextInstruction());
		appendInstruction(InstructionType.IF_EQ, condition.getAddr(), "0", null);
		symbolTable.enterBlockContext();
		return ret;
	}

	public Sentence ifSentenceEnd(Sentence ifSentence, Sentence ifBlock, int n, int m2, Sentence elseifBlock, int m3,
			Sentence elseBlock) {
		Sentence ret = null;
		ret = new Sentence();
		if (m2 == getNextInstruction()) {
			// ATENCION, SI BIEN LA ISNTRUCCION NOP PARECE INNECESARIA ES RE
			// IMPORTANTE PORQUE SI NO SE PISAN LOS LABELS Y A VECES QUEDAN
			// LABELS INDEFINIDOS, Martín!
			setNextLabel(createLabel());
			backpatch(ifSentence.getNextList(), getNextLabel());
			/* Agrego un NOP (No borrar, leer nota de arriba!!!!) */
			appendAssignNewVar("0");
		} else {
			backpatch(ifSentence.getNextList(), m2);
		}
		ret.getNextList().addAll(ifBlock.getNextList());
		ret.getQuitList().addAll(ifBlock.getQuitList());
		ret.getNextList().add(n);
		if (elseifBlock != null) {
			ret.getQuitList().addAll(elseifBlock.getQuitList());
			ret.getNextList().addAll(elseifBlock.getNextList());
		}
		if (elseBlock != null) {
			ret.getQuitList().addAll(elseBlock.getQuitList());
			ret.getNextList().addAll(elseBlock.getNextList());
		}
		symbolTable.leaveBlockContext();
		return ret;
	}

	public Sentence elseIfSentenceStart(Expression exp) {
		Sentence ret = null;
		symbolTable.enterBlockContext();
		if (exp.getType().equals(BoolTypeSymbol.getInstance())) {
			ret = new Sentence();
			ret.getNextList().add(getNextInstruction());
			appendInstruction(InstructionType.IF_EQ, exp.getAddr(), "0", null);
			return ret;
		} else {
			reportError("Else if condition must be a boolean expression.");
			ret = null;
		}
		return ret;
	}

	public Sentence elseIfSentenceEnd(Sentence ifSentence, Sentence ifBlock) {
		Sentence ret = new Sentence();
		symbolTable.leaveBlockContext();
		appendInstruction(InstructionType.GOTO, null, null, null);
		setNextLabel(createLabel());
		backpatch(ifSentence.getNextList(), getNextLabel());
		backpatch(ifBlock.getNextList(), getNextLabel());
		ret.getNextList().add(getNextInstruction() - 1);
		ret.getQuitList().addAll(ifBlock.getQuitList());
		return ret;
	}

	public Sentence elseIfSentenceBlock(Sentence block, Sentence elseIf) {
		Sentence ret = new Sentence();
		if (block != null) {
			ret.getNextList().addAll(block.getNextList());
			ret.getQuitList().addAll(block.getQuitList());
		}
		ret.getNextList().addAll(elseIf.getNextList());
		ret.getQuitList().addAll(elseIf.getQuitList());
		return ret;
	}

	public String loopSentenceStart() {
		return appendAssignNewVar("0");
	}

	public ForSentence loopSentenceBlockStart(LoopType loopType, String tempVar, Expression exp) {
		ForSentence ret = new ForSentence(tempVar, exp, null, -1);
		ret.getNextList().add(getNextInstruction());
		appendInstruction(loopType == LoopType.UNTIL ? InstructionType.IF_EQ : InstructionType.IF_NEQ, exp.getAddr(),
				"1", null);
		appendAssign(ret.getCounterAddr(), "1");
		symbolTable.enterBlockContext();
		return ret;
	}

	public Sentence loopSentenceBlockEnd(Integer m, ForSentence loopSentence, Sentence loopBlock) {
		symbolTable.leaveBlockContext();
		appendInstruction(InstructionType.GOTO, getInstructionLabel(m + 1), null, null);
		Sentence ret = new Sentence();
		ret.getQuitList().addAll(loopBlock.getQuitList());
		ret.getNextList().add(getNextInstruction());
		appendInstruction(InstructionType.IF_NEQ, loopSentence.getCounterAddr(), "0", null);
		backpatch(loopSentence.getNextList(), getNextInstruction() - 1);
		return ret;
	}

	public Sentence loopSentenceEnd(Sentence loopBlock, Sentence loopEndBlock, Sentence ifNeverBlock, Integer m,
			Sentence ifQuitBlock) {
		Sentence ret = new Sentence();
		if (m == getNextInstruction()) {
			setNextLabel(createLabel());
			backpatch(loopBlock.getQuitList(), getNextLabel());
		} else {
			backpatch(loopBlock.getQuitList(), m);
		}
		ret.getNextList().addAll(loopBlock.getNextList());
		ret.getNextList().addAll(loopEndBlock.getNextList());
		ret.getNextList().addAll(ifNeverBlock.getNextList());
		ret.getQuitList().addAll(ifNeverBlock.getQuitList());

		if (ifQuitBlock != null) {
			ret.getNextList().addAll(ifQuitBlock.getNextList());
			ret.getQuitList().addAll(ifQuitBlock.getQuitList());
		}
		return ret;
	}

	public Sentence loopSentenceBlockInstruction(Sentence block, int m, Sentence instr) {
		Sentence ret = new Sentence();
		ret.getQuitList().addAll(block.getQuitList());
		ret.getQuitList().addAll(instr.getQuitList());
		backpatch(block.getNextList(), m);
		ret.getNextList().addAll(instr.getNextList());
		return ret;
	}

	public Sentence quitSentence(Expression exp) {
		Sentence ret = new Sentence();

		if (exp != null) {
			if (exp.getType().equals(BoolTypeSymbol.getInstance())) {
				ret.getQuitList().add(getNextInstruction());
				appendInstruction(InstructionType.IF_EQ, exp.getAddr(), "1", null);
			} else {
				reportError("Quit if condition must be a boolean expression.");
			}
		} else {
			ret.getQuitList().add(getNextInstruction());
			appendInstruction(InstructionType.GOTO, null, null, null);
		}

		return ret;
	}

	public Sentence ifNeverSentence(Sentence block) {
		Sentence ret = new Sentence();
		ret.getNextList().add(getNextInstruction());
		appendInstruction(InstructionType.GOTO, null, null, null);

		if (block != null) {
			ret.getNextList().addAll(block.getNextList());
			ret.getQuitList().addAll(block.getQuitList());
		}
		return ret;
	}

	public ForSentence forSentenceBegin(String id, Expression exp) {
		symbolTable.enterBlockContext();
		if (!(exp.getType() instanceof ArrayTypeSymbol)) {
			reportError("For instruction may only iterate over arrays.");
			appendAssignNewVar("ERROR");
			appendAssignNewVar("ERROR");
			return new ForSentence("ERROR", new Expression(IntTypeSymbol.getInstance(), "ERROR"), "EROR", -1);
		}

		VariableSymbol iterator = new VariableSymbol(id, ((ArrayTypeSymbol) exp.getType()).getComponentsType(),
				new ArrayList<FunctionSymbol>(), false);
		try {
			iterator.setInitialized(true);
			symbolTable.addVariableSymbol(iterator);
		} catch (DuplicatedSymbolException e) {
			e.printStackTrace(); // Nunca pasa.
		}

		if (exp.getAddr() == null) {
			exp = buildArray(((LValue) exp).getLValues(), ((LValue) exp).getLValues().get(0).getType());
		}

		/* Generar la variable que cuenta la posicion actual */
		String iteratorIndex = appendAssignNewVar("0");

		int conditionalInstructionNumber = getNextInstruction();
		nextLabel = createLabel();
		/* Obtener la longitud del arreglo. */
		String arraySize = createTempVar();
		appendParam(exp.getAddr());
		appendParam("&" + arraySize);
		appendCall("_array_size", 2);
		// appendInstruction(InstructionType.ADD, arraySize, arraySize, "1");

		ForSentence ret = new ForSentence(iteratorIndex, exp, arraySize, conditionalInstructionNumber);
		ret.getNextList().add(getNextInstruction());
		appendInstruction(InstructionType.IF_EQ, iteratorIndex, arraySize, null);

		LValue lvalue = new LValue("#" + id, ((ArrayTypeSymbol) exp.getType()).getComponentsType(), false);
		String vector = appendAssignNewVar(/* "*" + */exp.getAddr());
		String vectorPosition = arrayGet(vector, iteratorIndex);
		// String tempVar = createTempVar();
		// appendInstruction(InstructionType.ADD, tempVar, vector,
		// iteratorIndex);
		// String rvalueAddr = appendAssignNewVar("*" + tempVar);
		Expression rvalue = new LValue("*" + vectorPosition, ((ArrayTypeSymbol) exp.getType()).getComponentsType(),
				false);
		assignValue(lvalue, rvalue, true);

		return ret;
	}

	public Sentence forSentenceBlock(Integer m, ForSentence forSentence, Sentence forBlock) {
		symbolTable.leaveBlockContext();
		backpatch(forBlock.getNextList(), getNextInstruction());
		appendInstruction(InstructionType.ADD, forSentence.getCounterAddr(), forSentence.getCounterAddr(), "4");
		appendInstruction(InstructionType.GOTO, getInstructionLabel(forSentence.getConditionalInstructionNumber())
				.toString(), null, null);
		setNextLabel(createLabel());

		Sentence ret = new Sentence();
		ret.getNextList().add(getNextInstruction());
		appendInstruction(InstructionType.IF_NEQ, forSentence.getSizeAddr(), "0", null);
		ret.getNextList().addAll(forSentence.getNextList());
		return ret;
	}

	public Sentence forSentenceEnd(Sentence forBlock, Sentence endForBlock, Sentence ifNeverBlock, Integer m,
			Sentence ifQuitBlock) {
		Sentence ret = new Sentence();
		if (ifQuitBlock == null) {
			setNextLabel(createLabel());
			backpatch(forBlock.getQuitList(), getNextLabel());
		} else {
			backpatch(forBlock.getQuitList(), m);
			ret.getNextList().addAll(ifQuitBlock.getNextList());
			ret.getQuitList().addAll(ifQuitBlock.getQuitList());
		}
		backpatch(forBlock.getNextList(), getNextInstruction() - 1);
		// ret.getNextList().addAll(forBlock.getNextList());
		ret.getNextList().addAll(endForBlock.getNextList());
		ret.getNextList().addAll(ifNeverBlock.getNextList());
		ret.getQuitList().addAll(ifNeverBlock.getQuitList());

		return ret;
	}

	public Sentence blockSentence(Sentence block, int m, Sentence instr) {
		Sentence ret = new Sentence();

		if (block.getNextList().size() > 0) {
			if (getNextInstruction() == m) {
				setNextLabel(createLabel());
				backpatch(block.getNextList(), getNextLabel());
			} else {
				backpatch(block.getNextList(), m);
			}
		}
		ret.getQuitList().addAll(block.getQuitList());

		if (instr != null) {
			ret.getQuitList().addAll(instr.getQuitList());
			ret.getNextList().addAll(instr.getNextList());
		}
		return ret;
	}

	public Expression mathExpression(MathOperator operator, Expression exp1, Expression exp2) {
		Expression errorRet = new Expression(IntTypeSymbol.getInstance(), "ERROR");

		/* Los operandos deben ser int o real */
		if ((!(exp1.getType() instanceof IntTypeSymbol) && !(exp1.getType() instanceof DoubleTypeSymbol))
				|| (!(exp2.getType() instanceof IntTypeSymbol) && !(exp2.getType() instanceof DoubleTypeSymbol))) {
			reportError("Math operands must be either int or double.");
			return errorRet;
		}
		if ((exp1.getType() instanceof IntTypeSymbol && exp2.getType() instanceof IntTypeSymbol)) {
			InstructionType opcode = null;
			opcode = operator.getInstructionType();
			if (operator == MathOperator.UNARY_MINUS) {
				opcode = InstructionType.SUB;
				exp1 = new Expression(IntTypeSymbol.getInstance(), appendAssignNewVar("0"));
			}
			String var = appendInstruction(opcode, exp1.getAddr(), exp2.getAddr());
			return new Expression(IntTypeSymbol.getInstance(), var);
		} else {
			String addr1 = castExpression(exp1, DoubleTypeSymbol.getInstance());
			String addr2 = castExpression(exp2, DoubleTypeSymbol.getInstance());
			String opcode = operator.getCastFunction();
			if (operator == MathOperator.MODULE) {
				reportError("Modulus operands must be integers.");
				return new Expression(IntTypeSymbol.getInstance(), "ERROR");
			}
			String tempVar = createTempVar();
			appendParam(addr1);
			appendParam(addr2);
			appendParam("&" + tempVar);
			appendCall(opcode, 3);
			return new Expression(DoubleTypeSymbol.getInstance(), tempVar);
		}
	}

	public Expression booleanExpressionNot(Expression exp) {
		if (exp.getType().equals(BoolTypeSymbol.getInstance())) {
			String var = appendInstruction(InstructionType.SUB, "1", exp.getAddr());
			return new Expression(BoolTypeSymbol.getInstance(), var);
		} else {
			reportError("Not operator requires a boolean argument.");
			return new Expression(BoolTypeSymbol.getInstance(), "ERROR");
		}
	}

	public Expression booleanExpressionAnd(Expression exp1, Expression exp2) {
		if (exp1.getType().equals(BoolTypeSymbol.getInstance()) && exp2.getType().equals(BoolTypeSymbol.getInstance())) {
			String var = appendAssignNewVar("0");
			String label = createLabel();
			appendInstruction(InstructionType.IF_EQ, exp1.getAddr(), "0", label);
			appendInstruction(InstructionType.IF_EQ, exp2.getAddr(), "0", label);
			appendAssign(var, "1");
			nextLabel = label;
			return new Expression(BoolTypeSymbol.getInstance(), var);
		} else {
			reportError("And operator requires boolean arguments.");
			return new Expression(BoolTypeSymbol.getInstance(), "ERROR");
		}
	}

	public Expression booleanExpressionOr(Expression exp1, Expression exp2) {
		if (exp1.getType().equals(BoolTypeSymbol.getInstance()) && exp2.getType().equals(BoolTypeSymbol.getInstance())) {
			String var = appendAssignNewVar("1");
			String label = createLabel();
			appendInstruction(InstructionType.IF_EQ, exp1.getAddr(), "1", label);
			appendInstruction(InstructionType.IF_EQ, exp2.getAddr(), "1", label);
			appendAssign(var, "0");
			nextLabel = label;
			return new Expression(BoolTypeSymbol.getInstance(), var);
		} else {
			reportError("Or operator requires boolean arguments.");
			return new Expression(BoolTypeSymbol.getInstance(), "ERROR");
		}
	}

	public Sentence assignValue(LValue lvalue, Expression rvalue, boolean mustCheckConstraint) {
		Sentence ret = new Sentence();
		Sentence errorRet = new Sentence();

		backPatchLvalue(lvalue);

		/*
		 * Si tengo una asignacion de valores escalares o variables de usuario
		 * de tipo vector
		 */
		if (lvalue.getLValues().size() == 0
				&& (!(rvalue instanceof LValue) || ((LValue) rvalue).getLValues().size() == 0)) {
			/* Validar tipos */
			if (!(rvalue.getType() != null && rvalue.getType().isAssignableTo(lvalue.getType()))) {
				reportError("Cannot cast from rvalue of type " + rvalue.getType() + " to lvalue of type "
						+ lvalue.getType());
				return errorRet;
			}

			/* Generar el addr segun sea componente de un arreglo o no */
			String rvalueAddr = rvalue.getAddr();
			if (!(rvalue instanceof LValue) || !((LValue) rvalue).isArray()) {
				rvalueAddr = rvalue.getAddr();
			} else {
				rvalueAddr = /* "*" + */rvalue.getAddr();
			}
			rvalue.setAddr(rvalueAddr);

			lvalue.setInitialized();
			String castedAddr = castExpression(rvalue, lvalue.getType());

			if (lvalue.isArray()) {
				appendAssign(/* "*" + */lvalue.getAddr(), castedAddr);
				String newAddr = appendAssignNewVar(lvalue.getAddr());
				LValue newLvalue = new LValue(newAddr, lvalue.getType(), lvalue.getConstraints(), lvalue.isArray());
				checkConstraints(newLvalue, /* "*" + */rvalueAddr);
			} else {
				appendAssign(lvalue.getAddr(), castedAddr);
				checkConstraints(lvalue, lvalue.getAddr());
			}
			if (lvalue.getFieldFrom() != null && mustCheckConstraint) {
				checkConstraints(lvalue.getFieldFrom(), lvalue.getFieldFrom().getAddr());
			}

			return ret;

		}
		/* Si tengo que asignar listas... */
		else {
			if (!(rvalue instanceof LValue)) {
				reportError("rvalue must be a list");
				return errorRet;
			}

			LValue castedRvalue = (LValue) rvalue;

			if (lvalue.getLValues().size() > 0 && castedRvalue.getLValues().size() != lvalue.getLValues().size()) {
				reportError("Lvalue and rvalue lists must have the same length.");
				return errorRet;
			}

			/*
			 * Si a la izquierda tengo un arreglo, genero yo mis lvalues. Sino,
			 * los levanto de la lista.
			 */
			if (lvalue.getType() instanceof ArrayTypeSymbol && lvalue.getLValues().size() == 0) {

				String addr = lvalue.getAddr();
				List<Integer> arrayGetList = new ArrayList<Integer>();
				for (int i = 0; i < castedRvalue.getLValues().size(); i++) {
					String componentAddr = arrayGet(addr, 4 * i + "");
					arrayGetList.add(getNextInstruction() - 5);

					assignValue(new LValue("*" + componentAddr, ((ArrayTypeSymbol) lvalue.getType())
							.getComponentsType(), new ArrayList<FunctionSymbol>(),
							((ArrayTypeSymbol) lvalue.getType()) instanceof ArrayTypeSymbol), castedRvalue.getLValues()
							.get(i), true);
				}
				lvalue.setArrayGetCallLine(arrayGetList);
				backPatchLvalue(lvalue);
			} else {
				if (!(lvalue.getType() instanceof ListTypeSymbol) && rvalue.getType() instanceof ListTypeSymbol) {
					reportError("rvalue cannot be a list");
					return errorRet;
				}

				int i = 0;

				for (Expression currentExpression : lvalue.getLValues()) {
					if (!(currentExpression instanceof LValue)) {
						reportError("expression must be lvalue");
						return errorRet;
					}
					LValue currentLValue = (LValue) currentExpression;
					assignValue(currentLValue, castedRvalue.getLValues().get(i), false);
					i++;
					// if (currentLValue.getFieldFrom() != null) {
					// checkConstraints(currentLValue.getFieldFrom(),
					// currentLValue.getFieldFrom().getAddr());
					// }
				}

				for (Expression currentExpression : lvalue.getLValues()) {
					LValue currentLValue = (LValue) currentExpression;
					if (currentLValue.getFieldFrom() != null) {
						checkConstraints(currentLValue.getFieldFrom(), currentLValue.getFieldFrom().getAddr());
					}
				}

			}
			checkConstraints(lvalue, lvalue.getAddr());
		}

		lvalue.setInitialized();
		return ret;
	}

	public LValue lvalueExpList(ArrayList<Expression> exps) {

		LValue errorRet = new LValue("ERROR", IntTypeSymbol.getInstance(), false);

		if (exps.size() == 0) {
			reportError("Expression list must have at least 1 element");
			return errorRet;
		}

		TypeSymbol type = exps.get(0).getType();
		boolean sameType = true;
		List<Expression> lvaluesList = new ArrayList<Expression>();
		ListTypeSymbol retType = new ListTypeSymbol();
		int i = 0;
		for (Expression exp : exps) {
			i++;
			if (!type.equals(exp.getType())) {
				sameType = false;
			}
			lvaluesList.add(exp);
			retType.getTypes().add(exp.getType());
		}

		if (sameType) {
			return new LValue(lvaluesList, new ArrayTypeSymbol(type));
		} else {
			return new LValue(lvaluesList, retType);
		}

	}

	public Expression relationalExpression(InstructionType instructionType, Expression exp1, Expression exp2) {

		String addr1 = null;
		String addr2 = null;
		/* De ser necesario castear */

		if (exp1.getType() instanceof StringTypeSymbol && exp2.getType() instanceof StringTypeSymbol
				&& (instructionType == InstructionType.IF_EQ || instructionType == InstructionType.IF_NEQ)) {
			addr1 = exp1.getAddr();
			addr2 = exp2.getAddr();
		}
		else if (((exp1.getType() instanceof UserDefinedTypeSymbol && exp2.getType() instanceof NullTypeSymbol)
				|| (exp1.getType() instanceof NullTypeSymbol && exp2.getType() instanceof UserDefinedTypeSymbol))
				&& (instructionType == InstructionType.IF_EQ || instructionType == InstructionType.IF_NEQ)) {
			addr1 = exp1.getAddr();
			addr2 = exp2.getAddr();			
		}
		else if (!(exp1.getType() instanceof IntTypeSymbol && exp2.getType() instanceof IntTypeSymbol)
				|| !(exp1.getType() instanceof DoubleTypeSymbol && exp2.getType() instanceof DoubleTypeSymbol)) {
			addr1 = castExpression(exp1, DoubleTypeSymbol.getInstance());
			addr2 = castExpression(exp2, DoubleTypeSymbol.getInstance());
		} else {
			addr1 = exp1.getAddr();
			addr2 = exp2.getAddr();
		}

		String var = appendAssignNewVar("1");
		String label = createLabel();
		appendInstruction(instructionType, addr1, addr2, label);
		appendAssign(var, "0");
		setNextLabel(label);
		return new Expression(BoolTypeSymbol.getInstance(), var);
	}

	public Expression functionCallExpression(FunctionCall funcCall, boolean mustReturnValue) {
		FunctionSymbol functionSymbol;

		/* Obtengo la función de la tabla de símbolos */
		functionSymbol = symbolTable.getFunctionSymbol(funcCall.getId(), true);
		if (functionSymbol == null) {
			reportError("Undefined function: " + funcCall.getId());
			return new Expression(IntTypeSymbol.getInstance(), "ERROR");
		}

		/* La cantidad de parámetros de entrada debe coincidir. */
		if (funcCall.getParams().size() != functionSymbol.getInputParams().size()) {
			reportError("Function " + funcCall.getId() + " requires " + functionSymbol.getInputParams().size()
					+ " input parameters.");
			return new Expression(new ListTypeSymbol(), "ERROR");
		}

		/* Si debe retornar un valor y no retorna ninguno, es un error. */
		if (functionSymbol.getOutputParams().size() == 0 && mustReturnValue) {
			reportError("Function " + funcCall.getId() + " has no output parameters.");
			return new Expression(IntTypeSymbol.getInstance(), "ERROR");
		}

		/* Castear y pushear los parámetros actuales */
		for (int i = 0; i < funcCall.getParams().size(); i++) {

			/*
			 * Si el parámetro es un arreglo que no tiene seteado el addr, lo
			 * construyo y le genero el addr.
			 */
			if (funcCall.getParams().get(i).getType() instanceof ArrayTypeSymbol
					&& funcCall.getParams().get(i).getAddr() == null) {
				LValue rvalue = (LValue) funcCall.getParams().get(i);
				funcCall.getParams().get(i)
						.setAddr(
								buildArray(rvalue.getLValues(),
										((ArrayTypeSymbol) rvalue.getType()).getMostDeepComponentType()).getAddr());
			}

			String castedAddr = castExpression(funcCall.getParams().get(i), functionSymbol.getInputParams().get(i)
					.getType());
			if (castedAddr == null) {
				reportError("Actual parameter of type " + funcCall.getParams().get(i).getType()
						+ " cannot be assigned to formal parameter of type "
						+ functionSymbol.getInputParams().get(i).getType());
				return new Expression(new ListTypeSymbol(), "ERROR");
			}
			appendParam(castedAddr);
		}

		/* Pushear variables temporales que van a ser los parametros de salida */
		LValue ret = new LValue(new ArrayList<Expression>(), new ListTypeSymbol());
		for (int i = 0; i < functionSymbol.getOutputParams().size(); i++) {
			String tempAddr = createTempVar();
			ret.getLValues().add(new LValue(tempAddr, functionSymbol.getOutputParams().get(i).getType(), false));
			appendParam("&" + tempAddr);
		}

		/* Invocar a la función. */
		appendCall(funcCall.getId(), functionSymbol.getInputParams().size() + functionSymbol.getOutputParams().size());

		/* Si la función participa de una expresión... */
		if (mustReturnValue) {
			if (functionSymbol.getOutputParams().size() == 1) {
				return new Expression(functionSymbol.getOutputParams().get(0).getType(), ret.getLValues().get(0)
						.getAddr());
			} else {
				return ret;
			}
		} else {
			return new Expression(IntTypeSymbol.getInstance(), "");
		}
	}

	/**
	 * Chequea que el tipo sea correcto
	 * 
	 * @param type Tipo de la variable
	 * @param arrayType Si es un arreglo, viene el addr de la variable que tiene
	 *            la cantidad de componentes. Si no, viene null.
	 * @param constraints
	 * @return
	 */
	public Type checkType(TypeSymbol type, ArrayList<Expression> dim, List<Constraint> constraints) {
		List<FunctionSymbol> constraintSymbols = new ArrayList<FunctionSymbol>();
		TypeSymbol typeSymbol = type;

		/* Procesar los constraints */
		FunctionSymbol constraintSymbol;
		for (Constraint constraint : constraints) {
			constraintSymbol = symbolTable.getFunctionSymbol(constraint.getId(), false);

			/*
			 * Verificar tipos y promover de ser necesario
			 */
			if (constraintSymbol != null && constraintSymbol.getInputParams().size() == 0) {
				reportError("constraint must have at leat one argument");
				return new Type(typeSymbol, constraintSymbols, dim);
			}

			constraintSymbols.add(new FunctionSymbol(constraint.getId(), constraint.getParams(), false, parser
					.getFileName()));
		}

		return new Type(typeSymbol, constraintSymbols, dim);
	}

	public TypeSymbol checkUserDefinedType(String id) {
		TypeSymbol userType;
		userType = symbolTable.getTypeSymbol(id);
		if (userType == null) {
			reportError("Undefined type: " + id);
		}
		return (UserDefinedTypeSymbol) userType;
	}

	/**
	 * Genera código intermedio para cuando se setea una constraint en una
	 * variable.
	 * 
	 * @param id Nombre del constraint aplicado.
	 * @param params Lista de parámetros actuales.
	 * @return El símbolo constraint.
	 */
	public Constraint newConstraint(String id, List<Expression> params) {
		Constraint constraint = new Constraint(id);
		FunctionSymbol functionSymbol;

		/*
		 * El primero va a ser el receptor, lo va a poner declareVariable o
		 * formalParam.
		 */
		constraint.getParams().add(null);

		/* El constraint tiene que estar en la tabla de símbolos. */
		functionSymbol = symbolTable.getFunctionSymbol(id, false);

		if (functionSymbol == null) {
			reportError("Undeclared constraint: " + id);
			return constraint;
		}

		for (int i = 0; i < params.size(); i++) {
			/* Validar que el parametro actual sea asignable al formal. */
			if (!(params.get(i).getType().isAssignableTo(functionSymbol.getInputParams().get(i + 1).getType()))) {
				reportError("Invalid constraint parameter.");
				return constraint;
			}

			/* De ser necesario castearlo, y asignarlo. */
			LValue castedParam = new LValue(createTempVar(), functionSymbol.getInputParams().get(i + 1).getType(),
					false);
			/* Si el parametro real es un arreglo anonimo, hacer el newArray */
			if (params.get(i) instanceof LValue && ((LValue) params.get(i)).getLValues().size() > 0) {
				arrayAlloc(castedParam.getAddr(), ((LValue) params.get(i)).getLValues().size());
				appendAssign(castedParam.getAddr(), castedParam.getAddr());
				VariableSymbol variableSymbol = new VariableSymbol(castedParam.getAddr(), castedParam.getAddr(),
						castedParam.getType(), new ArrayList<FunctionSymbol>(), true, VariableType.LOCAL);
				List<Expression> dimentions = new ArrayList<Expression>();
				dimentions.add(new Expression(IntTypeSymbol.getInstance(), ((LValue) params.get(i)).getLValues().size()
						+ ""));
				((ArrayTypeSymbol) variableSymbol.getType()).setDimentions(dimentions);
				try {
					symbolTable.addVariableSymbol(variableSymbol);
				} catch (DuplicatedSymbolException e) {
					// Nunca debe pasar
					e.printStackTrace();
				}
			}
			assignValue(castedParam, params.get(i), true);
			VariableSymbol actualParam = new VariableSymbol(castedParam.getAddr(), castedParam.getAddr(), params.get(i)
					.getType(), new ArrayList<FunctionSymbol>(), false, VariableType.INPUT);
			constraint.getParams().add(actualParam);
		}
		return constraint;
	}

	public LValue lvalueVariable(String id) {
		VariableSymbol vs = symbolTable.getVariableSymbol(id);
		LValue ret;
		if (vs == null) {
			reportError(id + " variable not defined");
			return new LValue("ERROR", IntTypeSymbol.getInstance(), false);
		} else {
			ret = new LValue("#" + vs.getAddr(), vs.getType(), vs.getConstraints(), false);
			ret.setVariable(vs);
		}
		return ret;
	}

	/**
	 * Valida que una lista de expresiones sean valores izquierdos, y la
	 * convierte en una lista de valores izquierdos.
	 */
	public List<LValue> lvalueExpressionList(List<Expression> exps) {
		List<LValue> ret = new ArrayList<LValue>();
		/* Validar que todas sean LValues */
		for (Expression exp : exps) {
			if (!(exp instanceof LValue)) {
				reportError("Expressions must be lvalues.");
				return ret;
			}
			ret.add((LValue) exp);
		}
		return ret;
	}

	public LValue lvalueArray(LValue lvalue, Expression exp) {
		LValue errorRet = lvalue;
		if (!(exp.getType() instanceof IntTypeSymbol)) {
			reportError("Index must be of type integer");
			return errorRet;
		}

		/* si es una lista heterogenea */
		if (!(lvalue.getType() instanceof ArrayTypeSymbol)) {
			reportError("lvalue is not an array");
			return errorRet;
		}

		if (lvalue.getAddr() == null) {
			lvalue.setAddr((buildArray(lvalue.getLValues(), lvalue.getLValues().get(0).getType())).getAddr());
		}

		String offset = appendInstruction(InstructionType.MULT, exp.getAddr(), "4");
		String arrayAddr = arrayGet(lvalue.getAddr(), offset);

		LValue lvalueRet = new LValue("*" + arrayAddr, ((ArrayTypeSymbol) lvalue.getType()).getComponentsType(),
				new ArrayList<FunctionSymbol>(), true);

		lvalueRet.setVariable(lvalue.getVariable());
		lvalueRet.getArrayGetCallLine().add(getNextInstruction() - 5);
		return lvalueRet;
	}

	public LValue lvalueArrayInterval(LValue lvalue, Integer min, Integer max) {
		LValue errorRet = lvalue;
		if (!(lvalue.getType() instanceof ArrayTypeSymbol)) {
			reportError("lvalue must be an array");
			return errorRet;
		}

		List<Expression> lvalueList = new ArrayList<Expression>();

		List<Integer> arrayGetList = new ArrayList<Integer>();
		for (int i = min; i <= max; i++) {
			lvalueList.add(new LValue(arrayGet(lvalue.getAddr(), i * 4 + ""), ((ArrayTypeSymbol) lvalue.getType())
					.getComponentsType(), new ArrayList<FunctionSymbol>(), true));
			arrayGetList.add(getNextInstruction() - 5);
		}

		LValue ret = new LValue(lvalueList, new ArrayTypeSymbol(((ArrayTypeSymbol) lvalue.getType())
				.getComponentsType()));
		ret.setVariable(lvalue.getVariable());
		ret.setArrayGetCallLine(arrayGetList);
		return ret;
	}

	public LValue lvalueAccess(LValue id, List<String> ids) {
		LValue ret;
		LValue errorRet = id;

		if (!(id.getType() instanceof UserDefinedTypeSymbol)) {
			reportError("LValue variable is not a user defined type.");
			return errorRet;
		}

		if (id.getLValues().size() > 0) {
			reportError("Dereferenced lvalue cannot be a list.");
			return errorRet;
		}

		String addr = appendAssignNewVar(id.getAddr());

		List<Expression> lvaluesList = new ArrayList<Expression>();
		ListTypeSymbol listTypeSymbol = new ListTypeSymbol();
		UserDefinedTypeSymbol type = (UserDefinedTypeSymbol) id.getType();
		for (String i : ids) {
			int componentIndex = type.getComponentIndex(i);

			if (componentIndex == -1) {
				reportError("Invalid field: " + i);
				return errorRet;
			}

			String indexAddr = appendInstruction(InstructionType.MULT, "4", componentIndex + "");

			ret = new LValue("*" + appendInstruction(InstructionType.ADD, addr, indexAddr), type.getComponents().get(
					componentIndex).getType(), type.getComponents().get(componentIndex).getConstraints(), true);
			ret.setFieldFrom(id);

			for (int j = 0; j < type.getComponents().get(componentIndex).getConstraints().size(); j++) {
				type.getComponents().get(componentIndex).getConstraints().get(j).getInputParams().set(
						0,
						new VariableSymbol("", ret.getAddr(), ret.getType(), new ArrayList<FunctionSymbol>(), false,
								VariableType.LOCAL));
			}

			if (ids.size() == 1) {
				ret.setVariable(id.getVariable());
				return ret;
			} else {
				lvaluesList.add(ret);
				listTypeSymbol.getTypes().add(type.getComponents().get(componentIndex).getType());
			}
		}
		ret = new LValue(lvaluesList, listTypeSymbol);
		ret.setVariable(id.getVariable());
		return ret;
	}

	public VariableSymbol formalParam(String id, Type type, List<FunctionSymbol> constraints) {
		VariableSymbol param = symbolTable.getVariableSymbol(id);
		if (param.getType() instanceof ArrayTypeSymbol) {
			((ArrayTypeSymbol) param.getType()).setDimentions(type.getArrayAddr());
			for (Expression exp : type.getArrayAddr()) {
				if (exp.getAddr().charAt(0) == '$') {
					try {

						symbolTable.addVariableSymbol(new VariableSymbol(exp.getAddr(), exp.getAddr(), exp.getType(),
								new ArrayList<FunctionSymbol>(), true, VariableType.LOCAL));
					} catch (DuplicatedSymbolException e) {
						// Nunca debe pasar
						e.printStackTrace();
					}
				}
			}
		}

		if (param == null) {
			/*
			 * Si el parámetro no estaba en la tabla, la única posibilidad es
			 * que la función haya estado definida más de una vez, con distintos
			 * prototipos, y el parámetro era de otro prototipo. No me importa,
			 * lo va a agarrar functionStart.
			 */
			return new VariableSymbol("ERROR", type.getType(), constraints, false);
		}
		/* Ponerme como receptor del constraint */
		param.setConstraints(constraints);
		for (FunctionSymbol constraint : constraints) {
			constraint.getInputParams().set(0, param);
		}
		return param;
	}

	public Expression nullConstant() {
		String var = appendAssignNewVar("0");
		return new Expression(NullTypeSymbol.getInstance(), var);
	}

	private void checkConstraints(LValue lvalue, String rvalueAddr) {
		if (lvalue.getConstraints() == null) {
			return;
		}
		for (FunctionSymbol actualConstraint : lvalue.getConstraints()) {
			FunctionSymbol formalConstraint = symbolTable.getFunctionSymbol(actualConstraint.getId(), false);
			if (formalConstraint == null) {
				reportError("Undefined constraint");
				return;
			}

			VariableSymbol actualParam = actualConstraint.getInputParams().get(0);
			String castedAddr = castExpression(lvalue.getAddr().charAt(0) == '$' ? lvalue.getAddr() : lvalue.getAddr()
					.charAt(0) == '#' ? lvalue.getAddr() : "#" + lvalue.getAddr(), lvalue.getType(), formalConstraint
					.getInputParams().get(0).getType());
			appendParam(castedAddr);

			/* Castear y pushear los parametros. */
			for (int i = 1; i < actualConstraint.getInputParams().size(); i++) {
				actualParam = actualConstraint.getInputParams().get(i);
				/*
				 * No es necesario castear los parametros de entrada de los
				 * constraints porque ya fueron casteado en la definicion de la
				 * variable
				 */
				// castedAddr = castExpression(actualParam.getAddr().charAt(0)
				// == '$' ? actualParam.getAddr() : "#"
				// + actualParam.getAddr(), actualParam.getType(),
				// formalConstraint.getInputParams().get(i)
				// .getType());
				appendParam(actualParam.getAddr());
			}

			appendCall(actualConstraint.getId(), actualConstraint.getInputParams().size());
		}
		return;
	}

	private String recursiveArrayAlloc(List<Expression> exps) {
		String arrayAddr = createTempVar();
		arrayAlloc(arrayAddr, exps.get(0).getAddr());

		if (exps.size() > 1) {
			String iterador = appendAssignNewVar("0");

			String label = createLabel();
			int m = getNextInstruction();
			appendInstruction(InstructionType.IF_GTE, iterador, exps.get(0).getAddr(), label);

			exps.remove(0);
			appendAssign(arrayAddr + "[" + iterador + "]", recursiveArrayAlloc(exps));
			appendInstruction(InstructionType.ADD, iterador, iterador, "4");
			appendInstruction(InstructionType.GOTO, getInstructionLabel(m), null, null);
			setNextLabel(label);
		}
		return "&" + arrayAddr;
	}

	private void appendInstruction(InstructionType opcode, String op1, String op2, String op3) {
		if (currentMode == Mode.COMMON) {
			Instruction newInstruction = new Instruction(nextLabel, opcode, op1, op2, op3);
			instructions.add(newInstruction);
			nextInstruction++;
			nextLabel = null;
		} else {
			Instruction newInstruction = new Instruction(nextLabel, opcode, op1, op2, op3);
			blockInstructions.add(newInstruction);
			blockNextInstruction++;
			nextLabel = null;
		}
	}

	private String appendInstruction(InstructionType opcode, String op2, String op3) {
		String var = createTempVar();
		appendInstruction(opcode, var, op2, op3);
		return var;
	}

	private void appendParam(String param) {
		appendInstruction(InstructionType.PARAM, param, null, null);
	}

	private void appendCall(String fn, int params) {
		appendInstruction(InstructionType.CALL, fn, params + "", null);
	}

	private void appendAssign(String lvalue, String rvalue) {
		appendInstruction(InstructionType.ASSIGN, lvalue, rvalue, null);
	}

	private String appendAssignNewVar(String rvalue) {
		return appendInstruction(InstructionType.ASSIGN, rvalue, null);
	}

	private String arrayAlloc(String name, int dim) {
		return arrayAlloc(name, dim + "");
	}

	private String arrayAlloc(String name, String dim) {
		appendInstruction(InstructionType.PARAM, "&" + name, null, null);
		appendInstruction(InstructionType.PARAM, dim, null, null);
		appendInstruction(InstructionType.CALL, "_array_alloc", "2", null);
		return name;
	}

	private String arrayGet(String addr, String offset) {
		String var = createTempVar();
		appendParam("0");
		appendParam(addr);
		appendParam(offset);
		appendParam("&" + var);
		appendCall("_array_get", 4);
		return var;
	}

	private String createTempVar() {
		return "$t" + tempVarCount++;
	}

	/**
	 * Dada una expresion y un tipo, castea la expresion al tipo dado de ser
	 * necesario. Si la castea, retorna el nuevo addr donde está el valor
	 * casteado. Si no hace falta castear, retorna el mismo addr de la expresión
	 * recibida. Si no se puede castear, retorna null.
	 * 
	 * @param exp Expresion que se quiere castear.
	 * @param type Tipo al que se quiere castear.
	 * @return El addr donde queda el valor casteado, o null si no se puede
	 *         castear.
	 */
	private String castExpression(Expression exp, TypeSymbol type) {
		return castExpression(exp.getAddr(), exp.getType(), type);
	}

	private String castExpression(String expAddr, TypeSymbol expType, TypeSymbol targetType) {
		try {
			String ret = "";

			ret = expType.castTo(targetType);
			if (ret == null) {
				return expAddr;
			}

			appendInstruction(InstructionType.PARAM, expAddr, null, null);
			String out = createTempVar();
			appendInstruction(InstructionType.PARAM, "&" + out, null, null);
			appendInstruction(InstructionType.CALL, ret, "2", null);
			return out;
		} catch (IncompatibleTypesException e) {
			reportError("Type " + expType + " cannot be casted to " + targetType);
			return "INVALID_CAST";
		}
	}

	private String createStringVar() {
		return "_string" + stringCount++;
	}

	/**
	 * Al todas las instrucciones cuya numeración aparece en la lista
	 * instrNumberList (instrucciones GOTO), se les asigna como dirección de
	 * salto instrNumber. Si existía una etiqueta en instrNumber, se hace el
	 * goto a dicha etiqueta. Si no existe, se la crea en el momento, para poder
	 * completar la etiqueta en el goto.
	 * 
	 * @param instrNumberList Lista con los números de instrucciones que tienen
	 *            saltos pendientes, a completar con la etiqueta a instrNumber.
	 * @param instrNumber Número de instrucción a la cual deben saltar todos los
	 *            gotos pendientes.
	 */

	private void backpatch(List<Integer> instrNumberList, String label) {
		List<Instruction> currentIntructions = (currentMode == Mode.COMMON) ? instructions : blockInstructions;
		Instruction instruction;

		for (int i : instrNumberList) {
			instruction = currentIntructions.get(i);
			int labelLine = getLabelLine(label);
			if (instruction.getInstructionType() == InstructionType.GOTO) {
				instruction.setOp1(label);
				instruction.setPointer(labelLine);
			} else if (instruction.getInstructionType() == InstructionType.IF_EQ
					|| instruction.getInstructionType() == InstructionType.IF_NEQ
					|| instruction.getInstructionType() == InstructionType.IF_GT
					|| instruction.getInstructionType() == InstructionType.IF_GTE
					|| instruction.getInstructionType() == InstructionType.IF_LT
					|| instruction.getInstructionType() == InstructionType.IF_LTE) {
				instruction.setOp3(label);
				instruction.setPointer(labelLine);
			} else if (instruction.getInstructionType() == InstructionType.CALL) {
				instruction.setOp1(label);
				instruction.setPointer(labelLine);
			}
		}
		instrNumberList.clear();
	}

	private void backpatch(List<Integer> instrNumberList, int instrNumber) {
		if (instrNumberList.size() > 0) {
			backpatch(instrNumberList, getInstructionLabel(instrNumber));
		}
	}

	/*
	 * private void backpatch(int instrNumber, String Label) { List<Integer>
	 * aux = new ArrayList<Integer>(); aux.add(instrNumber); backpatch(aux,
	 * Label); }
	 */
	private int getLabelLine(String label) {
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i).getLabel() != null && instructions.get(i).getLabel().equals(label))
				return i;
		}
		return -1;
	}

	/**
	 * Dado un número de instrucción, retorna la etiqueta de la misma. Si no
	 * tiene etiqueta, crea una y la retorna.
	 * 
	 * @param i Número de instrucción a consultar.
	 * @return La etiqueta de dicha instrucción.
	 */
	private String getInstructionLabel(int i) {
		if (i < 0) {
			return "ERROR";
		}
		List<Instruction> currentIntructions = (currentMode == Mode.COMMON) ? instructions : blockInstructions;

		int size = (currentMode == Mode.COMMON) ? instructions.size() : blockInstructions.size();
		if (i == size) {
			String label = createLabel();
			nextLabel = label;
			return label;
		}

		Instruction targetInstruction;

		try {
			targetInstruction = currentIntructions.get(i);
		} catch (IndexOutOfBoundsException e) {
			throw e;
		}
		String label;
		if (targetInstruction.getLabel() != null) {
			label = targetInstruction.getLabel();
		} else {
			label = createLabel();
			targetInstruction.setLabel(label);
		}
		return label;
	}

	/**
	 * Crea una nueva etiqueta, y la retorna.
	 * 
	 * @return Nombre de la nueva etiqueta.
	 */
	private String createLabel() {
		return "label" + labelCount++;
	}

	private void setNextLabel(String label) {
		nextLabel = label;
	}

	private String getNextLabel() {
		return nextLabel;
	}

	private Expression buildArray(List<Expression> elements, TypeSymbol type) {
		String arrayAddr = createTempVar();
		arrayAlloc(arrayAddr, elements.size());

		/* Parche para manejar arreglos anonimos correctamente */
		String tempVar = appendAssignNewVar("&" + arrayAddr);
		/* Construir el tipo del arreglo */
		ArrayTypeSymbol arrayTypeSymbol = new ArrayTypeSymbol(type, Arrays.asList(new String[] { new Integer(elements
				.size()).toString() }));
		try {
			symbolTable.addVariableSymbol(new VariableSymbol(tempVar, arrayTypeSymbol, new ArrayList<FunctionSymbol>(),
					true));
		} catch (DuplicatedSymbolException e) {
			// Nunca debe pasar
			e.printStackTrace();
		}
		for (int i = 0; i < elements.size(); i++) {
			Expression currentElement = elements.get(i);
			if (currentElement.getAddr() == null) {
				currentElement = buildArray(((LValue) currentElement).getLValues(), ((LValue) currentElement)
						.getLValues().get(0).getType());
			}
			String lvaluePointer = arrayGet(arrayAddr, (i * 4) + "");
			appendAssign("*" + lvaluePointer, currentElement.getAddr());
		}
		return new Expression(new ArrayTypeSymbol(type), arrayAddr);
	}

	private void arrayGetBackpatch(List<Integer> instructionNumber) {
		if (currentMode == Mode.COMMON) {
			for (Integer i : instructionNumber) {
				if (instructions.get(i).getInstructionType() != InstructionType.PARAM) {
					throw new RuntimeException("Wrong instruction list.");
				}
				instructions.get(i).setOp1("1");
			}
		} else {
			for (Integer i : instructionNumber) {
				if (blockInstructions.get(i).getInstructionType() != InstructionType.PARAM) {
					throw new RuntimeException("Wrong instruction list.");
				}

				blockInstructions.get(i).setOp1("1");
			}
		}

	}

	private void backPatchLvalue(LValue lvalue) {
		/* backpatcher el parametro de arrayGet */

		if (lvalue.getLValues() != null && lvalue.getLValues().size() > 0) {
			for (Expression currentExpression : lvalue.getLValues()) {
				if (currentExpression instanceof LValue) {
					LValue currentLValue = (LValue) currentExpression;
					arrayGetBackpatch(currentLValue.getArrayGetCallLine());
					if (currentLValue.getAddr() == null) {
						backPatchLvalue(currentLValue);
					}
				}
			}
		}
		arrayGetBackpatch(lvalue.getArrayGetCallLine());
	}

}
