package ar.edu.itba.dcc.tp.optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;

import ar.edu.itba.dcc.tp.semantic.Instruction;
import ar.edu.itba.dcc.tp.semantic.Instruction.InstructionType;

public class FlowDigraph implements Iterable<Block> {

	private List<Block> blocks;

	private Block root;

	public FlowDigraph(List<Instruction> instructions) {
		if (instructions.size() == 0) {
			throw new RuntimeException("Flow digraph has no instructions");
		}
		explode(instructions);
		root = blocks.get(0);
		setDominance();
	}

	public Block getRoot() {
		return root;
	}

	/**
	 * Divide una lista de instrucciones en bloques.
	 * 
	 * @param instructions
	 */
	private void explode(List<Instruction> instructions) {
		int[] blockNumbers = new int[instructions.size()];
		for (int i = 0; i < blockNumbers.length; i++) {
			Instruction instr = instructions.get(i);
			if (instr.isConditionalJump()) {
				int pointedLine = instr.getPointer();
				split(blockNumbers, pointedLine);
				split(blockNumbers, i + 1);
			} else if (instr.getInstructionType().equals(InstructionType.GOTO)) {
				int pointedLine = instr.getPointer();
				split(blockNumbers, pointedLine);
				split(blockNumbers, i + 1);
			} else if (instr.getInstructionType().equals(InstructionType.CALL)) {
				int pointedLine = instr.getPointer();
				split(blockNumbers, pointedLine);
				split(blockNumbers, i + 1);
			} else if (instr.getInstructionType().equals(InstructionType.RET)) {
				split(blockNumbers, i + 1);
			}
		}
		createBlocks(blockNumbers, instructions);
	}

	private void createBlocks(int[] blockNumbers, List<Instruction> instructions) {
		int numberOfBlocks = blockNumbers[blockNumbers.length - 1] + 1;
		blocks = new ArrayList<Block>(numberOfBlocks);
		for (int i = 0; i < numberOfBlocks; i++) {
			blocks.add(new Block());
		}
		for (int i = 0; i < instructions.size(); i++) {
			Block currentBlock = blocks.get(blockNumbers[i]);
			currentBlock.addInstruction(instructions.get(i));
			if (i > 0) {
				Block lastBlock = blocks.get(blockNumbers[i - 1]);
				Instruction lastInstruction = instructions.get(i - 1);
				if (lastInstruction.isConditionalJump()) {
					int p = lastInstruction.getPointer();
					if (p >= 0) {
						Block pointedBlock = blocks.get(blockNumbers[p]);
						lastBlock.addConditionalEdge(pointedBlock);
					}
					lastBlock.addNextEdge(currentBlock);
				} else if (lastInstruction.getInstructionType().equals(
						InstructionType.GOTO)) {
					int p = lastInstruction.getPointer();
					if (p >= 0) {
						Block pointedBlock = blocks.get(blockNumbers[p]);
						lastBlock.addNextEdge(pointedBlock);
					}
				} else if (lastInstruction.getInstructionType().equals(
						InstructionType.CALL)) {
					int p = lastInstruction.getPointer();
					if (p >= 0) {
						Block pointedBlock = blocks.get(blockNumbers[p]);
						lastBlock.addNextEdge(pointedBlock);
					}
					if (i + 1 < blocks.size()) {
						Block nextBlock = blocks.get(blockNumbers[i + 1]);
						nextBlock.addNextEdge(currentBlock);
					}
					// } else if (lastInstruction.getInstructionType().equals(
					// InstructionType.RET)) {
					// // No hace falta unir bloques porque lo resuelve el caso
					// del CALL
				} else {
					if (lastBlock != currentBlock) {
						lastBlock.addNextEdge(currentBlock);
					}
				}
			}
		}
	}

	public void split(int[] blockNumbers, int index) {
		if (!(index >= 0 && index < blockNumbers.length)
				|| (index > 0 && blockNumbers[index] != blockNumbers[index - 1]))
			return; // si ya estaba spliteado, listo
		for (int i = index; i < blockNumbers.length; i++) {
			blockNumbers[i]++;
		}
	}

	public void resetTags() {
		for (Block b : this) {
			b.tag = 0;
		}
	}

