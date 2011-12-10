package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class ArrayListWrapperTypeSymbol extends TypeSymbol {
	private static ArrayListWrapperTypeSymbol me = new ArrayListWrapperTypeSymbol();
	
	private ArrayListWrapperTypeSymbol() {
		super("TODO");
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/util/ArrayList;";
	}
	
	public static ArrayListWrapperTypeSymbol getInstance(){
		return me;
	}

}
