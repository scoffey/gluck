package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

public class ArrayTypeSymbolComponent extends TypeSymbol {
	private String alias;
	private String classname;
	private List<String> indexesAddrs;

	public ArrayTypeSymbolComponent(String alias) {
		super("TODO");
		this.alias = alias;
		this.indexesAddrs = new ArrayList<String>();
	}
	
	public ArrayTypeSymbolComponent(String alias, String classname) {
		super("TODO");
		this.alias = alias;
		this.classname = classname;
		this.indexesAddrs = new ArrayList<String>();
	}

	
	public ArrayTypeSymbolComponent(ArrayTypeSymbolComponent other){
		this(other.alias);
		for (String indexAddr: other.indexesAddrs){
			this.indexesAddrs.add(indexAddr);
		}
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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String register) {
		this.alias = register;
	}

	public List<String> getIndexesAddrs() {
		return indexesAddrs;
	}

	public void addIndexAddr(String indexAddr) {
		this.indexesAddrs.add(indexAddr);
	}
}
