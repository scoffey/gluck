package ar.edu.itba.dcc.tp.semantic.symbols;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Tipo de símbolo para cuando tengo posponer el chequeo de tipos en una asignación.
 */
public class UndefinedTypeSymbol extends TypeSymbol {

    private FunctionSymbol functionSymbol;

    public UndefinedTypeSymbol() {
    	super("undefined");
    	this.functionSymbol = null;    	
	}
    
    public UndefinedTypeSymbol(FunctionSymbol functionSymbol) {
        super("undefined");
        this.functionSymbol = functionSymbol;
    }

    public UndefinedTypeSymbol(String id) {
        super(id);
        this.functionSymbol = null;
    }

    public FunctionSymbol getFunctionSymbol() {
        return functionSymbol;
    }

    @Override
    public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
        return null;
    }

	@Override
	public String jasminType(String packageName, String className) {
		return null;
	}

    @Override
    public String toString() {
        return "undefined";
    }   
}
