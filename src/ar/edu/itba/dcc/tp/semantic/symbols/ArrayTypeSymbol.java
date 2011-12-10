package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;
import ar.edu.itba.dcc.tp.semantic.nodes.Expression;

/**
 * Tipo que representa un arreglo.
 */
public class ArrayTypeSymbol extends ListTypeSymbol {
	private List<String> dimentions;
	
	public ArrayTypeSymbol(TypeSymbol componentsType) {
		super.getTypes().add(componentsType);
	}

	public ArrayTypeSymbol(TypeSymbol componentsType, List<String> dimentions) {
		super.getTypes().add(componentsType);
		this.dimentions = dimentions;
	}

	
	public TypeSymbol getComponentsType() {
		return getTypes().get(0);
	}

	@Override
	public TypeSymbol getElementType(int index) {
		return getTypes().get(0);
	}

	@Override
	public String toString() {
		return getComponentsType().toString() + "[]";
	}

	public int getLevelCount() {
		if (getComponentsType() instanceof ArrayTypeSymbol) {
			return 1 + ((ArrayTypeSymbol) getComponentsType()).getLevelCount();
		} else {
			return 1;
		}
	}
	
	public TypeSymbol getIthLevelType(int i){
		if (i == 1){
			return getComponentsType();
		} else {
			if (!(getComponentsType() instanceof ArrayTypeSymbol)){
				return null;
			} else {
				return ((ArrayTypeSymbol)getComponentsType()).getIthLevelType(i-1);
			}
		}
	}
	
	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else {
			throw new IncompatibleTypesException();
		}
	}
	
	public boolean equals(TypeSymbol element) {
		if (element == null) {
			return false;
		}
		if (! (element instanceof ArrayTypeSymbol)) {
			return false;
		}
		ArrayTypeSymbol t = (ArrayTypeSymbol)element;
		return this.getComponentsType().equals(t.getComponentsType());
	}
	
	public List<String> getDimentions() {
		return dimentions;
	}
	
	public void setDimentions(List<Expression> list) {
		dimentions = new ArrayList<String>();
		for (Expression expression: list){
			dimentions.add(expression.getAddr());
		}
	}
	
	// Fix me: correcciones de ultimo momento
	public void setDimentions(List<String> list, boolean flag) {
		dimentions = new ArrayList<String>();
		for (String s: list){
			dimentions.add(s);
		}
	}

	
	@Override
	public String jasminType(String packageName, String className) {
		return "Ljava/util/ArrayList;";
	}
	
	public TypeSymbol getMostDeepComponentType(){
		return getIthLevelType(getLevelCount());
	}
}
