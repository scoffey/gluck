package ar.edu.itba.dcc.tp.semantic.nodes;

/**
 * Nodo del árbol sintáctico que representa a un tipo array.
 */
public class ArrayType {
	private String dimAddr;

	public ArrayType(String dimAddr) {
		this.dimAddr = dimAddr;
	}
	
	public String getDimAddr() {
		return dimAddr;
	}
}
