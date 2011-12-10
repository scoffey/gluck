package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class NativeArrayTypeSymbol extends TypeSymbol {
	public NativeArrayTypeSymbol(String jasminType) {
		super("TODO");
		this.jasminType = jasminType;
	}

	private String jasminType;
	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		return jasminType;
	}
}
