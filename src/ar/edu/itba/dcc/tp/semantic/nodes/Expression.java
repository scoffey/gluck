package ar.edu.itba.dcc.tp.semantic.nodes;

import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;

/**
 * Nodo del árbol sintáctico de tipo expresión. Contiene su tipo y el nombre de
 * la variable en donde queda el resultado (addr).
 */
public class Expression {
	private TypeSymbol type;
	private String addr;
	private FunctionSymbol functionSymbol;

	public Expression() {
		super();
	}

	/**
	 * Crea un nuevo nodo expresión con un determinado tipo y addr.
	 * 
	 * @param type Tipo del nodo a crear.
	 * @param addr Dirección donde va a quedar el resultado.
	 */
	public Expression(TypeSymbol type, String addr) {
		super();
		this.type = type;
		this.addr = addr;
	}

	public String getAddr() {
		return addr;
	}

	public TypeSymbol getType() {
		return type;
	}

	public void setAddr(String newAddr) {
		this.addr = newAddr;
	}

	public FunctionSymbol getFunction() {
		return functionSymbol;
	}

	public void setFunction(FunctionSymbol function) {
		this.functionSymbol = function;
	}
	
	public void setType(TypeSymbol type) {
		this.type = type;
	}

}
