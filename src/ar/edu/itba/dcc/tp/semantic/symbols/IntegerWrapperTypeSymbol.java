package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class IntegerWrapperTypeSymbol extends TypeSymbol {

	private static IntegerWrapperTypeSymbol me = new IntegerWrapperTypeSymbol();
	
	private IntegerWrapperTypeSymbol() {
		super("TODO");
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/lang/Integer;";
	}
	
	public static IntegerWrapperTypeSymbol getInstance(){
		return me;
	}

}
