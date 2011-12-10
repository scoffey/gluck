package ar.edu.itba.dcc.tp.semantic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.parser.ParserAdapter;
import ar.edu.itba.dcc.tp.run.Compiler;
import ar.edu.itba.dcc.tp.run.Compiler.CompilerModes;
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
import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol.VariableType;
import ar.edu.itba.dcc.tp.util.ProcessLogger;

/**
 * Clase abstracta de la cual debe extender cualquier clase que quiera agregar acciones a los métodos callback
 * que se invocan a medida que se parsea el archivo de entrada. En este caso, las dos implementaciones son
 * <code>SymbolTableBuilder</code>, que genera la tabla de símbolos con los símbolos de nivel superior, y
 * <code>CodeGenerator</code> que genera código intermedio y completa la tabla de símbolos.
 */
public abstract class SemanticAnalyzer {

	protected ParserAdapter parser;
	protected ProcessLogger logger;
	protected boolean reportedErrors = false;
	protected SymbolTable symbolTable;

	public SemanticAnalyzer(ParserAdapter parser, ProcessLogger logger) {
		this.parser = parser;
		this.logger = logger;
		this.symbolTable = new SymbolTable();
	}
	
	
	public void loadStandardLibrary(){
		try {
			symbolTable.loadStandardLibrary();
		} catch (IOException e) {
			reportError("Gluck standard library not found.");
		}
	}

	public void setSymbolTable(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}

	public SymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void enterContext() {
		symbolTable.enterBlockContext();
	}

	public void leaveContext() {
		symbolTable.leaveBlockContext();
	}

	public boolean hasReportedErrors() {
		return reportedErrors;
	}

	/**
	 * Declara una nueva variable de un tipo determinado y la agrega a la tabla de símbolos. Si exp es !=
	 * null, entonces la inicializa con dicha expresión.
	 */
	public abstract Sentence declareVariable(String id, Type type, Expression exp);

	public abstract Expression declareConstant(TypeSymbol type, int value);
	
	public abstract Expression declareConstant(TypeSymbol type, String value);

	public abstract Expression declareLiteral(String value);
	
	public abstract Integer getNextInstruction();
	
	/**
	 * @return Número de línea en donde se generó el goto.
	 */
	public abstract Integer generateGoto();
	
	public abstract void functionStartPreamble(String id, boolean isFunction);

	/**
	 * Valida la declaración de una función, crea el símbolo correspondiente, lo agrega a la tabla de símbolos
	 * y además lo retorna en su nombre.
	 * 
	 * @param id Nombre de la función.
	 * @param input Lista de parámetros de entrada.
	 * @param output Lista de parámetros de salida.
	 * @param isFunction True si es una funcion, false si es un constraint.
	 * @return El símbolo de función creado.
	 */
	public abstract FunctionSymbol functionStart(String id, List<VariableSymbol> input, List<VariableSymbol> output,
			boolean isFunction);

	public abstract void functionEnd(Sentence block);

	public abstract Sentence constraintEnd(String constraintName, Expression exp);

	public abstract void declareType(String id, List<VariableSymbol> components);

	public abstract Sentence ifSentenceStart(Expression condition);

	/**
	 * Genera el código intermedio para el final de una sentencia if. Se invoca al final de toda la
	 * instrucción if/else if/else. Realiza el backpatching de las cosas que quedaron pendientes.
	 * 
	 * @param ifSentence Sentencia que retornó la función ifSentenceStart cuando inicializó el bloque if.
	 * @param ifBlock Sentencia retornada por el bloque de condición verdadera del if.
	 * @param n Número de instrucción donde se generó el goto del final del bloque ifBlock, que se debe
	 *            backpatchear.
	 * @param m2 Número de instrucción donde comenzó la lista de else if. A esta dirección debe saltar el
	 *            ifSentence cuando la condición sea falsa.
	 * @param elseifBlock Sentencia retornada por la lista de elseif.
	 * @param m3 Número de instrucción donde comenzó el código del else.
	 * @param elseBlock Sentencia generada por el bloque else.
	 */
	public abstract Sentence ifSentenceEnd(Sentence ifSentence, Sentence ifBlock, int n, int m2, Sentence elseifBlock,
			int m3, Sentence elseBlock);

	public abstract Sentence elseIfSentenceStart(Expression exp);

