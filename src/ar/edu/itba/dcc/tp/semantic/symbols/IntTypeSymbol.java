package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Clase que representa al tipo int. Es manejada como un singleton.
 */
public class IntTypeSymbol extends TypeSymbol {
	private static IntTypeSymbol me = new IntTypeSymbol();

	private IntTypeSymbol() {
		super("int");
	}

	public static IntTypeSymbol getInstance() {
		return me;
	}

	@Override
	public String toString() {
		return "int";
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else if (typeSymbol instanceof DoubleTypeSymbol) {
			return "_int2double";
		} else if (typeSymbol instanceof StringTypeSymbol) {
			return "_int2string";
		} else if (typeSymbol instanceof ObjectWrapperTypeSymbol) {
			return "_int2string";
		} else
			throw new IncompatibleTypesException();
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "I";
	}
	
	@Override
	public boolean equals(Symbol obj) {
		return obj instanceof IntTypeSymbol;
	}
}
