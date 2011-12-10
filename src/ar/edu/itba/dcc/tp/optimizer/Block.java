package ar.edu.itba.dcc.tp.optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ar.edu.itba.dcc.tp.semantic.Instruction;

public class Block implements Iterable<Instruction> {

	// Variables de instancia privadas

	private List<Instruction> instructions;

	private HashSet<Block> incoming;

	private Block[] outgoing;

	private Block immediateDominantBlock;

	private TreeSet<Expression> generatedExpressions;

	private TreeSet<Expression> killedExpressions;

	private Map<String, Integer> constantsDefinitions;

	// private TreeSet<Instruction> generatedDefinitions;

	// Variables de instancia públicas
	// Son las que usa cada algoritmo para optimizar.
	// Ellos se hacen responsables de inicializar, destruir, etc.

	public int tag;

	public TreeSet<Expression> expressionInput;

	public TreeSet<Expression> expressionOutput;

	// public TreeSet<String> liveBefore;
	// public TreeSet<String> liveAfter;
	// public TreeSet<String> writtenVariables;
	// public TreeSet<String> readVariables;

	// Constructor

	public Block() {
		this.instructions = new ArrayList<Instruction>();
		this.incoming = new HashSet<Block>();
		this.outgoing = new Block[2];
		this.immediateDominantBlock = null;
		this.tag = 0;
		this.constantsDefinitions = new Hashtable<String, Integer>();
		this.generatedExpressions = null;
		this.killedExpressions = null;
	}

	public Block(ArrayList<Instruction> instructions) {
		this();
		setInstructions(instructions);
	}

	public void setInstructions(ArrayList<Instruction> instructions) {
		this.instructions = instructions;
	}

	public void addNextEdge(Block dst) {
		outgoing[0] = dst;
		dst.incoming.add(this);
	}

	public void addConditionalEdge(Block dst) {
		outgoing[1] = dst;
		dst.incoming.add(this);
	}

	public void setImmediateDominant(Block dom) {
		immediateDominantBlock = dom;
	}

	public Iterator<Instruction> iterator() {
		return instructions.iterator();
	}

	public Iterator<Block> getIncomingBlocksIterator() {
		return incoming.iterator();
	}

	public Iterator<Block> getOutgoingBlocksIterator() {
		ArrayList<Block> tmp = new ArrayList<Block>();
		if (outgoing[0] != null)
			tmp.add(outgoing[0]);
		if (outgoing[1] != null)
			tmp.add(outgoing[1]);
		return tmp.iterator();
	}

	public Iterator<Block> getDominantBlocksIterator() {
		HashSet<Block> dominantBlocks = new HashSet<Block>();
		dominantBlocks.add(this);
		if (immediateDominantBlock != null) {
			Iterator<Block> i = immediateDominantBlock
					.getDominantBlocksIterator();
			while (i.hasNext()) {
				dominantBlocks.add(i.next());
			}
		}
		return dominantBlocks.iterator();
	}

	public TreeSet<Expression> getGeneratedExpressions(
			Set<Expression> allExpressions) {
		if (generatedExpressions == null) {
			recomputeExpressionsGenKill(allExpressions);
			if (generatedExpressions == null)
				generatedExpressions = createExpressionGenSet();
		}
		return generatedExpressions;
	}

	public TreeSet<Expression> getKilledExpressions(
			Set<Expression> allExpressions) {
		if (killedExpressions == null) {
			recomputeExpressionsGenKill(allExpressions);
			if (killedExpressions == null)
				killedExpressions = new TreeSet<Expression>();
		}
		return killedExpressions;
	}

	public void recomputeExpressionsGenKill(Set<Expression> allExpressions) {
		TreeSet<Expression> lastGen = createExpressionGenSet();
		TreeSet<Expression> lastKill = new TreeSet<Expression>();
		for (Instruction i : this) {
			try {
				Expression e = new Expression(i);
				TreeSet<Expression> currentGen = getExpressionGen(e);
				TreeSet<Expression> currentKill = getExpressionKill(e,
						allExpressions);
				// Gen(S) = Gen(S_i) U (Gen(S_i-1) - Kill(S_i))
				generatedExpressions = currentGen;
				for (Expression j : lastGen) {
					if (!currentKill.contains(j)) {
						generatedExpressions.add(j);
					}
				}
				// Kill(S) = Kill(S_i) U (Kill(S_i-1) - Gen(S_i))
				killedExpressions = currentKill;
				for (Expression j : lastKill) {
					if (!currentGen.contains(j)) {
						killedExpressions.add(j);
					}
				}
			} catch (IllegalArgumentException e) {
				// si no es una expresión, continuar...
			}
		}
	}

	private TreeSet<Expression> createExpressionGenSet() {
		return new TreeSet<Expression>(new Comparator<Expression>() {
			public int compare(Expression o1, Expression o2) {
				return o1.compareTo(o2);
			}
		});
	}

	private TreeSet<Expression> getExpressionGen(Expression i) {
		TreeSet<Expression> gen = createExpressionGenSet();
		gen.add(i);
		return gen;
	}

	private TreeSet<Expression> getExpressionKill(Expression i,
			Set<Expression> allExpressions) {
		TreeSet<Expression> kill = new TreeSet<Expression>();
		for (Expression j : allExpressions) {
			if (i.compareTo(j) != 0)
				kill.add(j);
		}
		return kill;
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for (Instruction i : instructions) {
			s.append(i.toString() + "\n");
		}
		return s.toString();
	}

	public String debug() {
		StringBuffer s = new StringBuffer("Block (tag #" + tag + "): ");
		for (Instruction i : instructions) {
			s.append("\"" + i.toString() + "\", ");
		}
		return s.toString();
	}

	public void addInstruction(Instruction i) {
		instructions.add(i);
	}

	public void setInstructions(List<Instruction> instructions) {
		this.instructions = instructions;
	}

	public int getNumberOfInstructions() {
		return instructions.size();
	}

	public Map<String, Integer> getConstantsDefinitions() {
		return constantsDefinitions;
	}

	public void setConstantsDefinitions(
			Map<String, Integer> constantsDefinitions) {
		this.constantsDefinitions = constantsDefinitions;
	}

	/**
	 * Actualiza el conjunto de constantes definidas en este bloque propagando
	 * los de sus predecesores
	 */
	public boolean updateConstantsDefinitions() {
		boolean hasChanged = false;
		Iterator<Block> iter = getDominantBlocksIterator();
		while (iter.hasNext()) {
			Block b = iter.next();
			//System.out.println("Bloque de tag #" + tag + ": me domina " + b.debug());
			for (String key : b.constantsDefinitions.keySet()) {
				// Si la definición está en TODOS los predecesores, la agrego
				boolean containsDefinition = true;
				Iterator<Block> innerIterator = getDominantBlocksIterator();
				while (innerIterator.hasNext()) {
					Block inc = innerIterator.next();
					if (inc != this
							&& (!inc.constantsDefinitions.containsKey(key) || !inc.constantsDefinitions
									.get(key).equals(
											b.constantsDefinitions.get(key)))) {
						containsDefinition = false;
						break;
					}
				}

				if (containsDefinition
						&& !constantsDefinitions.containsKey(key)) {
					constantsDefinitions.put(key, b.constantsDefinitions
							.get(key));
					hasChanged = true;
				}
			}
		}
		return hasChanged;
	}
}
