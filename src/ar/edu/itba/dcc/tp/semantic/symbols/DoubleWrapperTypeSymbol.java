package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class DoubleWrapperTypeSymbol extends TypeSymbol {

	private static DoubleWrapperTypeSymbol me = new DoubleWrapperTypeSymbol();

	private DoubleWrapperTypeSymbol() {
		super("TODO");
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/lang/Double;";
	}

	public static DoubleWrapperTypeSymbol getInstance() {
		return me;
	}

}
