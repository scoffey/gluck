package ar.edu.itba.dcc.tp.semantic.symbols;

import java.util.Collection;
import java.util.List;

import ar.edu.itba.dcc.tp.semantic.nodes.Expression;

/**
 * Símbolo que representa a una variable en la tabla de símbolos. Sirve tanto
 * para variables como para parámetros de entrada y salida (determinado por el
 * atributo VariableType).
 */
public class VariableSymbol extends Symbol {

	protected TypeSymbol type;
	protected List<FunctionSymbol> constraints;
	protected boolean initialized;
	protected VariableType variableType;
	private String addr = null;
	private List<Expression> dimAddr;
	private String sourceFile;

	public enum VariableType {
		INPUT("pen"), OUTPUT("psal"), COMPONENT("com"), LOCAL("var");
		private String s;

		VariableType(String s) {
			this.s = s;
		}

		@Override
		public String toString() {
			return s;
		}
	};

	public VariableSymbol(String id, String addr, TypeSymbol type, List<FunctionSymbol> constraints,
			boolean initialized, VariableType variableType) {
		this(id, addr, type, constraints, initialized, variableType, null);
	}

	public VariableSymbol(String id, String addr, TypeSymbol type, List<FunctionSymbol> constraints,
			boolean initialized, VariableType variableType, List<Expression> dimAddr) {
		super(id);
		this.addr = addr;
		this.type = type;
		this.constraints = constraints;
		this.initialized = initialized;
		this.variableType = variableType;
		this.dimAddr = dimAddr;
	}

	public VariableSymbol(String id, TypeSymbol type, List<FunctionSymbol> constraints, boolean initialized,
			VariableType variableType) {
		this(id, null, type, constraints, initialized, variableType);
	}

	public VariableSymbol(String id, TypeSymbol type, List<FunctionSymbol> constraints, boolean initialized) {
		this(id, type, constraints, initialized, VariableType.LOCAL);
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public String getAddr() {
		if (addr != null) {
			return addr;
		} else {
			return id;
		}
	}

	public void setType(TypeSymbol type) {
		this.type = type;
	}

	public TypeSymbol getType() {
		return type;
	}

	public List<FunctionSymbol> getConstraints() {
		return constraints;
	}

	public void setVariableType(VariableType variableType) {
		this.variableType = variableType;
	}

	public boolean isInputParam() {
		return this.variableType == VariableType.INPUT;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	@Override
	public void print(int margin) {
		String m = getMargin(margin) + variableType.toString();
		System.out.printf("%-20s%-20s%s\n", m, id, type.toString());
		for (FunctionSymbol c : constraints) {
			m = getMargin(margin + 1) + "res";
			System.out.printf("%-20s%s\n", m, c.getId());
		}
	}

	public static class PendingCast {
		public int line;
		public TypeSymbol type;

		public PendingCast(int line, TypeSymbol type) {
			super();
			this.line = line;
			this.type = type;
		}

	}

	public Collection<? extends Expression> getArrayAddr() {
		return dimAddr;
	}

	public void setConstraints(List<FunctionSymbol> newConstraints) {
		this.constraints = newConstraints;
	}

	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf('.'));
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public boolean isLocalVariable() {
		return this.variableType == VariableType.LOCAL;
	}

	public FunctionSymbol getConstraint(String id) {
		for (FunctionSymbol constraint : getConstraints()) {
			if (constraint.getId().equals(id)) {
				return constraint;
			}
		}
		return null;
	}
}