	/**
	 * Genera el código intermedio para el final de un bloque elseif. Es invocada justo al terminar el bloque,
	 * y realiza los backpatches necesarios.
	 * 
	 * @param ifSentence Sentencia que retornó ifSentenceStart al principio.
	 * @param ifBlock Sentencia que generó el bloque del if.
	 * @return La sentencia generada.
	 */
	public abstract Sentence elseIfSentenceEnd(Sentence ifSentence, Sentence ifBlock);

	/**
	 * Genera el código intermedio para el bloque de un elseif. Como puede haber varios elseif, la
	 * construcción se hace de manera recursiva. Un bloque elseIf es retornado, y luego es pasado como segundo
	 * argumento a esta función para crear el próximo.
	 * 
	 * @param block Sentencia generada por el bloque del elseif actual.
	 * @param elseIf Sentencia retornada por esta misma función en otro elseif. (puede ser null en el caso de
	 *            que sea el último).
	 * @return La sentencia generada por el bloque elseif.
	 */
	public abstract Sentence elseIfSentenceBlock(Sentence block, Sentence elseIf);

	public abstract String loopSentenceStart();

	/**
	 * Genera el código intermedio para el comienzo de un bloque de una instrucción de loop (while o until).
	 * Genera el if goto para salir en caso de que la condición sea falsa, y luego le asigna 1 a la variable
	 * que controla si al menos entró una vez. Es invocada justo después de la evaluación de la condición,
	 * antes del bloque.
	 * 
	 * @param loopType Tipo de instrucción de ciclo (while o until).
	 * @param tempVar Variable para setear en 1 si entra al bloque.
	 * @param exp Condición a evaluar
	 * @return La sentencia generada.
	 */
	public abstract ForSentence loopSentenceBlockStart(LoopType loopType, String tempVar, Expression exp);

	/**
	 * Genera código intermedio para el final de un bloque de loop (while o until). Es invocada justo al final
	 * del bloque, antes de un posible ifnever, ifquit o ambos.
	 * 
	 * @param m Número de instrucción donde comnienza a evaluarse la expresión de la condición. A esta
	 *            instrucción debe saltar el goto que agregamos.
	 * @param loopSentence Sentencia retornada por loopSentenceBlockStart.
	 * @param loopBlock Sentencia generada por el bloque del loop.
	 * @return La sentencia generada.
	 */
	public abstract Sentence loopSentenceBlockEnd(Integer m, ForSentence loopSentence, Sentence loopBlock);

	/**
	 * Genera código intermedio para el final de todo un bloque de loop (incluyendo el ifnever y el ifquit.
	 * Realiza el backpatching de todas las cosas que quedaron pendientes.
	 * 
	 * @param loopBlock Bloque principal del loop.
	 * @param loopEndBlock Sentencia retornada por loopSentenceBlockEnd.
	 * @param ifNeverBlock Bloque generado por ifNeverSentenceBlock.
	 * @param m Número de instrucción donde comenzó el código del ifquit.
	 * @param ifQuitBlock Bloque del ifquit.
	 * @return La sentencia generada.
	 */
	public abstract Sentence loopSentenceEnd(Sentence loopBlock, Sentence loopEndBlock, Sentence ifNeverBlock, Integer m, Sentence ifQuitBlock);

	/**
	 * Acumula una sentencia a un bloque de instrucciones de loop. Propaga la quitList y la nextList y hace el
	 * backpatching de la nextList del bloque acumulado.
	 * 
	 * @param block Bloque acumulado hasta el momento.
	 * @param m Número de instrucción de la nueva instrucción.
	 * @param instr Nueva instrucción a acumular.
	 * @return La sentencia correspondiente al nuevo bloque.
	 */
	public abstract Sentence loopSentenceBlockInstruction(Sentence block, int m, Sentence instr);

	public abstract Sentence quitSentence(Expression exp);

	public abstract Sentence ifNeverSentence(Sentence block);

	/**
	 * Genera código intermedio para el comienzo de una sentencia de for. Es invocada justo antes del comienzo
	 * del bloque del ciclo.
	 * 
	 * @param id Variable iteradora.
	 * @param exp Expresión de tipo arreglo sobre la cual iterar.
	 * @return La sentencia generada.
	 */
	public abstract ForSentence forSentenceBegin(String id, Expression exp);