	private void doBFSTagging(Block source, boolean inOutgoingDirection) {
		resetTags();
		Queue<Block> pending = new LinkedList<Block>();
		pending.offer(source);
		int tag = 1;
		while (!pending.isEmpty()) {
			Block b = pending.poll();
			b.tag = tag++;
			Iterator<Block> i = inOutgoingDirection ? b
					.getOutgoingBlocksIterator() : b
					.getIncomingBlocksIterator();
			while (i.hasNext()) {
				Block incoming = i.next();
				if (incoming.tag == 0) {
					pending.offer(incoming);
				}
			}
		}
	}

	public TreeSet<Block> getBlocksReachingTo(Block destination) {
		doBFSTagging(destination, false);
		return getTagSortedBlockSet();
	}

	public TreeSet<Block> getBlocksReachableBy(Block source) {
		doBFSTagging(source, true);
		return getTagSortedBlockSet();
	}

	private TreeSet<Block> getTagSortedBlockSet() {
		TreeSet<Block> set = new TreeSet<Block>(new Comparator<Block>() {
			public int compare(Block b1, Block b2) {
				return b1.tag - b2.tag;
			}
		});
		for (Block b : blocks) {
			if (b.tag != 0)
				set.add(b);
		}
		return set;
	}

	private void setDominance() {
		TreeSet<Block> sortedBlocks = getBlocksReachableBy(root);
		for (Block b : sortedBlocks) {
			Block dominant = getImmediateDominantBlockFor(b);
			if (dominant != null) {
				b.setImmediateDominant(dominant);
			}
		}
	}

	private Block getImmediateDominantBlockFor(Block b) {
		Block dominant = null;
		Iterator<Block> i = b.getIncomingBlocksIterator();
		ArrayList<Block> precedentBlocks = new ArrayList<Block>();
		while (i.hasNext()) {
			Block p = i.next();
			if (p.tag < b.tag)
				precedentBlocks.add(p);
		}
		if (precedentBlocks.size() == 1) {
			dominant = precedentBlocks.get(0);
			b.setImmediateDominant(dominant);
		} else {
			HashSet<Block> intersection = null;
			// Calcular la intersección de los conjuntos de dominancia
			// de los bloques precedentes de b
			for (Block p : precedentBlocks) {
				Iterator<Block> j = p.getDominantBlocksIterator();
				HashSet<Block> pd = new HashSet<Block>();
				while (j.hasNext()) {
					pd.add(j.next());
				}
				if (intersection == null) {
					intersection = pd;
				} else {
					for (Block k : intersection) {
						HashSet<Block> newIntersection = new HashSet<Block>();
						if (pd.contains(k)) {
							newIntersection.add(k);
						}
						intersection = newIntersection;
					}
				}
			}
			// Tomar el bloque de mayor tag en la intersección
			if (intersection != null) {
				for (Block k : intersection) {
					if (dominant == null || dominant.tag < k.tag) {
						dominant = k;
					}
				}
			}
		}
		return dominant;
	}

	public Iterator<Block> iterator() {
		return blocks.iterator();
	}

	public int getNumberOfInstructions() {
		int n = 0;
		for (Block b : this) {
			n += b.getNumberOfInstructions();
		}
		return n;
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for (Block b : blocks) {
			s.append(b.toString());
		}
		return s.toString();
	}

	// public static void main(String[] args) {
	// // This is for testing
	// ArrayList<Instruction> list = new ArrayList<Instruction>();
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "foo", "bar", null, 0));
	// list.add(new Instruction(null, Instruction.InstructionType.IF_EQ,
	// "foo", "bar", "0", 1));
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "baz", "2", null, 2));
	// list.add(new Instruction(null, Instruction.InstructionType.GOTO, "5",
	// null, null, 3));
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "baz", "4", null, 4));
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "foo", "bar", null, 5));
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "foo", "bar", null, 6));
	// list.add(new Instruction(null, Instruction.InstructionType.ASSIGN,
	// "foo", "bar", null, 7));
	//
	// FlowDigraph fd = new FlowDigraph(list);
	// Optimization opt = new CommonSubexpressionOptimization();
	// fd = opt.optimize(fd, false);
	// for (Block b : fd) {
	// System.out.println("Bloque:");
	// System.out.println(b.toString());
	// }
	// }

}
