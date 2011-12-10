package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class NullTypeSymbol extends TypeSymbol {

	private static NullTypeSymbol me = new NullTypeSymbol();

	
	private NullTypeSymbol() {
		super("NULL");
	}
	
	public static NullTypeSymbol getInstance(){
		return me;
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (typeSymbol instanceof UserDefinedTypeSymbol){
			return null;
		}
		throw new IncompatibleTypesException();
	}

	@Override
	public String jasminType(String packageName, String className) {
		return null;
	}

}
