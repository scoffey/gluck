package ar.edu.itba.dcc.tp.optimizer.optimizations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import ar.edu.itba.dcc.tp.optimizer.Block;
import ar.edu.itba.dcc.tp.optimizer.Expression;
import ar.edu.itba.dcc.tp.optimizer.FlowDigraph;
import ar.edu.itba.dcc.tp.optimizer.InstructionLoader;
import ar.edu.itba.dcc.tp.optimizer.Optimization;
import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;

public class CommonSubexpressionOptimization implements Optimization {

	static int newVariableIndex = 0;

	private List<CommonSubexpressionNode> nodes;

	public boolean optimize(FlowDigraph fd, boolean doInterblockOptimizations) {
		boolean hasChangedOnce = false;

		// Reemplazos intra-loque
		for (Block b : fd) {
			hasChangedOnce |= optimize(b);
		}

		// Reemplazos extra-bloque
		if (doInterblockOptimizations) {
			hasChangedOnce |= doInterBlockOptimization(fd);
		}

		return hasChangedOnce;
	}

	private boolean optimize(Block block) {
		nodes = new ArrayList<CommonSubexpressionNode>();
		boolean hasChanged = false;
		ArrayList<Instruction> output = new ArrayList<Instruction>();
		for (Instruction i : block) {
			Instruction instruction = i;
			if (i.isAssignment()) {
				/* Ver si la subexpresion ya fue definida, sino creaarla */
				CommonSubexpressionNode node = getNode(i);
				/* Bajar la instruccion, segun corresponda */
				if (node.getLvalues().size() > 1) {
					instruction = new Instruction(i.getLabel(),
							InstructionType.ASSIGN, i.getOp1(), node
									.getLvalues().get(0), null);
					hasChanged = true;
				}
			}
			output.add(instruction);
		}

		block.setInstructions(output);
		return hasChanged;
	}

	private CommonSubexpressionNode createNode(String nodeValue) {
		for (CommonSubexpressionNode node : nodes) {
			if (node.getLvalues().contains(nodeValue)) {
				return node;
			}
		}

		ArrayList<String> lvalues = new ArrayList<String>();
		lvalues.add(nodeValue);
		CommonSubexpressionNode newNode = new CommonSubexpressionNode(
				nodeValue, lvalues);
		nodes.add(newNode);
		return newNode;
	}

	private CommonSubexpressionNode getNode(Instruction i) {
		if (i.getInstructionType().equals(InstructionType.ASSIGN)) {
			CommonSubexpressionNode rvalue = createNode(i.getOp2());
			for (CommonSubexpressionNode node : nodes) {
				if (node.getLvalues().contains(i.getOp1())) {
					node.getLvalues().remove(i.getOp1());
				}
			}
			rvalue.getLvalues().add(i.getOp1());
			return rvalue;
		} else {
			CommonSubexpressionNode lnode = createNode(i.getOp2());
			CommonSubexpressionNode rnode = createNode(i.getOp3());
			for (CommonSubexpressionNode node : nodes) {
				/* Si el operador es el mismo */
				if (node.getOperand().equals(i.getInstructionType().toString())) {
					/* Me fijo si los operando son los mismos */
					if (node.lnode.getLvalues().contains(i.getOp2())
							&& node.rnode.getLvalues().contains(i.getOp3())) {
						node.getLvalues().add(i.getOp1());
						return node;
					} else if (i.isConmutative()
							&& node.lnode.getLvalues().contains(i.getOp3())
							&& node.rnode.getLvalues().contains(i.getOp2())) {
						node.getLvalues().add(i.getOp1());
						return node;
					}
				}

				/* Curso del lvalue */
				if (node.getLvalues().contains(i.getOp1())) {
					node.getLvalues().remove(i.getOp1());
				}

			}
			/* Si la subexpresion no fue definida, la definimos */
			List<String> lvalues = new ArrayList<String>();
			lvalues.add(i.getOp1());
			CommonSubexpressionNode newSubexpression = new CommonSubexpressionNode(
					i.getInstructionType().toString(), lvalues, lnode, rnode);
			nodes.add(newSubexpression);
			return newSubexpression;

		}
	}

