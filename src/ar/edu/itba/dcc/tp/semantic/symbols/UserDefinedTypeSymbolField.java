package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class UserDefinedTypeSymbolField extends TypeSymbol {
	private String object;
	private VariableSymbol field;
	private String receptor;

	public UserDefinedTypeSymbolField(String object, String receptor, VariableSymbol field) {
		super("TODO");
		this.object = object;
		this.receptor = receptor;
		this.field = field;
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String jasminType(String packageName, String className) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getReceptor() {
		return receptor;
	}

	public String getObject() {
		return object;
	}

	public VariableSymbol getField() {
		return field;
	}
}
