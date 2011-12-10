package ar.edu.itba.dcc.tp.semantic;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.parser.ParserAdapter;
import ar.edu.itba.dcc.tp.semantic.CodeGenerator.LoopType;
import ar.edu.itba.dcc.tp.semantic.CodeGenerator.MathOperator;
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
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.IntTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.NullTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.StringTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UndefinedTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.UserDefinedTypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;
import ar.edu.itba.dcc.tp.util.ProcessLogger;

/**
 * Analizador semántico para hacer la primera pasada sobre el archivo. Construye
 * la tabla de símbolos con aquellos símbolos de nivel superior (funciones,
 * restricciones, tipos del usuario y variables). No genera código intermedio.
 * No asigna restricciones a variables o argumentos, solamente se limita a crear
 * las restricciones, para que en la segunda pasada existan en la tabla.
 */
public class SymbolTableBuilder extends SemanticAnalyzer {

	public SymbolTableBuilder(ParserAdapter parser, ProcessLogger logger) {
		super(parser, logger);
	}

	public Type checkType(TypeSymbol type, ArrayList<Expression> dim, List<Constraint> constraints) {
		return new Type(type, new ArrayList<FunctionSymbol>(), dim);
	}

	public TypeSymbol checkUserDefinedType(String id) {
		TypeSymbol type = symbolTable.getTypeSymbol(id);
		if (type == null) {
			return new UndefinedTypeSymbol(id);
		} else {
			return type;
		}
	}

	public void declareType(String id, List<VariableSymbol> components) {

		/* Crear el tipo y agregarlo a la tabla. */
		TypeSymbol typeSymbol = new UserDefinedTypeSymbol(id, components, this.parser.getFullName());
		try {
			symbolTable.addTypeSymbol(typeSymbol);
		} catch (DuplicatedSymbolException e) {
			reportError("Duplicated type: " + id);
		}

		/* Marcar las componentes y corregir en caso de arreglos. */
		for (VariableSymbol c : components) {
			c.setVariableType(VariableSymbol.VariableType.COMPONENT);

			/* Tipos recursivos. */
			if (c.getType() instanceof UndefinedTypeSymbol) {
				if (c.getType().getId().equals(id)) {
					c.setType(typeSymbol);
				} else {
					reportError("Undefined type: " + c.getType().getId());
				}
			}

			TypeSymbol variableType = c.getType();
			if (c.getArrayAddr().size() > 0) {
				for (int i = 0; i < c.getArrayAddr().size(); i++) {
					variableType = new ArrayTypeSymbol(variableType);
				}
			}
			c.setType(variableType);
		}

	}

	public Sentence declareVariable(String id, Type type, Expression exp) {

		/* En caso de que sea arreglo, construir el tipo. */
		TypeSymbol variableType = type.getType();
		for (int i = 0; i < type.getArrayAddr().size(); i++) {
			variableType = new ArrayTypeSymbol(variableType);
		}

		/* Crear el símbolo y agregarlo a la tabla. */
		VariableSymbol variableSymbol = new VariableSymbol(id, variableType, type.getConstraints(), false);
		variableSymbol.setSourceFile(this.parser.getFullName());
		try {
			symbolTable.addVariableSymbol(variableSymbol);
		} catch (DuplicatedSymbolException e) {
			reportError("Redefined variable: " + id);
		}

		return new Sentence();
	}

	public VariableSymbol formalParam(String id, Type type, List<FunctionSymbol> constraints) {
		if (type.getType() instanceof UndefinedTypeSymbol) {
			reportError("Undefined type of formal parameter " + id);
			return new VariableSymbol(id, IntTypeSymbol.getInstance(), new ArrayList<FunctionSymbol>(), true);
		}
		TypeSymbol variableType = type.getType();
		for (int i = 0; i < type.getArrayAddr().size(); i++) {
			variableType = new ArrayTypeSymbol(variableType);
		}
		return new VariableSymbol(id, variableType, constraints, false);
	}

	public void functionStartPreamble(String id, boolean isFunction) {
		FunctionSymbol functionSymbol = new FunctionSymbol(id, null, null, isFunction, this.parser.getFullName());
		try {
			symbolTable.createFunctionContext(functionSymbol);
		} catch (DuplicatedSymbolException e) {
			reportError("Duplicated function: " + id);
		}
	}

	public FunctionSymbol functionStart(String id, List<VariableSymbol> input, List<VariableSymbol> output,
			boolean isFunction) {
		FunctionSymbol functionSymbol = symbolTable.getFunctionSymbol(id, isFunction);

		functionSymbol.setInputParams(input);
		functionSymbol.setOutputParams(output);

		try {
			for (VariableSymbol s : input) {
				symbolTable.addVariableSymbol(s);
			}
			for (VariableSymbol s : output) {
				symbolTable.addVariableSymbol(s);
			}
		} catch (DuplicatedSymbolException e) {
			reportError("Duplicated parameters.");
		}
		return functionSymbol;
	}

	public void functionEnd(Sentence block) {
		symbolTable.leaveFunctionContext();
	}