	private boolean doInterBlockOptimization(FlowDigraph fd) {
		boolean hasChanged = false;

		// Inicialización de cada b.expressionOutput como U
		// (todas las expresiones) o vacío si es la raíz
		TreeSet<Expression> allExpressions = getAllExpressions(fd);
		initializeInOutSets(fd, allExpressions);

		// Construir los conjuntos de expresiones In y Out de cada bloque
		// iterativamente (mientras haya cambios)
		boolean thereAreChanges = true;
		while (thereAreChanges) {
			thereAreChanges = false;
			thereAreChanges |= computeInOutSets(fd, allExpressions);
		}

		// Ahora b.expressionInput son expresiones disponibles al comienzo
		// del bloque b.

		// Algoritmo de la clase 7 de Marcelo, diapositiva 30
		for (Block b : fd) {
			hasChanged |= doInterBlockOptimizationInBlock(b, fd);
		}

		return hasChanged;
	}

	private void initializeInOutSets(FlowDigraph fd,
			TreeSet<Expression> allExpressions) {
		for (Block b : fd) {
			b.expressionOutput = new TreeSet<Expression>();
			if (b == fd.getRoot()) {
				b.expressionOutput = b.getGeneratedExpressions(allExpressions);
				b.expressionInput = new TreeSet<Expression>();
			} else {
				// shallow copy de allExpressions
				for (Expression e : allExpressions) {
					b.expressionOutput.add(e);
				}
			}
		}
	}

	private boolean computeInOutSets(FlowDigraph fd,
			TreeSet<Expression> allExpressions) {
		boolean thereAreChanges = false;
		// foreach basic block other than entry:
		for (Block b : fd) {
			if (b == fd.getRoot()) {
				continue;
			}

			// In(B) = Intersection([Out(P) for P in Predecessors(B)])
			TreeSet<Expression> intersection = null;
			for (Block p : fd.getBlocksReachingTo(b)) {
				if (b == p) {
					continue;
				}
				TreeSet<Expression> out = getExpressionOutput(p, fd);
				if (intersection == null) {
					intersection = out;
				} else {
					for (Expression e : out) {
						if (!intersection.contains(e))
							intersection.remove(e);
					}
				}
			}
			b.expressionInput = (intersection == null) ? new TreeSet<Expression>()
					: intersection;
			// System.out.println("Expresiones disponibles "
			// + "al comienzo del bloque " + n + ": "
			// + b.expressionInput);

			// Out(B) = Union(Gen(B), In(B) - Kill(B))
			TreeSet<Expression> union = b
					.getGeneratedExpressions(allExpressions);
			TreeSet<Expression> kill = b.getKilledExpressions(allExpressions);
			for (Expression e : b.expressionInput) {
				if (!kill.contains(e)) {
					union.add(e);
				}
			}
			if (!union.equals(b.expressionOutput)) {
				thereAreChanges = true;
				b.expressionOutput = union;
			}
			// System.out.println("Expresiones disponibles al"
			// + " final del bloque " + n + ": " + b.expressionOutput);
		}
		return thereAreChanges;
	}

	private boolean doInterBlockOptimizationInBlock(Block b, FlowDigraph fd) {
		boolean hasChanged = false;
		TreeSet<String> definitions = new TreeSet<String>();
		for (Instruction i : b) {
			if (isExpression(i) && !definitions.contains(i.getOp2())
					&& !definitions.contains(i.getOp3())) {
				Expression e = new Expression(i);
				if (b.expressionInput.contains(e)) {
					TreeSet<Block> reaching = fd.getBlocksReachingTo(b);
					// Crear una nueva variable u
					String u = String.format("#_cse%d", ++newVariableIndex);
					// Para cada bloque predecesor, hacer la sustitución
					for (Block r : reaching) {
						replaceSubexpression(r, b, e, u);
					}
					hasChanged = true;
				}
			}
			if (i.isAssignment()) {
				definitions.add(i.getOp1());
			}
		}
		return hasChanged;
	}

