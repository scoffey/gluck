package ar.edu.itba.dcc.tp.semantic.nodes;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol;

/**
 * Nodo del árbol sintáctico que representa a una constraint. Contiene el nombre y la lista de parámetros de
 * entrada.
 */
public class Constraint {
	private String id;
	private List<VariableSymbol> params = new ArrayList<VariableSymbol>();

	public Constraint(String id) {
		super();
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public List<VariableSymbol> getParams() {
		return params;
	}

}
