package ar.edu.itba.dcc.tp.semantic.symbols;

/**
 * Un símbolo dentro de la tabla de símbolos. Cada tipo de símbolo extiende de
 * esta clase para agregar los campos y la funcionalidad necesaria.
 */
public abstract class Symbol {
	protected String id;
	private boolean external = false;
	
	public Symbol(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (byte b : id.getBytes()) {
			hash += b;
		}

		return hash;
	}

	public boolean equals(Symbol obj) {
		Symbol element = (Symbol)obj;
		return id.equals(element.id) && this.getClass().equals(element.getClass());
	}

	public String getId() {
		return id;
	}

	public void setExternal() {
		this.external = true;
	}
	
	public boolean isExternal() {
		return this.external;
	}
	public abstract void print(int margin);

	protected String getMargin(int margin) {
		String ret = new String();
		for (int i = 0; i < margin; i++) {
			ret += "  ";
		}
		return ret;
	}
}