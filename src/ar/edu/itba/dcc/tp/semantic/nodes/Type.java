package ar.edu.itba.dcc.tp.semantic.nodes;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.symbols.FunctionSymbol;
import ar.edu.itba.dcc.tp.semantic.symbols.TypeSymbol;

/**
 * Nodo del árbol sintáctico que representa un tipo. Contiene la referencia al símbolo del tipo, la lista de
 * constraints aplicadas y en el caso de que sea un tipo arreglo n-dimensional, una lista de n elementos, con la
 * cantidad de elementos declarada en cada dimensión (arrayAddr).
 */
public class Type {
	private TypeSymbol type;
	private List<FunctionSymbol> constraints;
	private ArrayList<Expression> arrayAddr;

	/**
	 * Crea un nuevo nodo para un tipo, con la referencia al símbolo tipo, una lista de constraints y el addr
	 * de la dimensión en el caso de que sea un array.
	 * 
	 * @param type Referencia al tipo.
	 * @param constraints Lista de constraints que se aplican.
	 * @param arrayAddr Dirección donde está la dimensión, solo para arreglos.
	 */
	public Type(TypeSymbol type, List<FunctionSymbol> constraints, ArrayList<Expression> arrayAddr) {
		super();
		this.type = type;
		this.constraints = constraints;
		this.arrayAddr = arrayAddr;
	}

	/**
	 * @return La lista de constraints.
	 */
	public List<FunctionSymbol> getConstraints() {
		return constraints;
	}

	/**
	 * @return La dirección donde se encuentra la dimensión del array.
	 */
	public List<Expression> getArrayAddr() {
		return arrayAddr;
	}

	/**
	 * @return Referencia al tipo representado.
	 */
	public TypeSymbol getType() {
		return type;
	}

}
