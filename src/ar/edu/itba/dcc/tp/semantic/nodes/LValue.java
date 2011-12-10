package ar.edu.itba.dcc.tp.semantic.nodes;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;

/**
 * Un LValue es una expresión, en donde el addr de las cosas es en realidad un
 * lvalue del usuario, y que además contiene una lista de constraints aplicadas
 * a la variable. Adicionalmente, un LValue puede ser un posible componente de
 * una estructura, porque al momento que yo reduzco un lvalue a un ID, yo no sé
 * si eso va a ser una variable en una lista de expresiones, o un campo de un
 * tipo en una desreferencia. Entonces en este ultimo caso, guardo en el addr el
 * nombre, y en type pongo Undefined.
 */
public class LValue extends Expression {

	private List<Expression> lvalues;

	/* Campos propios del LValue */
	private List<FunctionSymbol> constraints;
	private boolean isArray = false;
	private List<Integer> arrayGetCallLine = new ArrayList<Integer>();
	private LValue fieldFrom;
	// Si este LValue es componente de un vector, tengo el addr del vector y el
	// indice.
//	private String offset;

	/*
	 * Me guardo una referencia a la variable que estoy usando (para setearla en
	 * inicializada cuando participa de una asignación
	 */
	private VariableSymbol variable;

	public LValue(List<Expression> lvalues, TypeSymbol type) {
		super(type, null);
		this.lvalues = lvalues;
		this.isArray = false;
	}

	public LValue(String addr, TypeSymbol type, boolean isArray) {
		this(addr, type, new ArrayList<FunctionSymbol>(), isArray);
	}

	public LValue(String addr, TypeSymbol type, List<FunctionSymbol> constraints, boolean isArray) 
	{
		super(type, addr);
		this.lvalues = new ArrayList<Expression>();
		this.isArray = isArray;
		this.constraints = constraints;
	}
	public List<FunctionSymbol> getConstraints() {
		return constraints;
	}


	public void setVariable(VariableSymbol variable) {
		this.variable = variable;
	}

	public VariableSymbol getVariable() {
		return variable;
	}

	public void setInitialized() {
		if (variable != null) {
			variable.setInitialized(true);
		}
	}

	public List<Expression> getLValues() {
		return lvalues;
	}
	
	public boolean isArray(){
		return isArray;
	}
	
	public List<Integer> getArrayGetCallLine() {
		return arrayGetCallLine;
	}
	
	public void setArrayGetCallLine(List<Integer> arrayGetCallLine) {
		this.arrayGetCallLine = arrayGetCallLine;
	}
	
	public void setFieldFrom(LValue fieldFrom) {
		this.fieldFrom = fieldFrom;
	}
	
	public LValue getFieldFrom() {
		return fieldFrom;
	}
}
