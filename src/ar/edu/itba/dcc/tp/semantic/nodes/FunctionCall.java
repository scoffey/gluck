package ar.edu.itba.dcc.tp.semantic.nodes;

import java.util.List;

/**
 * Nodo del árbol sintáctico que representa una invocación a una función. Contiene el nombre de la misma y la
 * lista de parámetros actuales.
 */
public class FunctionCall {
	private String id;
	private List<Expression> params;

	/**
	 * Crea un nuevo nodo de tipo invocación a función, con un nombre y una lista de parámetros actuales.
	 * 
	 * @param id Nombre de la función invocada.
	 * @param params Lista de parámetros actuales
	 */
	public FunctionCall(String id, List<Expression> params) {
		super();
		this.id = id;
		this.params = params;
	}

	public String getId() {
		return id;
	}
	
	public List<Expression> getParams() {
		return params;
	}
}
