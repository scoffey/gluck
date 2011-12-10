package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Tipo booleano de datos. Se maneja como un singleton.
 */
public class BoolTypeSymbol extends TypeSymbol {
	private static BoolTypeSymbol me = new BoolTypeSymbol();

	private BoolTypeSymbol() {
		super("bool");
	}

	public static BoolTypeSymbol getInstance() {
		return me;
	}

	@Override
	public String toString() {
		return "bool";
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else if (typeSymbol instanceof StringTypeSymbol) {
			return "_bool2string";
		} else if (typeSymbol instanceof ObjectWrapperTypeSymbol) {
			return "_bool2string";
		} else {
			throw new IncompatibleTypesException();
		}
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Z";
	}
	@Override
	public boolean equals(Symbol obj) {
		return obj instanceof BoolTypeSymbol;
	}

}