// Tiempo perdido...

package ar.edu.itba.dcc.tp.optimizer.optimizations;

//import java.util.ArrayList;
//import java.util.TreeSet;

//import ar.edu.itba.dcc.tp.optimizer.Block;
import ar.edu.itba.dcc.tp.optimizer.FlowDigraph;
import ar.edu.itba.dcc.tp.optimizer.Optimization;
//import ar.edu.itba.dcc.tp.semantic.Instruction;

public class DeadCodeOptimization implements Optimization {

	public boolean optimize(FlowDigraph fd, boolean doInterblockOptimizations) {
//		boolean thereAreChanges = true;
//		while (thereAreChanges) {
//			thereAreChanges = false;
//			for (Block b : fd) {
//				thereAreChanges |= optimize(b, fd.getBlocksReachableBy(b));
//			}
//		}
//		int i = 0;
//		for (Block b : fd) {
//			System.out.println("Bloque " + i++);
//			System.out.println(b.liveBefore);
//			System.out.println(b.liveAfter);
//			for (String s : getWrittenVariables(b)) {
//				if (!(b.liveAfter.contains(s))) {
//					System.out.println("Detectamos un código muerto!!!: " + s);
//					System.out.println(b);
//				}
//				
//			}
//		}
		return false;
	}

//	public boolean optimize(Block b, TreeSet<Block> reachable) {
//		// Si b.liveAfter y b.liveBefore no estaban inicializados,
//		// se toman como conjuntos vacíos
//		int initialLiveAfterSize = getLiveAfterSet(b).size();
//		int initialLiveBeforeSize = getLiveBeforeSet(b).size();
//		// Determinar b.liveAfter = Unión de i.liveBefore
//		// para cada bloque i alcanzable por b
//		for (Block r : reachable) {
//			b.liveAfter.addAll(getLiveBeforeSet(r));
//		}
//		// Determinar las variables definidas en b
//		TreeSet<String> defined = getWrittenVariables(b);
//		// Determinar b.liveBefore = todas las variables de
//		// b.liveAfter excepto por las variables definidas en b
//		for (String s : b.liveAfter) {
//			if (!defined.contains(s)) {
//				b.liveBefore.add(s);
//			}
//		}
//		b.liveBefore.addAll(getReadVariables(b));
//		// Decidir si hubieron cambios en alguno de los dos conjuntos
//		return (b.liveAfter.size() != initialLiveAfterSize)
//				|| (b.liveBefore.size() != initialLiveBeforeSize);
//	}
//
//	private TreeSet<String> getLiveAfterSet(Block b) {
//		if (b.liveAfter == null) {
//			b.liveAfter = new TreeSet<String>();
//		}
//		return b.liveAfter;
//	}
//
//	private TreeSet<String> getLiveBeforeSet(Block b) {
//		if (b.liveBefore == null) {
//			b.liveBefore = new TreeSet<String>();
//		}
//		return b.liveBefore;
//	}
//
//	private TreeSet<String> getWrittenVariables(Block b) {
//		// Si b.definedVariables no está inicializada, construirla
//		if (b.writtenVariables == null) {
//			b.writtenVariables = new TreeSet<String>();
//			for (Instruction i : b) {
//				String w = i.getWrittenOperands();
//				if (w != null) {
//					b.writtenVariables.add(w);
//				}
//			}
//
//		}
//		return b.writtenVariables;
//	}
//
//	private TreeSet<String> getReadVariables(Block b) {
//		// Si b.definedVariables no está inicializada, construirla
//		if (b.readVariables == null) {
//			b.readVariables = new TreeSet<String>();
//			for (Instruction i : b) {
//				String[] r = i.getReadOperands();
//				if (r != null && r.length > 0) {
//					if (!Character.isDigit(r[0].charAt(0)))
//						b.readVariables.add(r[0]);
//					if (r.length > 1 && !Character.isDigit(r[1].charAt(0)))
//						b.readVariables.add(r[1]);
//				}
//			}
//
//		}
//		return b.readVariables;
//	}
//
//	public static void main(String[] args) {
//		ArrayList<Instruction> list = new ArrayList<Instruction>();
//		list.add(new Instruction(null, Instruction.InstructionType.IF_EQ, "c",
//				"c", "8", 0));
//		list.add(new Instruction(null, Instruction.InstructionType.ADD, "x",
//				"y", "1", 1));
//		list.add(new Instruction(null, Instruction.InstructionType.MULT, "y",
//				"2", "z", 2));
//		list.add(new Instruction(null, Instruction.InstructionType.IF_EQ, "d",
//				"d", "6", 3));
//		list.add(new Instruction(null, Instruction.InstructionType.ADD, "x",
//				"y", "z", 4));
//		list.add(new Instruction(null, Instruction.InstructionType.MULT, "z",
//				"x", "2", 5));
//		list.add(new Instruction(null, Instruction.InstructionType.ASSIGN, "z",
//				"1", null, 6));
//		list.add(new Instruction(null, Instruction.InstructionType.GOTO, "1",
//				null, null, 7));
//		list.add(new Instruction(null, Instruction.InstructionType.ASSIGN, "z",
//				"x", null, 8));
//		FlowDigraph fd = new FlowDigraph(list);
//		DeadCodeOptimization opt = new DeadCodeOptimization();
//		opt.optimize(fd);
//	}

}
