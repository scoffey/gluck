package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Símbolo que representa una lista heterogénea de cosas. No hay una instancia
 * de este tipo en la tabla de símbolos, sino que solamente está asociada como
 * tipo de la expresión que se resuelve a una lista de expresiones.
 */
public class ListTypeSymbol extends TypeSymbol {
	private List<TypeSymbol> types;
	
	public ListTypeSymbol() {
		super("");
		this.types = new ArrayList<TypeSymbol>();
	}
	
	public int size() {
		return types.size();
	}

	public TypeSymbol getElementType(int index) {
		return types.get(index);
	}

	public List<TypeSymbol> getTypes() {
		return types;
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (typeSymbol instanceof ListTypeSymbol) { 
			ListTypeSymbol listType = (ListTypeSymbol)typeSymbol;
			if (this.getTypes().size() != listType.getTypes().size()) {
				throw new IncompatibleTypesException();
			}
			for (int i=0; i<listType.size(); i++) {
				if (!this.getElementType(i).isAssignableTo(listType.getElementType(i))) {
					throw new IncompatibleTypesException();
				}
			}
		}
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return null;
	}
}
