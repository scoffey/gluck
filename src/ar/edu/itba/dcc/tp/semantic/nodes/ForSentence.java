package ar.edu.itba.dcc.tp.semantic.nodes;

/**
 * Nodo del árbol sintáctico para los comienzos de sentencias for. Guarda el
 * nombre de la variable contadora y la expresión sobre la que se itera (que
 * debe ser un arreglo).
 */
public class ForSentence extends Sentence {

	private String counterAddr;
	private Expression exp;
	private String sizeAddr;
	private int conditionalInstructionNumber;

	/**
	 * Crea un nuevo nodo de tipo sentencia de for, con una variable contadora y
	 * una expresión sobre la cual iterar.
	 * 
	 * @param counterAddr Nombre de la variable contadora.
	 * @param exp Expresión de tipo arreglo sobre la que se itera.
	 */
	public ForSentence(String counterAddr, Expression exp, String sizeAddr, int conditionalInstructionNumber) {
		this.counterAddr = counterAddr;
		this.exp = exp;
		this.sizeAddr = sizeAddr;
		this.conditionalInstructionNumber = conditionalInstructionNumber;
	}

	public ForSentence(String counterAddr, Expression exp, String sizeAddr) {
		this(counterAddr, exp, sizeAddr, -1);
	}

	public String getCounterAddr() {
		return counterAddr;
	}

	public Expression getExp() {
		return exp;
	}

	public String getSizeAddr() {
		return sizeAddr;
	}

	public int getConditionalInstructionNumber() {
		return conditionalInstructionNumber;
	}
}