	private void replaceSubexpression(Block b, Block optimized, Expression e,
			String u) {
		ArrayList<Instruction> tmp = new ArrayList<Instruction>();
		for (Instruction i : b) {
			// Sustituir cada proposición w:= y+z por
			if (isExpression(i)) {
				Expression x = new Expression(i);
				if (x.compareTo(e) == 0) {
					Instruction alt;
					// System.out.println("El bloque con el tag " + b.tag
					// + " se va a reemplazar en la expresion " + e);
					if (b == optimized) {
						// Sustituir por: x:= u;
						alt = new Instruction(i.getLabel(),
								InstructionType.ASSIGN, e.getOp1(), u, null);
						tmp.add(alt);
					} else {
						// Sustituir por: u := y+z; w := u;
						alt = new Instruction(i.getLabel(), e
								.getInstructionType(), u, e.getOp2(), e
								.getOp3());
						tmp.add(alt);
						alt = new Instruction(null, InstructionType.ASSIGN, i
								.getOp1(), u, null);
						tmp.add(alt);
					}
					continue;
				}
			}
			// Sino mantener la misma instrucción
			tmp.add(i);
		}
		b.setInstructions(tmp);
	}

	/**
	 * Devuelve todas las expresiones presentes en el digrafo de flujo.
	 * 
	 * @param fd
	 * @return
	 */
	private TreeSet<Expression> getAllExpressions(FlowDigraph fd) {
		TreeSet<Expression> allExpressions = new TreeSet<Expression>();
		for (Block b : fd) {
			for (Instruction i : b) {
				if (isExpression(i)) {
					Expression e = new Expression(i);
					allExpressions.add(e);
				}
			}
		}
		return allExpressions;
	}

	private boolean isExpression(Instruction i) {
		return i.isAssignment()
				&& !i.getInstructionType().equals(InstructionType.ASSIGN);
	}

	public TreeSet<Expression> getExpressionInput(Block b, FlowDigraph fd) {
		if (b.expressionInput == null) {
			b.expressionInput = new TreeSet<Expression>();
		}
		return b.expressionInput;
	}

	public TreeSet<Expression> getExpressionOutput(Block b, FlowDigraph fd) {
		if (b.expressionOutput == null) {
			b.expressionOutput = new TreeSet<Expression>();
		}
		return b.expressionOutput;
	}

	/*
	 * Como me gusta esta clase inner
	 */
	class CommonSubexpressionNode {
		private String operand;
		private List<String> lvalues;

		public CommonSubexpressionNode lnode, rnode;

		public CommonSubexpressionNode(String operand, List<String> lvalues) {
			this(operand, lvalues, null, null);
		}

		public CommonSubexpressionNode(String operand, List<String> lvalues,
				CommonSubexpressionNode lnode, CommonSubexpressionNode rnode) {
			super();
			this.operand = operand;
			this.lvalues = lvalues;
			this.lnode = lnode;
			this.rnode = rnode;
		}

		public String getOperand() {
			return operand;
		}

		public List<String> getLvalues() {
			return lvalues;
		}

		public String toString() {
			return String.format("(%s, %s, %s)", operand, lnode, rnode);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		if (args.length < 1) {
			System.err.println("File argument missing");
			return;
		}

		for (int i = 0; i < args.length; i++) {
			InstructionLoader loader = new InstructionLoader();
			ArrayList<Instruction> list = loader.load(new File(args[i]));
			FlowDigraph fd = new FlowDigraph(list);
			Optimization opt = new CommonSubexpressionOptimization();
			opt.optimize(fd, true);
			int n = 1;
			for (Block b : fd) {
				System.out.println("[Bloque #" + (n++) + "]");
				System.out.println(b.toString());
			}
		}
	}

}
