package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.IncompatibleTypesException;

/**
 * Tipo de dato definido por el usuario
 */
public class UserDefinedTypeSymbol extends TypeSymbol {
	private List<VariableSymbol> components;
	private String sourceFile;

	public UserDefinedTypeSymbol(String id, List<VariableSymbol> components, String sourceFile) {
		super(id);
		this.sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
		this.components = new ArrayList<VariableSymbol>(components);
	}

	public UserDefinedTypeSymbol(String id, String sourceFile) {
		super(id);
		this.sourceFile = sourceFile;
		this.components = new ArrayList<VariableSymbol>();
	}

	public List<VariableSymbol> getComponents() {
		return components;
	}

	/**
	 * Busca un componente dentro del tipo. Si lo encuentra retorna el índice, y
	 * en caso contrario -1.
	 * 
	 * @param component Nombre del componente buscado.
	 * @return Indice que ocupa el componente, o -1 si no existe.
	 */
	public int getComponentIndex(String component) {
		for (int i = 0; i < components.size(); i++) {
			if (components.get(i).getId().equals(component)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void print(int margin) {
		String m = getMargin(margin) + "typ";
		System.out.printf("%-20s%s\n", m, id);
		for (VariableSymbol c : components) {
			c.print(margin + 1);
		}
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public String castTo(TypeSymbol typeSymbol) throws IncompatibleTypesException {
		if (this.equals(typeSymbol)) {
			return null;
		} else {
			throw new IncompatibleTypesException();
		}
	}

	@Override
	public boolean equals(Symbol element) {
		return this.equals((Object) element);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UserDefinedTypeSymbol)) {
			return false;
		}
		UserDefinedTypeSymbol castedObj = (UserDefinedTypeSymbol) obj;
		return castedObj.getId().equals(id);
	}

	@Override
	public String jasminType(String packageName, String className) {
		return "L" + sourceFile + "$" + id + ";";
	}
	
	public String getSourceFile() {
		return sourceFile;
	}

}
