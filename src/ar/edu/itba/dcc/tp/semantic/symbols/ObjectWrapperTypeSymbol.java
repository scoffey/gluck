package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class ObjectWrapperTypeSymbol extends TypeSymbol {
	private static ObjectWrapperTypeSymbol me = new ObjectWrapperTypeSymbol();
	
	private ObjectWrapperTypeSymbol() {
		super("TODO");
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/lang/Object;";
	}
	
	public static ObjectWrapperTypeSymbol getInstance(){
		return me;
	}

}