	/**
	 * Genera código intermedio para el final de un bloque de for (antes del ifnever o el ifquit). Incrementa
	 * la variable iteradora, genera el goto a la condición y hace backpatching de lo que pueda.
	 * 
	 * @param m Número de la primera instrucción del bloque del for (a la cual saltar).
	 * @param forSentence Sentencia generada por forSentenceBegin.
	 * @param forBlock Bloque principal del for.
	 * @return La sentencia generada.
	 */
	public abstract Sentence forSentenceBlock(Integer m, ForSentence forSentence, Sentence forBlock);

	/**
	 * Genera código intermedio para el final de una sentencia for (incluyendo el ifnever y el ifquit).
	 * 
	 * @param forBlock Bloque principal del for.
	 * @param endForBlock Bloque generado por forSentenceBlock.
	 * @param ifNeverBlock Bloque de la zona de ifnever.
	 * @param m Número de instrucción donde comienza el código del ifquit.
	 * @param ifQuitBlock Bloque de la zona de ifquit.
	 * @return La sentencia generada.
	 */
	public abstract Sentence forSentenceEnd(Sentence forBlock, Sentence endForBlock, Sentence ifNeverBlock, Integer m,
			Sentence ifQuitBlock);

	/**
	 * Agrega una instrucción a un bloque, propagando todas las listas que sean necesarias, y retorna la nueva
	 * sentencia que representa al bloque con la nueva instrucción.
	 * 
	 * @param block Bloque acumulado hasta el momento.
	 * @param m Número de instrucción de la nueva instrucción.
	 * @param instr Nueva instrucción.
	 * @return El bloque con la nueva instrucción.
	 */
	public abstract Sentence blockSentence(Sentence block, int m, Sentence instr);

	public abstract Expression mathExpression(MathOperator operator, Expression exp1, Expression exp2);

	public abstract Expression booleanExpressionNot(Expression exp);

	public abstract Expression booleanExpressionAnd(Expression exp1, Expression exp2);

	public abstract Expression booleanExpressionOr(Expression exp1, Expression exp2);

	public abstract Sentence assignValue(LValue lvalue, Expression rvalue, boolean flag);

	public abstract LValue lvalueExpList(ArrayList<Expression> exps);

	public abstract Expression relationalExpression(InstructionType instructionType, Expression exp1, Expression exp2);

	public abstract Expression functionCallExpression(FunctionCall funcCall, boolean mustReturnValue);

	/**
	 * Chequea que el tipo sea correcto
	 * 
	 * @param type Tipo de la variable
	 * @param arrayType Si es un arreglo, viene el addr de la variable que tiene la cantidad de componentes.
	 *            Si no, viene null.
	 * @param constraints
	 * @return
	 */
	public abstract Type checkType(TypeSymbol type, ArrayList<Expression> dim, List<Constraint> constraints);

	public abstract TypeSymbol checkUserDefinedType(String id);

	/**
	 * Genera código intermedio para cuando se setea una constraint en una variable.
	 * 
	 * @param id Nombre del constraint aplicado.
	 * @param params Lista de parámetros actuales.
	 * @return El símbolo constraint.
	 */
	public abstract Constraint newConstraint(String id, List<Expression> params);

	public abstract LValue lvalueVariable(String id);

	/**
	 * Valida que una lista de expresiones sean valores izquierdos, y la convierte en una lista de valores
	 * izquierdos.
	 */
	public abstract List<LValue> lvalueExpressionList(List<Expression> exps);

	public abstract LValue lvalueArray(LValue lvalue, Expression exp);

	public abstract LValue lvalueArrayInterval(LValue lvalue, Integer min, Integer max);

	public abstract LValue lvalueAccess(LValue id, List<String> ids);

	public abstract VariableSymbol formalParam(String id, Type type, List<FunctionSymbol> constraints);

	public List<VariableSymbol> changeVariableType(List<VariableSymbol> v, VariableType type) {
		for (VariableSymbol var : v) {
			var.setVariableType(type);
		}
		return v;
	}

	public abstract Expression nullConstant();

	public void reportError(String message) {
		reportedErrors = true;
		if (Compiler.getInstance().getMode().equals(CompilerModes.MODE_SEMANTIC)
				|| Compiler.getInstance().getMode().equals(CompilerModes.MODE_IC)
				|| Compiler.getInstance().getMode().equals(CompilerModes.MODE_BYTECODES)
				|| Compiler.getInstance().getMode().equals(CompilerModes.MODE_JASMIN)
				|| Compiler.getInstance().getMode().equals(CompilerModes.MODE_SYMBOL_TABLE))
			logger.showError(parser.getFileName(), parser.line(), parser.column(), message);
	}
}