	public Sentence constraintEnd(String constraintName, Expression exp) {
		symbolTable.leaveFunctionContext();
		return new Sentence();
	}

	public Constraint newConstraint(String id, List<Expression> params) {
		/*
		 * Esto se deja para la segunda pasada, cuando existan todas las
		 * constraints en la tabla.
		 */
		return new Constraint(id);
	}

	public Sentence ifSentenceStart(Expression condition) {
		symbolTable.enterBlockContext();
		return new Sentence();
	}

	public Sentence ifSentenceEnd(Sentence ifSentence, Sentence ifBlock, int n, int m2, Sentence elseifBlock, int m3,
			Sentence elseBlock) {
		symbolTable.leaveBlockContext();
		return new Sentence();
	}

	public ForSentence forSentenceBegin(String id, Expression exp) {
		symbolTable.enterBlockContext();

		return new ForSentence("", exp, "");
	}

	public Sentence forSentenceEnd(Sentence forBlock, Sentence endForBlock, Sentence ifNeverBlock, Integer m,
			Sentence ifQuitBlock) {
		symbolTable.leaveBlockContext();
		return new Sentence();
	}

	/***************************************************************************
	 * 
	 * DE ACA PARA ABAJO NO APORTAN NADA EN ESTA PASADA
	 * 
	 **************************************************************************/

	public Expression functionCallExpression(FunctionCall funcCall, boolean mustReturnValue) {
		return new Expression(IntTypeSymbol.getInstance(), "");
	}

	public Sentence assignValue(LValue lvalue, Expression rvalue, boolean flag) {
		return new Sentence();
	}

	public Sentence blockSentence(Sentence block, int m, Sentence instr) {
		return new Sentence();
	}

	public Expression booleanExpressionAnd(Expression exp1, Expression exp2) {
		return new Expression(BoolTypeSymbol.getInstance(), "");
	}

	public Expression booleanExpressionNot(Expression exp) {
		return new Expression(BoolTypeSymbol.getInstance(), "");
	}

	public Expression booleanExpressionOr(Expression exp1, Expression exp2) {
		return new Expression(BoolTypeSymbol.getInstance(), "");
	}

	public Expression declareLiteral(String value) {
		return new Expression(new StringTypeSymbol(), "");
	}

	public Expression declareConstant(TypeSymbol type, int value) {
		return new Expression(type, "");
	}

	public Expression declareConstant(TypeSymbol type, String value) {
		return new Expression(type, "");
	}

	public Integer getNextInstruction() {
		return 0;
	}

	public Integer generateGoto() {
		return 0;
	}

	public Sentence elseIfSentenceBlock(Sentence block, Sentence elseIf) {
		return new Sentence();
	}

	public Sentence elseIfSentenceEnd(Sentence ifSentence, Sentence ifBlock) {
		return new Sentence();
	}

	public Sentence elseIfSentenceStart(Expression exp) {
		return new Sentence();
	}

	public Sentence forSentenceBlock(Integer m, ForSentence forSentence, Sentence forBlock) {
		return new Sentence();
	}

	public Sentence ifNeverSentence(Sentence block) {
		return new Sentence();
	}

	public Sentence loopSentenceBlockEnd(Integer m, ForSentence loopSentence, Sentence loopBlock) {
		return new Sentence();
	}

	public Sentence loopSentenceBlockInstruction(Sentence block, int m, Sentence instr) {
		return new Sentence();
	}

	public ForSentence loopSentenceBlockStart(LoopType loopType, String tempVar, Expression exp) {
		return new ForSentence("", exp, "");
	}

	public Sentence loopSentenceEnd(Sentence loopBlock, Sentence loopEndBlock, Sentence ifNeverBlock, Integer m,
			Sentence ifQuitBlock) {
		return new Sentence();
	}

	public String loopSentenceStart() {
		return "";
	}

	public LValue lvalueAccess(LValue id, List<String> ids) {
		return id;
	}

	public LValue lvalueArray(LValue lvalue, Expression exp) {
		return lvalue;
	}

	public LValue lvalueArrayInterval(LValue lvalue, Integer min, Integer max) {
		return lvalue;
	}

	public LValue lvalueExpList(ArrayList<Expression> exps) {
		return new LValue("", IntTypeSymbol.getInstance(), false);
	}

	public List<LValue> lvalueExpressionList(List<Expression> exps) {
		return new ArrayList<LValue>();
	}

	public LValue lvalueVariable(String id) {
		return new LValue("", IntTypeSymbol.getInstance(), false);
	}

	public Expression nullConstant() {
		return new Expression(NullTypeSymbol.getInstance(), "");
	}

	public Sentence quitSentence(Expression exp) {
		return new Sentence();
	}

	public Expression relationalExpression(InstructionType instructionType, Expression exp1, Expression exp2) {
		return new Expression(BoolTypeSymbol.getInstance(), "");
	}

	public Expression mathExpression(MathOperator operator, Expression exp1, Expression exp2) {
		return new Expression(IntTypeSymbol.getInstance(), "");
	}

}