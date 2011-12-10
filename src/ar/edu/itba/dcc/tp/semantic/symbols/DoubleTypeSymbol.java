package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Tipo double de datos (real). Es manejado como un singleton.
 */
public class DoubleTypeSymbol extends TypeSymbol {
	private static DoubleTypeSymbol me = new DoubleTypeSymbol();

	private DoubleTypeSymbol() {
		super("real");
	}

	public static DoubleTypeSymbol getInstance() {
		return me;
	}

	@Override
	public String toString() {
		return "real";
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else if (typeSymbol instanceof IntTypeSymbol) {
			return "_double2int";
		} else if (typeSymbol instanceof StringTypeSymbol) {
			return "_double2string";
		} else  if (typeSymbol instanceof ObjectWrapperTypeSymbol){
			return "_double2string";
		} else {
			throw new IncompatibleTypesException();
		}
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "D";
	}
	
	@Override
	public boolean equals(Symbol obj) {
		return obj instanceof DoubleTypeSymbol;
	}

}