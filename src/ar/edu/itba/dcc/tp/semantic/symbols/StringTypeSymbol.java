package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Clase que representa al tipo de dato String. Es manejada como un singleton.
 */
public class StringTypeSymbol extends TypeSymbol {

	private String value;

	public StringTypeSymbol(String value) {
		super("string");
		this.value = value;
	}

	public StringTypeSymbol() {
		this(null);
	}

	@Override
	public String toString() {
		return "string";
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else if (typeSymbol instanceof ObjectWrapperTypeSymbol) {
			return null;
		} else {
			throw new IncompatibleTypesException();
		}
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/lang/String;";
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Symbol obj) {
		return obj instanceof StringTypeSymbol;
	}


}
